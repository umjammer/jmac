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

import davaguine.jmac.info.APEFileInfo;
import davaguine.jmac.info.APEInfo;
import davaguine.jmac.info.APETag;
import davaguine.jmac.info.SpecialFrame;
import davaguine.jmac.info.WaveFormat;
import davaguine.jmac.info.WaveHeader;
import davaguine.jmac.prediction.IPredictorDecompress;
import davaguine.jmac.prediction.PredictorDecompress3950toCurrent;
import davaguine.jmac.prediction.PredictorDecompressNormal3930to3950;
import davaguine.jmac.tools.CircleBuffer;
import davaguine.jmac.tools.Crc32;
import davaguine.jmac.tools.File;
import davaguine.jmac.tools.JMACException;
import davaguine.jmac.tools.Prepare;


/**
 * @author Dmitry Vaguine
 * @version 04.03.2004 14:51:31
 */
public class APEDecompress extends IAPEDecompress {

    private final static int DECODE_BLOCK_SIZE = 4096;

    public APEDecompress(APEInfo apeInfo) {
        this(apeInfo, -1, -1);
    }

    public APEDecompress(APEInfo apeInfo, int startBlock) {
        this(apeInfo, startBlock, -1);
    }

    public APEDecompress(APEInfo apeInfo, int startBlock, int finishBlock) {
        // open / analyze the file
        this.apeInfo = apeInfo;

        // version check (this implementation only works with 3.93 and later files)
        if (this.apeInfo.getApeInfoFileVersion() < 3930)
            throw new JMACException("Unsupported Version");

        // get format information
        wfeInput = this.apeInfo.getApeInfoWaveFormatEx();
        blockAlign = this.apeInfo.getApeInfoBlockAlign();

        // initialize other stuff
        decompressorInitialized = false;
        currentFrame = 0;
        realFrame = 0;
        currentBlock = 0;
        currentFrameBufferBlock = 0;
        frameBufferFinishedBlocks = 0;
        errorDecodingCurrentFrame = false;

        // set the "real" start and finish blocks
        this.startBlock = (startBlock < 0) ? 0 : Math.min(startBlock, this.apeInfo.getApeInfoTotalBlocks());
        this.finishBlock = (finishBlock < 0) ? this.apeInfo.getApeInfoTotalBlocks() : Math.min(finishBlock, this.apeInfo.getApeInfoTotalBlocks());
        isRanged = (this.startBlock != 0) || (this.finishBlock != this.apeInfo.getApeInfoTotalBlocks());
    }

