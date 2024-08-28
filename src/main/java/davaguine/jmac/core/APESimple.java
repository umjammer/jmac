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

package davaguine.jmac.core;

import java.io.IOException;
import java.io.RandomAccessFile;

import davaguine.jmac.decoder.IAPEDecompress;
import davaguine.jmac.encoder.IAPECompress;
import davaguine.jmac.info.APEFileInfo;
import davaguine.jmac.info.InputSource;
import davaguine.jmac.info.WaveFormat;
import davaguine.jmac.tools.File;
import davaguine.jmac.tools.Globals;
import davaguine.jmac.tools.IntegerPointer;
import davaguine.jmac.tools.JMACException;
import davaguine.jmac.tools.JMACSkippedException;
import davaguine.jmac.tools.JMACStoppedByUserException;
import davaguine.jmac.tools.MD5;
import davaguine.jmac.tools.ProgressCallback;
import davaguine.jmac.tools.ProgressHelper;


/**
 * @author Dmitry Vaguine
 * @version 11.05.2004 16:26:19
 */
public class APESimple {

    public final static int UNMAC_DECODER_OUTPUT_APE = 2;
    public final static int UNMAC_DECODER_OUTPUT_WAV = 1;
    public final static int UNMAC_DECODER_OUTPUT_NONE = 0;
    public final static int BLOCKS_PER_DECODE = 9216;

    public static void verifyFile(String inputFilename, ProgressCallback progressor) throws IOException, JMACSkippedException, JMACStoppedByUserException {
        // error check the function parameters
        if (inputFilename == null)
            throw new JMACException("Bad Parameters");

        // see if we can quickly verify
        File file = File.createFile(inputFilename, "r");
        IAPEDecompress apeDecompress = null;
        try {
            apeDecompress = IAPEDecompress.createAPEDecompress(file);

            APEFileInfo info = apeDecompress.getApeInfoInternalInfo();

            if ((info.version < 3980) || (info.apeDescriptor == null))
                decompressCore(apeDecompress, null, UNMAC_DECODER_OUTPUT_NONE, -1, progressor);
            else {
                MD5 MD5Helper = new MD5();

                File io = apeDecompress.getApeInfoIoSource();

                int head = (int) (info.junkHeaderBytes + info.apeDescriptor.descriptorBytes);
                int start = (int) (head + info.apeDescriptor.headerBytes + info.apeDescriptor.seekTableBytes);

                io.seek(head);
                int headBytes = start - head;
                byte[] headBuffer = new byte[headBytes];
                io.readFully(headBuffer);

                int bytesLeft = (int) (info.apeDescriptor.headerDataBytes + info.apeDescriptor.apeFrameDataBytes + info.apeDescriptor.terminatingDataBytes);
                // create the progress helper
                ProgressHelper macProgressHelper = new ProgressHelper(bytesLeft / 16384, progressor);
                byte[] buffer = new byte[16384];
                int bytesRead = 1;
                while ((bytesLeft > 0) && (bytesRead > 0)) {
                    int bytesToRead = Math.min(16384, bytesLeft);
                    bytesRead = io.read(buffer, 0, bytesToRead);

                    MD5Helper.update(buffer, bytesRead);
                    bytesLeft -= bytesRead;

                    macProgressHelper.updateProgress();
                    if (macProgressHelper.isKillFlag())
                        throw new JMACStoppedByUserException();
                }

                if (bytesLeft != 0)
                    throw new JMACException("The File Is Broken");

                MD5Helper.update(headBuffer, headBytes);

                // fire the "complete" progress notification
                macProgressHelper.updateProgressComplete();

                byte[] result = MD5Helper.final_();
                for (int i = 0; i < 16; i++)
                    if (result[i] != info.apeDescriptor.fileMD5[i])
                        throw new JMACException("Invalid Checksum");
            }
        } finally {
            file.close();
        }
    }

    /**
     * Convert file
     */
    public static void convertFile(String inputFilename, String outputFilename, int compressionLevel, ProgressCallback progressor) throws IOException, JMACSkippedException, JMACStoppedByUserException {
        decompressCore(inputFilename, outputFilename, UNMAC_DECODER_OUTPUT_APE, compressionLevel, progressor);
    }

