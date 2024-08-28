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
import java.util.Arrays;

import davaguine.jmac.info.APEFileInfo;
import davaguine.jmac.info.APEInfo;
import davaguine.jmac.info.APETag;
import davaguine.jmac.info.WaveFormat;
import davaguine.jmac.info.WaveHeader;
import davaguine.jmac.tools.ByteBuffer;
import davaguine.jmac.tools.File;
import davaguine.jmac.tools.JMACException;


/**
 * @author Dmitry Vaguine
 * @version 04.03.2004 14:51:31
 */
public class APEDecompressOld extends IAPEDecompress {

    public APEDecompressOld(APEInfo apeInfo) {
        this(apeInfo, -1, -1);
    }

    public APEDecompressOld(APEInfo apeInfo, int startBlock) {
        this(apeInfo, startBlock, -1);
    }

    public APEDecompressOld(APEInfo apeInfo, int startBlock, int finishBlock) {
        // open / analyze the file
        this.apeInfo = apeInfo;

        // version check (this implementation only works with 3.92 and earlier files)
        if (getApeInfoFileVersion() > 3920)
            throw new JMACException("Wrong Version");

        // create the buffer
        blockAlign = getApeInfoBlockAlign();

        // initialize other stuff
        bufferTail = 0;
        decompressorInitialized = false;
        currentFrame = 0;
        currentBlock = 0;

        // set the "real" start and finish blocks
        this.startBlock = (startBlock < 0) ? 0 : Math.min(startBlock, getApeInfoTotalBlocks());
        this.finishBlock = (finishBlock < 0) ? getApeInfoTotalBlocks() : Math.min(finishBlock, getApeInfoTotalBlocks());
        isRanged = (this.startBlock != 0) || (this.finishBlock != getApeInfoTotalBlocks());
    }

    @Override
    public int getData(byte[] buffer, int blocks) throws IOException {
        initializeDecompressor();

        // cap
        int blocksUntilFinish = finishBlock - currentBlock;
        blocks = Math.min(blocks, blocksUntilFinish);

        int blocksRetrieved = 0;

        // fulfill as much of the request as possible
        int totalBytesNeeded = blocks * blockAlign;
        int bytesLeft = totalBytesNeeded;
        int blocksDecoded = 1;

        while (bytesLeft > 0 && blocksDecoded > 0) {
            // empty the buffer
            int bytesAvailable = bufferTail;
            int intialBytes = Math.min(bytesLeft, bytesAvailable);
            if (intialBytes > 0) {
                System.arraycopy(this.buffer, 0, buffer, totalBytesNeeded - bytesLeft, intialBytes);

                if ((bufferTail - intialBytes) > 0)
                    System.arraycopy(this.buffer, intialBytes, this.buffer, 0, bufferTail - intialBytes);

                bytesLeft -= intialBytes;
                bufferTail -= intialBytes;

            }

            // decode more
            if (bytesLeft > 0) {
                output.reset(this.buffer, bufferTail);
                blocksDecoded = unMAC.decompressFrame(output, currentFrame++);
                bufferTail += (blocksDecoded * blockAlign);
            }
        }

        blocksRetrieved = (totalBytesNeeded - bytesLeft) / blockAlign;

        // update the position
        currentBlock += blocksRetrieved;

        return blocksRetrieved;
    }

    @Override
    public void seek(int blockOffset) throws IOException {
        initializeDecompressor();

        // use the offset
        blockOffset += startBlock;

        // cap (to prevent seeking too far)
        if (blockOffset >= finishBlock)
            blockOffset = finishBlock - 1;
        if (blockOffset < startBlock)
            blockOffset = startBlock;

        // flush the buffer
        bufferTail = 0;

        // seek to the perfect location
        int baseFrame = blockOffset / getApeInfoBlocksPerFrame();
        int blocksToSkip = blockOffset % getApeInfoBlocksPerFrame();
        int bytesToSkip = blocksToSkip * blockAlign;

        // skip necessary blocks
        int maximumDecompressedFrameBytes = blockAlign * getApeInfoBlocksPerFrame();
        byte[] tempBuffer = new byte[maximumDecompressedFrameBytes + 16];
        Arrays.fill(tempBuffer, (byte) 0);

        currentFrame = baseFrame;

        output.reset(tempBuffer);
        int blocksDecoded = unMAC.decompressFrame(output, currentFrame++);

        if (blocksDecoded == -1)
            throw new JMACException("Error While Decoding");

        int bytesToKeep = (blocksDecoded * blockAlign) - bytesToSkip;
        System.arraycopy(tempBuffer, bytesToSkip, buffer, bufferTail, bytesToKeep);
        bufferTail += bytesToKeep;

        currentBlock = blockOffset;
    }

