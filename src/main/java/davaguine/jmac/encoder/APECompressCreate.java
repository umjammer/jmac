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

package davaguine.jmac.encoder;

import java.io.IOException;
import java.util.Arrays;

import davaguine.jmac.info.APEDescriptor;
import davaguine.jmac.info.APEHeader;
import davaguine.jmac.info.APEHeaderNew;
import davaguine.jmac.info.CompressionLevel;
import davaguine.jmac.info.WaveFormat;
import davaguine.jmac.tools.ByteArrayReader;
import davaguine.jmac.tools.ByteArrayWriter;
import davaguine.jmac.tools.File;
import davaguine.jmac.tools.Globals;
import davaguine.jmac.tools.JMACException;


/**
 * @author Dmitry Vaguine
 * @version 08.05.2004 12:40:36
 */
public class APECompressCreate {

    APECompressCreate() {
        maxFrames = 0;
    }

    public void initializeFile(File io, WaveFormat wfeInput, int maxFrames, int compressionLevel, byte[] headerData, int headerBytes) throws IOException {
        // error check the parameters
        if (io == null || wfeInput == null || maxFrames <= 0)
            throw new JMACException("Bad Parameters");

        APEDescriptor apeDescriptor = new APEDescriptor();
        APEHeaderNew header = new APEHeaderNew();

        // create the descriptor (only fill what we know)
        apeDescriptor.id = "MAC ";
        apeDescriptor.version = Globals.MAC_VERSION_NUMBER;

        apeDescriptor.descriptorBytes = APEDescriptor.APE_DESCRIPTOR_BYTES;
        apeDescriptor.headerBytes = APEHeaderNew.APE_HEADER_BYTES;
        apeDescriptor.seekTableBytes = maxFrames * 4L;
        apeDescriptor.headerDataBytes = (headerBytes == IAPECompress.CREATE_WAV_HEADER_ON_DECOMPRESSION) ? 0 : headerBytes;

        // create the header (only fill what we know now)
        header.bitsPerSample = wfeInput.bitsPerSample;
        header.channels = wfeInput.channels;
        header.sampleRate = wfeInput.samplesPerSec;

        header.compressionLevel = compressionLevel;
        header.formatFlags = (headerBytes == IAPECompress.CREATE_WAV_HEADER_ON_DECOMPRESSION) ? APEHeader.MAC_FORMAT_FLAG_CREATE_WAV_HEADER : 0;

        header.blocksPerFrame = samplesPerFrame;

        // write the data to the file
        ByteArrayWriter writer = new ByteArrayWriter(APEDescriptor.APE_DESCRIPTOR_BYTES + APEHeaderNew.APE_HEADER_BYTES);
        apeDescriptor.write(writer);
        header.write(writer);
        io.write(writer.getBytes());

        // write an empty seek table
        seekTable = new long[maxFrames];
        Arrays.fill(seekTable, 0);
        byte[] zeroTable = new byte[maxFrames * 4];
        Arrays.fill(zeroTable, (byte) 0);
        io.write(zeroTable);
        this.maxFrames = maxFrames;

        // write the WAV data
        if ((headerData != null) && (headerBytes > 0) && (headerBytes != IAPECompress.CREATE_WAV_HEADER_ON_DECOMPRESSION)) {
            apeCompressCore.getBitArray().getMD5Helper().update(headerData, headerBytes);
            io.write(headerData, 0, headerBytes);
        }
    }

    public void finalizeFile(File io, int numberOfFrames, int finalFrameBlocks, byte[] terminatingData, int terminatingBytes, int wavTerminatingBytes, int peakLevel) throws IOException {
        // store the tail position
        int tailPosition = (int) io.getFilePointer();

        // append the terminating data
        if (terminatingBytes > 0) {
            apeCompressCore.getBitArray().getMD5Helper().update(terminatingData, terminatingBytes);
            io.write(terminatingData, 0, terminatingBytes);
        }

        // go to the beginning and update the information
        io.seek(0);

        // get the descriptor
        APEDescriptor descriptor = APEDescriptor.read(io);

        // get the header
        APEHeaderNew header = APEHeaderNew.read(io);

        // update the header
        header.finalFrameBlocks = finalFrameBlocks;
        header.totalFrames = numberOfFrames;

        // update the descriptor
        descriptor.apeFrameDataBytes = tailPosition - (descriptor.descriptorBytes + descriptor.headerBytes + descriptor.seekTableBytes + descriptor.headerDataBytes);
        descriptor.apeFrameDataBytesHigh = 0;
        descriptor.terminatingDataBytes = terminatingBytes;

        // update the MD5
        ByteArrayWriter writer = new ByteArrayWriter(APEHeaderNew.APE_HEADER_BYTES);
        header.write(writer);
        apeCompressCore.getBitArray().getMD5Helper().update(writer.getBytes());
        writer.reset(maxFrames * 4);
        for (int i = 0; i < maxFrames; i++) {
            writer.writeUnsignedInt(seekTable[i]);
        }
        byte[] seekTable = writer.getBytes();
        apeCompressCore.getBitArray().getMD5Helper().update(seekTable);
        descriptor.fileMD5 = apeCompressCore.getBitArray().getMD5Helper().final_();

        // set the pointer and re-write the updated header and peak level
        io.seek(0);
        writer.reset(APEDescriptor.APE_DESCRIPTOR_BYTES + APEHeaderNew.APE_HEADER_BYTES);
        descriptor.write(writer);
        header.write(writer);
        io.write(writer.getBytes());
        // write the updated seek table
        io.write(seekTable);
    }