    /**
     * Decompress file
     */
    public static void decompressFile(String inputFilename, String outputFilename, ProgressCallback progressor) throws IOException, JMACSkippedException, JMACStoppedByUserException {
        if (outputFilename == null)
            verifyFile(inputFilename, progressor);
        else
            decompressCore(inputFilename, outputFilename, UNMAC_DECODER_OUTPUT_WAV, -1, progressor);
    }

    public static void decompressCore(String inputFilename, String outputFilename, int outputMode, int compressionLevel, ProgressCallback progressor) throws IOException, JMACSkippedException, JMACStoppedByUserException {
        // error check the function parameters
        if (inputFilename == null)
            throw new JMACException("Bad Parameters");

        // variable declares
        IAPEDecompress apeDecompress;
        File file = File.createFile(inputFilename, "r");
        try {
            apeDecompress = IAPEDecompress.createAPEDecompress(file);
            decompressCore(apeDecompress, outputFilename, outputMode, compressionLevel, progressor);
        } finally {
            file.close();
        }
    }

    public static void decompressCore(IAPEDecompress apeDecompress, String outputFilename, int outputMode, int compressionLevel, ProgressCallback progressor) throws IOException, JMACSkippedException, JMACStoppedByUserException {
        // variable declares
        java.io.RandomAccessFile output = null;
        IAPECompress apeCompress = null;

        try {
            // create the core
            WaveFormat wfeInput = apeDecompress.getApeInfoWaveFormatEx();

            // allocate space for the header
            byte[] waveHeaderBuffer = apeDecompress.getApeInfoWavHeaderData(apeDecompress.getApeInfoWavHeaderBytes());

            // initialize the output
            if (outputMode == UNMAC_DECODER_OUTPUT_WAV) {
                // create the file
                output = new RandomAccessFile(outputFilename, "rw");

                // output the header
                output.write(waveHeaderBuffer);
            } else if (outputMode == UNMAC_DECODER_OUTPUT_APE) {
                // quit if there is nothing to do
                if (apeDecompress.getApeInfoFileVersion() == Globals.MAC_VERSION_NUMBER && apeDecompress.getApeInfoCompressionLevel() == compressionLevel)
                    throw new JMACSkippedException();

                // create and start the compressor
                apeCompress = IAPECompress.createIAPECompress();
                apeCompress.start(outputFilename, wfeInput, apeDecompress.getApeInfoDecompressTotalBlocks() * apeDecompress.getApeInfoBlockAlign(),
                        compressionLevel, waveHeaderBuffer, apeDecompress.getApeInfoWavHeaderBytes());
            }

            int blockAlign = apeDecompress.getApeInfoBlockAlign();
            // allocate space for decompression
            byte[] tempBuffer = new byte[blockAlign * BLOCKS_PER_DECODE];

            int blocksLeft = apeDecompress.getApeInfoDecompressTotalBlocks();

            // create the progress helper
            ProgressHelper macProgressHelper = new ProgressHelper(blocksLeft / BLOCKS_PER_DECODE, progressor);

            // main decoding loop
            while (blocksLeft > 0) {
                // decode data
                int blocksDecoded = apeDecompress.getData(tempBuffer, BLOCKS_PER_DECODE);

                // handle the output
                if (outputMode == UNMAC_DECODER_OUTPUT_WAV)
                    output.write(tempBuffer, 0, blocksDecoded * blockAlign);
                else if (outputMode == UNMAC_DECODER_OUTPUT_APE)
                    apeCompress.addData(tempBuffer, blocksDecoded * apeDecompress.getApeInfoBlockAlign());

                // update amount remaining
                blocksLeft -= blocksDecoded;

                // update progress and kill flag
                macProgressHelper.updateProgress();
                if (macProgressHelper.isKillFlag())
                    throw new JMACStoppedByUserException();
            }

            // terminate the output
            if (outputMode == UNMAC_DECODER_OUTPUT_WAV) {
                // write any terminating WAV data
                if (apeDecompress.getApeInfoWavTerminatingBytes() > 0) {
                    byte[] termData = apeDecompress.getApeInfoWavTerminatingData(apeDecompress.getApeInfoWavTerminatingBytes());

                    int bytesToWrite = apeDecompress.getApeInfoWavTerminatingBytes();
                    output.write(termData, 0, bytesToWrite);
                }
            } else if (outputMode == UNMAC_DECODER_OUTPUT_APE) {
                // write the WAV data and any tag
                int tagBytes = apeDecompress.getApeInfoTag().getTagBytes();
                boolean hasTag = (tagBytes > 0);
                int terminatingBytes = tagBytes;
                terminatingBytes += apeDecompress.getApeInfoWavTerminatingBytes();

                if (terminatingBytes > 0) {
                    tempBuffer = apeDecompress.getApeInfoWavTerminatingData(terminatingBytes);

                    if (hasTag) {
                        apeDecompress.getApeInfoIoSource().seek(apeDecompress.getApeInfoIoSource().length() - tagBytes);
                        apeDecompress.getApeInfoIoSource().read(tempBuffer, apeDecompress.getApeInfoWavTerminatingBytes(), tagBytes);
                    }

                    apeCompress.finish(tempBuffer, terminatingBytes, apeDecompress.getApeInfoWavTerminatingBytes());
                } else
                    apeCompress.finish(null, 0, 0);
            }

            // fire the "complete" progress notification
            macProgressHelper.updateProgressComplete();
        } finally {
            if (output != null)
                output.close();
            if (apeCompress != null)
                apeCompress.kill();
        }
    }