    // buffer
    protected byte[] buffer;
    protected int bufferTail;
    protected final ByteBuffer output = new ByteBuffer();

    // file info
    protected int blockAlign;
    protected int currentFrame;

    // start / finish information
    protected int startBlock;
    protected int finishBlock;
    protected int currentBlock;
    protected boolean isRanged;

    // decoding tools
    protected final UnMAC unMAC = new UnMAC();
    protected final APEInfo apeInfo;

    protected boolean decompressorInitialized;

    protected void initializeDecompressor() throws IOException {
        // check if we have anything to do
        if (decompressorInitialized)
            return;

        // initialize the core
        unMAC.initialize(this);

        int maximumDecompressedFrameBytes = blockAlign * getApeInfoBlocksPerFrame();
        int totalBufferBytes = Math.max(65536, (maximumDecompressedFrameBytes + 16) * 2);
        buffer = new byte[totalBufferBytes];

        // update the initialized flag
        decompressorInitialized = true;

        // seek to the beginning
        seek(0);
    }

    @Override
    public int getApeInfoDecompressCurrentBlock() {
        return currentBlock - startBlock;
    }

    @Override
    public int getApeInfoDecompressCurrentMS() {
        int sampleRate = apeInfo.getApeInfoSampleRate();
        if (sampleRate > 0)
            return (int) ((currentBlock * 1000L) / sampleRate);
        return 0;
    }

    @Override
    public int getApeInfoDecompressTotalBlocks() {
        return finishBlock - startBlock;
    }

    @Override
    public int getApeInfoDecompressLengthMS() {
        int sampleRate = apeInfo.getApeInfoSampleRate();
        if (sampleRate > 0)
            return (int) (((finishBlock - startBlock) * 1000L) / sampleRate);
        return 0;
    }

    @Override
    public int getApeInfoDecompressCurrentBitRate() throws IOException {
        return apeInfo.getApeInfoFrameBitrate(currentFrame);
    }

    @Override
    public int getApeInfoDecompressAverageBitrate() throws IOException {
        if (isRanged) {
            // figure the frame range
            int blocksPerFrame = apeInfo.getApeInfoBlocksPerFrame();
            int startFrame = startBlock / blocksPerFrame;
            int finishFrame = (finishBlock + blocksPerFrame - 1) / blocksPerFrame;

            // get the number of bytes in the first and last frame
            int totalBytes = (apeInfo.getApeInfoFrameBytes(startFrame) * (startBlock % blocksPerFrame)) / blocksPerFrame;
            if (finishFrame != startFrame)
                totalBytes += (apeInfo.getApeInfoFrameBytes(finishFrame) * (finishBlock % blocksPerFrame)) / blocksPerFrame;

            // get the number of bytes in between
            int totalFrames = apeInfo.getApeInfoTotalFrames();
            for (int frame = startFrame + 1; (frame < finishFrame) && (frame < totalFrames); frame++)
                totalBytes += apeInfo.getApeInfoFrameBytes(frame);

            // figure the bitrate
            int totalMS = (int) (((finishBlock - startBlock) * 1000L) / apeInfo.getApeInfoSampleRate());
            if (totalMS != 0)
                return (totalBytes * 8) / totalMS;
        } else {
            return apeInfo.getApeInfoAverageBitrate();
        }
        return 0;
    }

    @Override
    public int getApeInfoWavHeaderBytes() {
        if (isRanged)
            return WaveHeader.WAVE_HEADER_BYTES;
        return apeInfo.getApeInfoWavHeaderBytes();
    }

    @Override
    public byte[] getApeInfoWavHeaderData(int maxBytes) {
        if (isRanged) {
            if (WaveHeader.WAVE_HEADER_BYTES > maxBytes)
                return null;
            else {
                WaveFormat wfeFormat = apeInfo.getApeInfoWaveFormatEx();
                WaveHeader WAVHeader = new WaveHeader();
                WaveHeader.fillWaveHeader(WAVHeader, (finishBlock - startBlock) * apeInfo.getApeInfoBlockAlign(), wfeFormat, 0);
                return WAVHeader.write();
            }
        }
        return apeInfo.getApeInfoWavHeaderData(maxBytes);
    }