    public void setSeekByte(int frame, int byteOffset) {
        if (frame >= maxFrames)
            throw new JMACException("APE Compress Too Much Data");
        seekTable[frame] = byteOffset;
    }

    public void start(File output, WaveFormat wfeInput, int maxAudioBytes) throws IOException {
        start(output, wfeInput, maxAudioBytes, CompressionLevel.COMPRESSION_LEVEL_NORMAL, null, IAPECompress.CREATE_WAV_HEADER_ON_DECOMPRESSION);
    }

    public void start(File output, WaveFormat wfeInput, int maxAudioBytes, int compressionLevel) throws IOException {
        start(output, wfeInput, maxAudioBytes, compressionLevel, null, IAPECompress.CREATE_WAV_HEADER_ON_DECOMPRESSION);
    }

    public void start(File output, WaveFormat wfeInput, int maxAudioBytes, int compressionLevel, byte[] headerData) throws IOException {
        start(output, wfeInput, maxAudioBytes, compressionLevel, headerData, IAPECompress.CREATE_WAV_HEADER_ON_DECOMPRESSION);
    }

    public void start(File output, WaveFormat wfeInput, int maxAudioBytes, int compressionLevel, byte[] headerData, int headerBytes) throws IOException {
        // verify the parameters
        if (output == null || wfeInput == null)
            throw new JMACException("Bad Parameters");

        // verify the wave format
        if ((wfeInput.channels != 1) && (wfeInput.channels != 2))
            throw new JMACException("Input File Unsupported Channel Count");
        if ((wfeInput.bitsPerSample != 8) && (wfeInput.bitsPerSample != 16) && (wfeInput.bitsPerSample != 24))
            throw new JMACException("Input File Unsupported Bit Depth");

        // initialize (creates the base classes)
        samplesPerFrame = 73728;
        if (compressionLevel == CompressionLevel.COMPRESSION_LEVEL_EXTRA_HIGH)
            samplesPerFrame *= 4;
        else if (compressionLevel == CompressionLevel.COMPRESSION_LEVEL_INSANE)
            samplesPerFrame *= 16;

        io = output;
        apeCompressCore = new APECompressCore(io, wfeInput, samplesPerFrame, compressionLevel);

        // copy the format
        this.wfeInput = wfeInput;

        // the compression level
        this.compressionLevel = compressionLevel;
        frameIndex = 0;
        lastFrameBlocks = samplesPerFrame;

        // initialize the file
        if (maxAudioBytes < 0)
            maxAudioBytes = 2147483647;

        long maxAudioBlocks = maxAudioBytes / wfeInput.blockAlign;
        int maxFrames = (int) (maxAudioBlocks / samplesPerFrame);
        if ((maxAudioBlocks % samplesPerFrame) != 0) maxFrames++;

        initializeFile(io, this.wfeInput, maxFrames,
                this.compressionLevel, headerData, headerBytes);
    }

    public int getFullFrameBytes() {
        return samplesPerFrame * wfeInput.blockAlign;
    }

    public void encodeFrame(ByteArrayReader inputData, int inputBytes) throws IOException {
        int inputBlocks = inputBytes / wfeInput.blockAlign;

        if ((inputBlocks < samplesPerFrame) && (lastFrameBlocks < samplesPerFrame))
            throw new JMACException("Bad Parameters");

        // update the seek table
        apeCompressCore.getBitArray().advanceToByteBoundary();
        setSeekByte(frameIndex, (int) (io.getFilePointer() + (apeCompressCore.getBitArray().getCurrentBitIndex() / 8)));

        // compress
        apeCompressCore.encodeFrame(inputData, inputBytes);

        // update stats
        lastFrameBlocks = inputBlocks;
        frameIndex++;
    }

    public void finish(byte[] terminatingData, int terminatingBytes, int wavTerminatingBytes) throws IOException {
        // clear the bit array
        apeCompressCore.getBitArray().outputBitArray(true);

        // finalize the file
        finalizeFile(io, frameIndex, lastFrameBlocks,
                terminatingData, terminatingBytes, wavTerminatingBytes, apeCompressCore.getPeakLevel());
    }

    private long[] seekTable;
    private int maxFrames;

    private File io;
    private APECompressCore apeCompressCore;

    private WaveFormat wfeInput;
    private int compressionLevel;
    private int samplesPerFrame;
    private int frameIndex;
    private int lastFrameBlocks;
}
