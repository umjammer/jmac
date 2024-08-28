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
import java.net.MalformedURLException;
import java.net.URL;

import davaguine.jmac.info.APEFileInfo;
import davaguine.jmac.info.APEInfo;
import davaguine.jmac.info.APELink;
import davaguine.jmac.info.APETag;
import davaguine.jmac.info.WaveFormat;
import davaguine.jmac.tools.File;
import davaguine.jmac.tools.Globals;
import davaguine.jmac.tools.JMACException;


/**
 * @author Dmitry Vaguine
 * @version 04.03.2004 14:51:31
 */
public abstract class IAPEDecompress {

    /**
     * Gets raw decompressed audio
     *
     * @param buffer a pointer to a buffer to put the data into
     * @param blocks the number of audio blocks desired (see note at intro about blocks vs. samples)
     */
    public abstract int getData(byte[] buffer, int blocks) throws IOException;

    /**
     * Seeks
     *
     * @param blockOffset the block to seek to (see note at intro about blocks vs. samples)
     */
    public abstract void seek(int blockOffset) throws IOException;

    // Get Information

    public abstract int getApeInfoDecompressCurrentBlock();

    public abstract int getApeInfoDecompressCurrentMS();

    public abstract int getApeInfoDecompressTotalBlocks();

    public abstract int getApeInfoDecompressLengthMS();

    public abstract int getApeInfoDecompressCurrentBitRate() throws IOException;

    public abstract int getApeInfoDecompressAverageBitrate() throws IOException;

    public abstract File getApeInfoIoSource();

    public abstract int getApeInfoBlocksPerFrame();

    public abstract int getApeInfoFileVersion();

    public abstract int getApeInfoCompressionLevel();

    public abstract int getApeInfoFormatFlags();

    public abstract int getApeInfoSampleRate();

    public abstract int getApeInfoBitsPerSample();

    public abstract int getApeInfoBytesPerSample();

    public abstract int getApeInfoChannels();

    public abstract int getApeInfoBlockAlign();

    public abstract int getApeInfoFinalFrameBlocks();

    public abstract int getApeInfoTotalFrames();

    public abstract int getApeInfoWavHeaderBytes();

    public abstract int getApeInfoWavTerminatingBytes();

    public abstract int getApeInfoWavDataBytes();

    public abstract int getApeInfoWavTotalBytes();

    public abstract int getApeInfoApeTotalBytes();

    public abstract int getApeInfoTotalBlocks();

    public abstract int getApeInfoLengthMs();

    public abstract int getApeInfoAverageBitrate();

    public abstract int getApeInfoSeekByte(int frame);

    public abstract int getApeInfoFrameBytes(int frame) throws IOException;

    public abstract int getApeInfoFrameBlocks(int frame);

    public abstract int getApeInfoFrameBitrate(int frame) throws IOException;

    public abstract int getApeInfoDecompressedBitrate();

    public abstract int getApeInfoPeakLevel();

    public abstract int getApeInfoSeekBit(int frame);

    public abstract WaveFormat getApeInfoWaveFormatEx();

    public abstract byte[] getApeInfoWavHeaderData(int maxBytes);

    public abstract APETag getApeInfoTag();

    public abstract byte[] getApeInfoWavTerminatingData(int maxBytes) throws IOException;

    public abstract APEFileInfo getApeInfoInternalInfo();

    public static IAPEDecompress CreateIAPEDecompressCore(APEInfo apeInfo, int startBlock, int finishBlock) {
        IAPEDecompress apeDecompress = null;
        if (apeInfo != null) {
            if (apeInfo.getApeInfoFileVersion() >= 3930) {
                if (Globals.NATIVE)
                    apeDecompress = new APEDecompressNative(apeInfo, startBlock, finishBlock);
                else
                    apeDecompress = new APEDecompress(apeInfo, startBlock, finishBlock);
            } else
                apeDecompress = new APEDecompressOld(apeInfo, startBlock, finishBlock);
        }

        return apeDecompress;
    }

    public static APEInfo createAPEInfo(File in) throws IOException {
        // variables
        APEInfo apeInfo = null;

        // get the extension
        if (in.isLocal()) {
            String extension = in.getExtension();

            // take the appropriate action (based on the extension)
            if (extension.equalsIgnoreCase(".mac") || extension.equalsIgnoreCase(".ape"))
                // plain .ape file
                apeInfo = new APEInfo(in);
        } else
            apeInfo = new APEInfo(in);

        // fail if we couldn't get the file information
        if (apeInfo == null)
            throw new JMACException("Invalid Input File");
        return apeInfo;
    }

    public static IAPEDecompress createAPEDecompress(File in) throws IOException {
        // variables
        APEInfo apeInfo = null;
        int startBlock = -1;
        int finishBlock = -1;

        // get the extension
        if (in.isLocal()) {
            String filename = in.getFilename();
            String extension = in.getExtension();

            // take the appropriate action (based on the extension)
            if (extension.equalsIgnoreCase(".apl")) {
                // "link" file (.apl linked large APE file)
                APELink apeLink = new APELink(filename);
                if (apeLink.isLinkFile()) {
                    URL url = null;
                    try {
                        url = new URL(apeLink.getImageFilename());
                        apeInfo = new APEInfo(url);
                    } catch (MalformedURLException e) {
                        apeInfo = new APEInfo(new java.io.File(apeLink.getImageFilename()));
                    }
                    startBlock = apeLink.getStartBlock();
                    finishBlock = apeLink.getFinishBlock();
                }
            } else if (extension.equalsIgnoreCase(".mac") || extension.equalsIgnoreCase(".ape"))
                // plain .ape file
                apeInfo = new APEInfo(in);
        } else
            apeInfo = new APEInfo(in);

        // fail if we couldn't get the file information
        if (apeInfo == null)
            throw new JMACException("Invalid Input File");

        // create and return
        IAPEDecompress apeDecompress = CreateIAPEDecompressCore(apeInfo, startBlock, finishBlock);
        return apeDecompress;
    }

    public static IAPEDecompress createIAPEDecompressEx(APEInfo apeInfo, int startBlock, int finishBlock) {
        return CreateIAPEDecompressCore(apeInfo, startBlock, finishBlock);
    }
}