    @Override
    public int getApeInfoWavTerminatingBytes() {
        if (isRanged)
            return 0;
        else
            return apeInfo.getApeInfoWavTerminatingBytes();
    }

    @Override
    public byte[] getApeInfoWavTerminatingData(int maxBytes) throws IOException {
        if (isRanged)
            return null;
        else
            return apeInfo.getApeInfoWavTerminatingData(maxBytes);
    }

    @Override
    public WaveFormat getApeInfoWaveFormatEx() {
        return apeInfo.getApeInfoWaveFormatEx();
    }

    @Override
    public File getApeInfoIoSource() {
        return apeInfo.getApeInfoIoSource();
    }

    @Override
    public int getApeInfoBlocksPerFrame() {
        return apeInfo.getApeInfoBlocksPerFrame();
    }

    @Override
    public int getApeInfoFileVersion() {
        return apeInfo.getApeInfoFileVersion();
    }

    @Override
    public int getApeInfoCompressionLevel() {
        return apeInfo.getApeInfoCompressionLevel();
    }

    @Override
    public int getApeInfoFormatFlags() {
        return apeInfo.getApeInfoFormatFlags();
    }

    @Override
    public int getApeInfoSampleRate() {
        return apeInfo.getApeInfoSampleRate();
    }

    @Override
    public int getApeInfoBitsPerSample() {
        return apeInfo.getApeInfoBitsPerSample();
    }

    @Override
    public int getApeInfoBytesPerSample() {
        return apeInfo.getApeInfoBytesPerSample();
    }

    @Override
    public int getApeInfoChannels() {
        return apeInfo.getApeInfoChannels();
    }

    @Override
    public int getApeInfoBlockAlign() {
        return apeInfo.getApeInfoBlockAlign();
    }

    @Override
    public int getApeInfoFinalFrameBlocks() {
        return apeInfo.getApeInfoFinalFrameBlocks();
    }

    @Override
    public int getApeInfoTotalFrames() {
        return apeInfo.getApeInfoTotalFrames();
    }

    @Override
    public int getApeInfoWavDataBytes() {
        return apeInfo.getApeInfoWavDataBytes();
    }

    @Override
    public int getApeInfoWavTotalBytes() {
        return apeInfo.getApeInfoWavTotalBytes();
    }

    @Override
    public int getApeInfoApeTotalBytes() {
        return apeInfo.getApeInfoApeTotalBytes();
    }

    @Override
    public int getApeInfoTotalBlocks() {
        return apeInfo.getApeInfoTotalBlocks();
    }

    @Override
    public int getApeInfoLengthMs() {
        return apeInfo.getApeInfoLengthMs();
    }

    @Override
    public int getApeInfoAverageBitrate() {
        return apeInfo.getApeInfoAverageBitrate();
    }

    @Override
    public int getApeInfoSeekByte(int frame) {
        return apeInfo.getApeInfoSeekByte(frame);
    }

    @Override
    public int getApeInfoFrameBytes(int frame) throws IOException {
        return apeInfo.getApeInfoFrameBytes(frame);
    }

    @Override
    public int getApeInfoFrameBlocks(int frame) {
        return apeInfo.getApeInfoFrameBlocks(frame);
    }

    @Override
    public int getApeInfoFrameBitrate(int frame) throws IOException {
        return apeInfo.getApeInfoFrameBitrate(frame);
    }

    @Override
    public int getApeInfoDecompressedBitrate() {
        return apeInfo.getApeInfoDecompressedBitrate();
    }

    @Override
    public int getApeInfoPeakLevel() {
        return apeInfo.getApeInfoPeakLevel();
    }

    @Override
    public int getApeInfoSeekBit(int frame) {
        return apeInfo.getApeInfoSeekBit(frame);
    }

    @Override
    public APETag getApeInfoTag() {
        return apeInfo.getApeInfoTag();
    }

    @Override
    public APEFileInfo getApeInfoInternalInfo() {
        return apeInfo.getApeInfoInternalInfo();
    }
}
