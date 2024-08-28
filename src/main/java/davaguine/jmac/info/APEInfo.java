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

package davaguine.jmac.info;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.net.URL;

import davaguine.jmac.tools.File;
import davaguine.jmac.tools.InputStreamFile;
import davaguine.jmac.tools.JMACException;
import davaguine.jmac.tools.RandomAccessFile;

import static java.lang.System.getLogger;


/**
 * @author Dmitry Vaguine
 * @version 04.03.2004 14:51:31
 */
public class APEInfo {

    private static final Logger logger = getLogger(APEInfo.class.getName());

    // construction and destruction

    public APEInfo(URL url) throws IOException {
        this(url, null);
    }

    public APEInfo(URL url, APETag tag) throws IOException {
        this(url.openStream(), tag);
    }

    public APEInfo(java.io.File file) throws IOException {
        this(file, null);
    }

    public APEInfo(java.io.File file, APETag tag) throws IOException {
        this(new RandomAccessFile(file, "r"), tag);
    }

    public APEInfo(InputStream io) throws IOException {
        this(io, null);
    }

    public APEInfo(InputStream io, APETag tag) throws IOException {
        this(new InputStreamFile(io), tag);
    }

    public APEInfo(File io) throws IOException {
        this(io, null);
    }

    public APEInfo(File io, APETag tag) throws IOException {
        // open the file
        this.io = io;
//logger.log(Level.DEBUG, "io: " + io);

        // get the file information
        getFileInformation();

        // get the tag (do this second so that we don't do it on failure)
        if (tag == null) {
            // we don't want to analyze right away for non-local files
            // since a single I/O object is shared, we can't tag and read at the same time (i.e. in multiple threads)

            apeTag = new APETag(this.io, io.isLocal());
        } else
            apeTag = tag;
    }

    public void close() throws IOException {
        apeFileInfo.waveHeaderData = null;
        apeFileInfo.seekBitTable = null;
        apeFileInfo.seekByteTable = null;
        apeFileInfo.apeDescriptor = null;
        apeTag = null;

        // re-initialize variables
        apeFileInfo.seekTableElements = 0;
        hasFileInformationLoaded = false;
    }

    public int getApeInfoFileVersion() {
        return apeFileInfo.version;
    }

    public int getApeInfoCompressionLevel() {
        return apeFileInfo.compressionLevel;
    }

    public int getApeInfoFormatFlags() {
        return apeFileInfo.formatFlags;
    }

    public int getApeInfoSampleRate() {
        return apeFileInfo.sampleRate;
    }

    public int getApeInfoBitsPerSample() {
        return apeFileInfo.bitsPerSample;
    }

    public int getApeInfoBytesPerSample() {
        return apeFileInfo.bytesPerSample;
    }

    public int getApeInfoChannels() {
        return apeFileInfo.channels;
    }

    public int getApeInfoBlockAlign() {
        return apeFileInfo.blockAlign;
    }

    public int getApeInfoBlocksPerFrame() {
        return apeFileInfo.blocksPerFrame;
    }

    public int getApeInfoFinalFrameBlocks() {
        return apeFileInfo.finalFrameBlocks;
    }

    public int getApeInfoTotalFrames() {
        return apeFileInfo.totalFrames;
    }

    public int getApeInfoWavHeaderBytes() {
        return apeFileInfo.wavHeaderBytes;
    }

    public int getApeInfoWavTerminatingBytes() {
        return apeFileInfo.wavTerminatingBytes;
    }

    public int getApeInfoWavDataBytes() {
        return apeFileInfo.wavDataBytes;
    }

    public int getApeInfoWavTotalBytes() {
        return apeFileInfo.wavTotalBytes;
    }

    public int getApeInfoApeTotalBytes() {
        return apeFileInfo.apeTotalBytes;
    }

    public int getApeInfoTotalBlocks() {
        return apeFileInfo.totalBlocks;
    }

    public int getApeInfoLengthMs() {
        return apeFileInfo.lengthMS;
    }

    public int getApeInfoAverageBitrate() {
        return apeFileInfo.averageBitrate;
    }

    public int getApeInfoSeekByte(int frame) {
        return (frame < 0 || frame >= apeFileInfo.totalFrames) ? 0 : apeFileInfo.seekByteTable[frame] + apeFileInfo.junkHeaderBytes;
    }