    public static void compressFile(String inputFilename, String outputFilename, int compressionLevel, ProgressCallback progressor) throws IOException, JMACStoppedByUserException {
        // declare the variables
        IAPECompress apeCompress = null;
        InputSource inputSource = null;

        try {
            byte[] buffer = null;

            WaveFormat waveFormatEx = new WaveFormat();

            // create the input source
            IntegerPointer audioBlocks = new IntegerPointer();
            audioBlocks.value = 0;
            IntegerPointer headerBytes = new IntegerPointer();
            headerBytes.value = 0;
            IntegerPointer terminatingBytes = new IntegerPointer();
            terminatingBytes.value = 0;
            inputSource = InputSource.createInputSource(inputFilename, waveFormatEx, audioBlocks,
                    headerBytes, terminatingBytes);

            // create the compressor
            apeCompress = IAPECompress.createIAPECompress();

            // figure the audio bytes
            int audioBytes = audioBlocks.value * waveFormatEx.blockAlign;

            // start the encoder
            if (headerBytes.value > 0) buffer = new byte[headerBytes.value];
            inputSource.getHeaderData(buffer);
            apeCompress.start(outputFilename, waveFormatEx, audioBytes,
                    compressionLevel, buffer, headerBytes.value);

            // set-up the progress
            ProgressHelper macProgressHelper = new ProgressHelper(audioBytes, progressor);

            // master loop
            int bytesLeft = audioBytes;

            macProgressHelper.updateStatus("Process data by compressor");

            while (bytesLeft > 0) {
                int bytesAdded = apeCompress.addDataFromInputSource(inputSource, bytesLeft);

                bytesLeft -= bytesAdded;

                // update the progress
                macProgressHelper.updateProgress(audioBytes - bytesLeft);

                // process the kill flag
                if (macProgressHelper.isKillFlag())
                    throw new JMACStoppedByUserException();
            }

            macProgressHelper.updateStatus("Finishing compression");

            // finalize the file
            if (terminatingBytes.value > 0) buffer = new byte[terminatingBytes.value];
            inputSource.getTerminatingData(buffer);
            apeCompress.finish(buffer, terminatingBytes.value, terminatingBytes.value);

            // update the progress to 100%
            macProgressHelper.updateStatus("Compression finished");
        } finally {
            // kill the compressor if we failed
            if (apeCompress != null)
                apeCompress.kill();
            if (inputSource != null)
                inputSource.close();
        }
    }
}