    @Override
    public int getData(byte[] buffer, int blocks) throws IOException {
        initializeDecompressor();

        // cap
        int blocksUntilFinish = finishBlock - currentBlock;
        int blocksToRetrieve = Math.min(blocks, blocksUntilFinish);

        // get the data
        int blocksLeft = blocksToRetrieve;
        int blocksThisPass = 1;
        int index = 0;
        while ((blocksLeft > 0) && (blocksThisPass > 0)) {
            // fill up the frame buffer
            fillFrameBuffer();

            // analyze how much to remove from the buffer
            int frameBufferBlocks = frameBufferFinishedBlocks;
            blocksThisPass = Math.min(blocksLeft, frameBufferBlocks);

            // remove as much as possible
            if (blocksThisPass > 0) {
                frameBuffer.get(buffer, index, blocksThisPass * blockAlign);
                index += blocksThisPass * blockAlign;
                blocksLeft -= blocksThisPass;
                frameBufferFinishedBlocks -= blocksThisPass;
            }
        }

        // calculate the blocks retrieved
        int blocksRetrieved = blocksToRetrieve - blocksLeft;

        // update position
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

        // seek to the perfect location
        int baseFrame = blockOffset / apeInfo.getApeInfoBlocksPerFrame();
        int blocksToSkip = blockOffset % apeInfo.getApeInfoBlocksPerFrame();
        int bytesToSkip = blocksToSkip * blockAlign;

        currentBlock = baseFrame * this.getApeInfoBlocksPerFrame();
        currentFrameBufferBlock = baseFrame * this.getApeInfoBlocksPerFrame();
        currentFrame = baseFrame;
        frameBufferFinishedBlocks = 0;
        frameBuffer.empty();
        seekToFrame(currentFrame);

        // skip necessary blocks
        byte[] tempBuffer = new byte[bytesToSkip];

        int blocksRetrieved = getData(tempBuffer, blocksToSkip);
        if (blocksRetrieved != blocksToSkip)
            throw new JMACException("Undefined Error");
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
        if (isRanged || !apeInfo.getApeInfoIoSource().isLocal()) {
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
        } else
            return apeInfo.getApeInfoAverageBitrate();
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

    // file info
    protected int blockAlign;
    protected int currentFrame;
    protected int realFrame;

    // start / finish information
    protected int startBlock;
    protected int finishBlock;
    protected int currentBlock;
    protected boolean isRanged;
    protected boolean decompressorInitialized;

    // decoding tools
    protected WaveFormat wfeInput;
    protected Crc32 crc;
    protected long storedCRC;
    protected int specialCodes;

    public void seekToFrame(int frameIndex) throws IOException {
        int seekRemainder = (apeInfo.getApeInfoSeekByte(frameIndex) - apeInfo.getApeInfoSeekByte(0)) % 4;
        unBitArray.fillAndResetBitArray(frameIndex == realFrame ? -1 : apeInfo.getApeInfoSeekByte(frameIndex) - seekRemainder, seekRemainder * 8);
        realFrame = frameIndex;
    }

    protected void decodeBlocksToFrameBuffer(int blocks) throws IOException {
        // decode the samples
        int blocksProcessed = 0;

        try {
            if (wfeInput.channels == 2) {
                if ((specialCodes & SpecialFrame.SPECIAL_FRAME_LEFT_SILENCE) > 0 &&
                        (specialCodes & SpecialFrame.SPECIAL_FRAME_RIGHT_SILENCE) > 0) {
                    for (blocksProcessed = 0; blocksProcessed < blocks; blocksProcessed++) {
                        Prepare.unprepare(0, 0, wfeInput, frameBuffer.getDirectWritePointer(), crc);
                        frameBuffer.updateAfterDirectWrite(blockAlign);
                    }
                } else if ((specialCodes & SpecialFrame.SPECIAL_FRAME_PSEUDO_STEREO) > 0) {
                    for (blocksProcessed = 0; blocksProcessed < blocks; blocksProcessed++) {
                        int x = newPredictorX.decompressValue(unBitArray.decodeValueRange(bitArrayStateX));
                        Prepare.unprepare(x, 0, wfeInput, frameBuffer.getDirectWritePointer(), crc);
                        frameBuffer.updateAfterDirectWrite(blockAlign);
                    }
                } else {
                    if (apeInfo.getApeInfoFileVersion() >= 3950) {
                        for (blocksProcessed = 0; blocksProcessed < blocks; blocksProcessed++) {
                            int ny = unBitArray.decodeValueRange(bitArrayStateY);
                            int nx = unBitArray.decodeValueRange(bitArrayStateX);
                            int y = newPredictorY.decompressValue(ny, lastX);
                            int x = newPredictorX.decompressValue(nx, y);
                            lastX = x;

                            Prepare.unprepare(x, y, wfeInput, frameBuffer.getDirectWritePointer(), crc);
                            frameBuffer.updateAfterDirectWrite(blockAlign);
                        }
                    } else {
                        for (blocksProcessed = 0; blocksProcessed < blocks; blocksProcessed++) {
                            int x = newPredictorX.decompressValue(unBitArray.decodeValueRange(bitArrayStateX));
                            int y = newPredictorY.decompressValue(unBitArray.decodeValueRange(bitArrayStateY));

                            Prepare.unprepare(x, y, wfeInput, frameBuffer.getDirectWritePointer(), crc);
                            frameBuffer.updateAfterDirectWrite(blockAlign);
                        }
                    }
                }
            } else {
                if ((specialCodes & SpecialFrame.SPECIAL_FRAME_MONO_SILENCE) > 0) {
                    for (blocksProcessed = 0; blocksProcessed < blocks; blocksProcessed++) {
                        Prepare.unprepare(0, 0, wfeInput, frameBuffer.getDirectWritePointer(), crc);
                        frameBuffer.updateAfterDirectWrite(blockAlign);
                    }
                } else {
                    for (blocksProcessed = 0; blocksProcessed < blocks; blocksProcessed++) {
                        int X = newPredictorX.decompressValue(unBitArray.decodeValueRange(bitArrayStateX));
                        Prepare.unprepare(X, 0, wfeInput, frameBuffer.getDirectWritePointer(), crc);
                        frameBuffer.updateAfterDirectWrite(blockAlign);
                    }
                }
            }
        } catch (JMACException e) {
            errorDecodingCurrentFrame = true;
        }

        currentFrameBufferBlock += blocks;
    }

    protected void fillFrameBuffer() throws IOException {
        // determine the maximum blocks we can decode
        // note that we won't do end capping because we can't use data
        // until EndFrame(...) successfully handles the frame
        // that means we may decode a little extra in end capping cases
        // but this allows robust error handling of bad frames
        int maxBlocks = frameBuffer.maxAdd() / blockAlign;

        boolean invalidChecksum = false;

        // loop and decode data
        int blocksLeft = maxBlocks;
        while (blocksLeft > 0) {
            int frameBlocks = this.getApeInfoFrameBlocks(currentFrame);
            if (frameBlocks < 0)
                break;

            int frameOffsetBlocks = currentFrameBufferBlock % this.getApeInfoBlocksPerFrame();
            int frameBlocksLeft = frameBlocks - frameOffsetBlocks;
            int blocksThisPass = Math.min(frameBlocksLeft, blocksLeft);

            // start the frame if we need to
            if (frameOffsetBlocks == 0)
                startFrame();

            // store the frame buffer bytes before we start
            int frameBufferBytes = frameBuffer.maxGet();

            // decode data
            decodeBlocksToFrameBuffer(blocksThisPass);

            // end the frame if we need to
            if ((frameOffsetBlocks + blocksThisPass) >= frameBlocks) {
                endFrame();
                if (errorDecodingCurrentFrame) {
                    // remove any decoded data from the buffer
                    frameBuffer.removeTail(frameBuffer.maxGet() - frameBufferBytes);

                    // add silence
                    byte silence = (this.getApeInfoBitsPerSample() == 8) ? (byte) 127 : (byte) 0;
                    for (int z = 0; z < frameBlocks * blockAlign; z++) {
                        frameBuffer.getDirectWritePointer().append(silence);
                        frameBuffer.updateAfterDirectWrite(1);
                    }

                    // seek to try to synchronize after an error
                    seekToFrame(currentFrame);

                    // save the return value
                    invalidChecksum = true;
                }
            }

            blocksLeft -= blocksThisPass;
        }

        if (invalidChecksum)
            throw new JMACException("Invalid Checksum");
    }

    protected void startFrame() throws IOException {
        crc = new Crc32();

        // get the frame header
        storedCRC = unBitArray.decodeValue(DecodeValueMethod.DECODE_VALUE_METHOD_UNSIGNED_INT);
        errorDecodingCurrentFrame = false;

        // get any 'special' codes if the file uses them (for silence, FALSE stereo, etc.)
        specialCodes = 0;
        if (apeInfo.getApeInfoFileVersion() > 3820) {
            if ((storedCRC & 0x8000_0000L) > 0) {
                specialCodes = (int) unBitArray.decodeValue(DecodeValueMethod.DECODE_VALUE_METHOD_UNSIGNED_INT);
            }
            storedCRC &= 0x7fff_ffff;
        }

        newPredictorX.flush();
        newPredictorY.flush();

        unBitArray.flushState(bitArrayStateX);
        unBitArray.flushState(bitArrayStateY);

        unBitArray.flushBitArray();

        lastX = 0;
    }

    protected void endFrame() {
        frameBufferFinishedBlocks += this.getApeInfoFrameBlocks(currentFrame);
        currentFrame++;
        // finalize
        unBitArray.finalize_();

        // check the CRC
        if (crc.checksum() != storedCRC)
            errorDecodingCurrentFrame = true;
    }

    protected void initializeDecompressor() throws IOException {
        // check if we have anything to do
        if (decompressorInitialized)
            return;

        // update the initialized flag
        decompressorInitialized = true;

        // create a frame buffer
        frameBuffer.createBuffer((this.getApeInfoBlocksPerFrame() + DECODE_BLOCK_SIZE) * blockAlign, blockAlign * 64);

        // create decoding components
        unBitArray = UnBitArrayBase.createUnBitArray(this, apeInfo.getApeInfoFileVersion());

        if (apeInfo.getApeInfoFileVersion() >= 3950) {
            newPredictorX = new PredictorDecompress3950toCurrent(apeInfo.getApeInfoCompressionLevel(), apeInfo.getApeInfoFileVersion());
            newPredictorY = new PredictorDecompress3950toCurrent(apeInfo.getApeInfoCompressionLevel(), apeInfo.getApeInfoFileVersion());
        } else {
            newPredictorX = new PredictorDecompressNormal3930to3950(apeInfo.getApeInfoCompressionLevel(), apeInfo.getApeInfoFileVersion());
            newPredictorY = new PredictorDecompressNormal3930to3950(apeInfo.getApeInfoCompressionLevel(), apeInfo.getApeInfoFileVersion());
        }

        // seek to the beginning
        seek(-1);
    }

    // more decoding components
    protected final APEInfo apeInfo;
    protected UnBitArrayBase unBitArray;
    protected final UnBitArrayState bitArrayStateX = new UnBitArrayState();
    protected final UnBitArrayState bitArrayStateY = new UnBitArrayState();

    protected IPredictorDecompress newPredictorX;
    protected IPredictorDecompress newPredictorY;

    protected int lastX;

    // decoding buffer
    protected boolean errorDecodingCurrentFrame;
    protected int currentFrameBufferBlock;
    protected int frameBufferFinishedBlocks;
    protected final CircleBuffer frameBuffer = new CircleBuffer();
}