    public int getApeInfoFrameBytes(int frame) throws IOException {
        if ((frame < 0) || (frame >= apeFileInfo.totalFrames))
            return -1;
        else {
            if (frame != (apeFileInfo.totalFrames - 1))
                return getApeInfoSeekByte(frame + 1) - getApeInfoSeekByte(frame);
            else {
                if (io.isLocal())
                    return (int) io.length() - apeTag.getTagBytes() - apeFileInfo.wavTerminatingBytes - getApeInfoSeekByte(frame);
                else if (frame > 0)
                    return getApeInfoSeekByte(frame) - getApeInfoSeekByte(frame - 1);
                else
                    return -1;
            }
        }
    }

    public int getApeInfoFrameBlocks(int frame) {
        if ((frame < 0) || (frame >= apeFileInfo.totalFrames))
            return -1;
        else {
            if (frame != (apeFileInfo.totalFrames - 1))
                return apeFileInfo.blocksPerFrame;
            else
                return apeFileInfo.finalFrameBlocks;
        }
    }

    public int getApeInfoFrameBitrate(int frame) throws IOException {
        int frameBytes = getApeInfoFrameBytes(frame);
        int frameBlocks = getApeInfoFrameBlocks(frame);
        if ((frameBytes > 0) && (frameBlocks > 0) && apeFileInfo.sampleRate > 0) {
            int frameMS = (frameBlocks * 1000) / apeFileInfo.sampleRate;
            if (frameMS != 0) {
                return (frameBytes * 8) / frameMS;
            }
        }
        return apeFileInfo.averageBitrate;
    }

    public int getApeInfoDecompressedBitrate() {
        return apeFileInfo.decompressedBitrate;
    }

    public int getApeInfoPeakLevel() {
        return apeFileInfo.peakLevel;
    }

    public int getApeInfoSeekBit(int frame) {
        if (getApeInfoFileVersion() > 3800)
            return 0;
        else {
            if (frame < 0 || frame >= apeFileInfo.totalFrames)
                return 0;
            else
                return apeFileInfo.seekBitTable[frame];
        }
    }

    public WaveFormat getApeInfoWaveFormatEx() {
        WaveFormat waveFormatEx = new WaveFormat();
        WaveFormat.fillWaveFormatEx(waveFormatEx, apeFileInfo.sampleRate, apeFileInfo.bitsPerSample, apeFileInfo.channels);
        return waveFormatEx;
    }

    public byte[] getApeInfoWavHeaderData(int maxBytes) {
        if ((apeFileInfo.formatFlags & APEHeader.MAC_FORMAT_FLAG_CREATE_WAV_HEADER) > 0) {
            if (WaveHeader.WAVE_HEADER_BYTES > maxBytes)
                return null;
            else {
                WaveFormat wfeFormat = getApeInfoWaveFormatEx();
                WaveHeader WAVHeader = new WaveHeader();
                WaveHeader.fillWaveHeader(WAVHeader, apeFileInfo.wavDataBytes, wfeFormat, apeFileInfo.wavTerminatingBytes);
                return WAVHeader.write();
            }
        } else {
            if (apeFileInfo.wavHeaderBytes > maxBytes)
                return null;
            else {
                byte[] buffer = new byte[apeFileInfo.wavHeaderBytes];
                System.arraycopy(apeFileInfo.waveHeaderData, 0, buffer, 0, apeFileInfo.wavHeaderBytes);
                return buffer;
            }
        }
    }

    public File getApeInfoIoSource() {
        return io;
    }

    public APETag getApeInfoTag() {
        return apeTag;
    }

    public byte[] getApeInfoWavTerminatingData(int maxBytes) throws IOException {
        if (apeFileInfo.wavTerminatingBytes > maxBytes)
            return null;
        else {
            if (apeFileInfo.wavTerminatingBytes > 0) {
                // variables
                long originalFileLocation = io.getFilePointer();

                // check for a tag
                io.seek(io.length() - (apeTag.getTagBytes() + apeFileInfo.wavTerminatingBytes));
                byte[] buffer = new byte[apeFileInfo.wavTerminatingBytes];
                try {
                    io.readFully(buffer);
                } catch (EOFException e) {
                    throw new JMACException("Can't Read WAV Terminating Bytes");
                }

                // restore the file pointer
                io.seek(originalFileLocation);
                return buffer;
            }
            return null;
        }
    }

    public APEFileInfo getApeInfoInternalInfo() {
        return apeFileInfo;
    }

    private void getFileInformation() throws IOException {
        // quit if the file information has already been loaded
        if (hasFileInformationLoaded)
            return;

        // use a CAPEHeader class to help us analyze the file
        APEHeader apeHeader = new APEHeader(io);
        apeHeader.analyze(apeFileInfo);

        hasFileInformationLoaded = true;
    }

    // internal variables
    private boolean hasFileInformationLoaded;
    private final File io;
    private APETag apeTag;
    private final APEFileInfo apeFileInfo = new APEFileInfo();
}
