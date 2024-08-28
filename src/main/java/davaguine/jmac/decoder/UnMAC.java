/*
 *  21.04.2004 Original verion. davagin@udm.ru.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package davaguine.jmac.decoder;

import java.io.IOException;

import davaguine.jmac.info.APEHeader;
import davaguine.jmac.info.SpecialFrame;
import davaguine.jmac.info.WaveFormat;
import davaguine.jmac.tools.ByteBuffer;
import davaguine.jmac.tools.Crc32;
import davaguine.jmac.tools.JMACException;
import davaguine.jmac.tools.Prepare;


/**
 * @author Dmitry Vaguine
 * @version 04.03.2004 14:51:31
 */
public class UnMAC {

    // construction/destruction

    public UnMAC() {
        // initialize member variables
        initialized = false;
        realFrame = 0;
        lastDecodedFrameIndex = -1;
        apeDecompress = null;

        apeDecompressCore = null;

        blocksProcessed = 0;
    }

    // functions

    public void initialize(IAPEDecompress apeDecompress) {
        // uninitialize if it is currently initialized
        if (initialized)
            uninitialize();

        if (apeDecompress == null) {
            uninitialize();
            throw new JMACException("Error Initializing UnMAC");
        }

        // set the member pointer to the IAPEDecompress class
        this.apeDecompress = apeDecompress;

        // set the last decode frame to -1 so it forces a seek on start
        lastDecodedFrameIndex = -1;

        apeDecompressCore = new APEDecompressCore(apeDecompress);

        // set the initialized flag to TRUE
        initialized = true;

        wfeInput = apeDecompress.getApeInfoWaveFormatEx();
    }

    public void uninitialize() {
        if (initialized) {
            apeDecompressCore = null;

            // clear the APE info pointer
            apeDecompress = null;

            // set the last decoded frame again
            lastDecodedFrameIndex = -1;

            // set the initialized flag to FALSE
            initialized = false;
        }
    }

    public int decompressFrame(ByteBuffer outputData, int frameIndex) throws IOException {
        return decompressFrameOld(outputData, frameIndex);
    }

    public void seekToFrame(int frameIndex) throws IOException {
        if (apeDecompress.getApeInfoFileVersion() > 3800) {
            if ((lastDecodedFrameIndex == -1) || ((frameIndex - 1) != lastDecodedFrameIndex)) {
                int seekRemainder = (apeDecompress.getApeInfoSeekByte(frameIndex) - apeDecompress.getApeInfoSeekByte(0)) % 4;
                apeDecompressCore.getUnBitArray().fillAndResetBitArray(realFrame == frameIndex ? -1 : apeDecompress.getApeInfoSeekByte(frameIndex) - seekRemainder, seekRemainder * 8);
                realFrame = frameIndex;
            } else
                apeDecompressCore.getUnBitArray().advanceToByteBoundary();
        } else {
            if ((lastDecodedFrameIndex == -1) || ((frameIndex - 1) != lastDecodedFrameIndex)) {
                apeDecompressCore.getUnBitArray().fillAndResetBitArray(realFrame == frameIndex ? -1 : apeDecompress.getApeInfoSeekByte(frameIndex), apeDecompress.getApeInfoSeekBit(frameIndex));
                realFrame = frameIndex;
            }
        }
    }

    // data members

    private boolean initialized;
    private int lastDecodedFrameIndex;
    private int realFrame;
    private IAPEDecompress apeDecompress;

    private APEDecompressCore apeDecompressCore;

    // functions

    private int decompressFrameOld(ByteBuffer outputData, int frameIndex) throws IOException {
        // error check the parameters (too high of a frame index, etc.)
        if (frameIndex >= apeDecompress.getApeInfoTotalFrames())
            return 0;

        // get the number of samples in the frame
        int blocks = 0;
        blocks = ((frameIndex + 1) >= apeDecompress.getApeInfoTotalFrames()) ? apeDecompress.getApeInfoFinalFrameBlocks() : apeDecompress.getApeInfoBlocksPerFrame();
        if (blocks == 0)
            throw new JMACException("Invalid Frame Index"); // nothing to do (file must be zero length) (have to return error)

        // take care of seeking and frame alignment
        seekToFrame(frameIndex);

        // get the checksum
        long specialCodes = 0;
        long storedCRC;

        if ((apeDecompress.getApeInfoFormatFlags() & APEHeader.MAC_FORMAT_FLAG_CRC) <= 0) {
            storedCRC = apeDecompressCore.getUnBitArray().decodeValue(DecodeValueMethod.DECODE_VALUE_METHOD_UNSIGNED_RICE, 30);
            if (storedCRC == 0)
                specialCodes = SpecialFrame.SPECIAL_FRAME_LEFT_SILENCE | SpecialFrame.SPECIAL_FRAME_RIGHT_SILENCE;
        } else {
            storedCRC = apeDecompressCore.getUnBitArray().decodeValue(DecodeValueMethod.DECODE_VALUE_METHOD_UNSIGNED_INT);

            // get any 'special' codes if the file uses them (for silence, FALSE stereo, etc.)
            specialCodes = 0;
            if (apeDecompress.getApeInfoFileVersion() > 3820) {
                if ((storedCRC & 0x8000_0000L) > 0)
                    specialCodes = apeDecompressCore.getUnBitArray().decodeValue(DecodeValueMethod.DECODE_VALUE_METHOD_UNSIGNED_INT);
                storedCRC &= 0x7fff_ffff;
            }
        }

        // decompress and convert from (x,y) -> (l,r)
        // sort of int and ugly.... sorry
        if (apeDecompress.getApeInfoChannels() == 2) {
            apeDecompressCore.generateDecodedArrays(blocks, (int) specialCodes, frameIndex);

            Prepare.unprepareOld(apeDecompressCore.dataX, apeDecompressCore.dataY, blocks, wfeInput,
                    outputData, crc, apeDecompress.getApeInfoFileVersion());
        } else if (apeDecompress.getApeInfoChannels() == 1) {
            apeDecompressCore.generateDecodedArrays(blocks, (int) specialCodes, frameIndex);

            Prepare.unprepareOld(apeDecompressCore.dataX, null, blocks, wfeInput,
                    outputData, crc, apeDecompress.getApeInfoFileVersion());
        }

        if (apeDecompress.getApeInfoFileVersion() > 3820)
            crc.finalizeCrc();

        // check the CRC
        if ((apeDecompress.getApeInfoFormatFlags() & APEHeader.MAC_FORMAT_FLAG_CRC) <= 0) {
            long checksum = calculateOldChecksum(apeDecompressCore.dataX, apeDecompressCore.dataY, apeDecompress.getApeInfoChannels(), blocks);
            if (checksum != storedCRC)
                throw new JMACException("Invalid Checksum");
        } else {
            if (crc.getCrc() != storedCRC)
                throw new JMACException("Invalid Checksum");
        }

        lastDecodedFrameIndex = frameIndex;
        return blocks;
    }

    private static long calculateOldChecksum(int[] dataX, int[] dataY, int channels, int blocks) {
        long checksum = 0;

        if (channels == 2) {
            for (int z = 0; z < blocks; z++) {
                int r = dataX[z] - (dataY[z] / 2);
                int l = r + dataY[z];
                checksum += (Math.abs(r) + Math.abs(l));
            }
        } else if (channels == 1) {
            for (int z = 0; z < blocks; z++)
                checksum += Math.abs(dataX[z]);
        }

        return checksum;
    }

    public final int blocksProcessed;
    public final Crc32 crc = new Crc32();
    public long storedCRC;
    public WaveFormat wfeInput;
}
