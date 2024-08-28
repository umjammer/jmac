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

import davaguine.jmac.info.InputSource;
import davaguine.jmac.info.WaveFormat;
import davaguine.jmac.tools.ByteArrayReader;
import davaguine.jmac.tools.ByteBuffer;
import davaguine.jmac.tools.File;
import davaguine.jmac.tools.IntegerPointer;
import davaguine.jmac.tools.JMACException;


/**
 * @author Dmitry Vaguine
 * @version 08.05.2004 11:17:57
 */
public class APECompress extends IAPECompress {

    public APECompress() {
        bufferHead = 0;
        bufferTail = 0;
        bufferSize = 0;
        bufferLocked = false;
        ownsOutputIO = false;
        output = null;

        apeCompressCreate = new APECompressCreate();

        buffer = null;
    }

    @Override
    protected void finalize() {
        kill();
    }

    // start encoding

    @Override
    public void start(String outputFilename, WaveFormat wfeInput, int maxAudioBytes, int compressionLevel, byte[] headerData, int headerBytes) throws IOException {
        output = File.createFile(outputFilename, "rw");
        ownsOutputIO = true;

        apeCompressCreate.start(output, wfeInput, maxAudioBytes, compressionLevel,
                headerData, headerBytes);

        bufferSize = apeCompressCreate.getFullFrameBytes();
        buffer = new byte[bufferSize];
        this.wfeInput = wfeInput;
    }

    @Override
    public void startEx(File output, WaveFormat wfeInput, int maxAudioBytes, int compressionLevel, byte[] headerData, int headerBytes) throws IOException {
        this.output = output;
        ownsOutputIO = false;

        apeCompressCreate.start(this.output, wfeInput, maxAudioBytes, compressionLevel,
                headerData, headerBytes);

        bufferSize = apeCompressCreate.getFullFrameBytes();
        buffer = new byte[bufferSize];
        this.wfeInput = wfeInput;
    }

    // add data / compress data

    /** allows linear, immediate access to the buffer (fast) */
    @Override
    public int getBufferBytesAvailable() {
        return bufferSize - bufferTail;
    }

    @Override
    public void unlockBuffer(int bytesAdded, boolean process) throws IOException {
        if (!bufferLocked)
            throw new JMACException("Error Undefined");

        bufferTail += bytesAdded;
        bufferLocked = false;

        if (process)
            processBuffer();
    }

    private final ByteBuffer bufferPointer = new ByteBuffer();

    @Override
    public ByteBuffer lockBuffer(IntegerPointer bytesAvailable) {
        if (buffer == null) {
            return null;
        }

        if (bufferLocked)
            return null;

        bufferLocked = true;

        if (bytesAvailable != null)
            bytesAvailable.value = getBufferBytesAvailable();

        bufferPointer.reset(buffer, bufferTail);
        return bufferPointer;
    }

    /** slower, but easier than locking and unlocking (copies data) */
    private final IntegerPointer addDataBytesAvailable = new IntegerPointer();

    @Override
    public void addData(byte[] data, int bytes) throws IOException {
        int bytesDone = 0;

        while (bytesDone < bytes) {
            // lock the buffer
            addDataBytesAvailable.value = 0;
            ByteBuffer buffer = lockBuffer(addDataBytesAvailable);
            if (buffer == null || addDataBytesAvailable.value <= 0)
                throw new JMACException("Error Undefined");

            // calculate how many bytes to copy and add that much to the buffer
            int bytesToProcess = Math.min(addDataBytesAvailable.value, bytes - bytesDone);
            buffer.append(data, bytesDone, bytesToProcess);

            // unlock the buffer (fail if not successful)
            unlockBuffer(bytesToProcess);

            // update our progress
            bytesDone += bytesToProcess;
        }
    }

    // use a CIO (input source) to add data
    private final IntegerPointer addDataFromInputSourceBytesAvailavle = new IntegerPointer();

    @Override
    public int addDataFromInputSource(InputSource inputSource, int maxBytes) throws IOException {
        // error check the parameters
        if (inputSource == null)
            throw new JMACException("Bad Parameters");

        // initialize
        int bytesAdded = 0;
        int bytesRead = 0;

        // lock the buffer
        addDataFromInputSourceBytesAvailavle.value = 0;
        ByteBuffer buffer = lockBuffer(addDataFromInputSourceBytesAvailavle);

        // calculate the 'ideal' number of bytes
        int idealBytes = apeCompressCreate.getFullFrameBytes() - (bufferTail - bufferHead);
        if (idealBytes > 0) {
            // get the data
            int bytesToAdd = addDataFromInputSourceBytesAvailavle.value;

            if (maxBytes > 0) {
                if (bytesToAdd > maxBytes) bytesToAdd = maxBytes;
            }

            if (bytesToAdd > idealBytes) bytesToAdd = idealBytes;

            // always make requests along block boundaries
            while ((bytesToAdd % wfeInput.blockAlign) != 0)
                bytesToAdd--;

            int blocksToAdd = bytesToAdd / wfeInput.blockAlign;

            // get data
            int blocksAdded = inputSource.getData(buffer, blocksToAdd);
            bytesRead = (blocksAdded * wfeInput.blockAlign);

            // store the bytes read
            bytesAdded = bytesRead;
        }

        // unlock the data and process
        unlockBuffer(bytesRead, true);

        return bytesAdded;
    }

    // finish / kill
    @Override
    public void finish(byte[] terminatingData, int terminatingBytes, int wavTerminatingBytes) throws IOException {
        processBuffer(true);
        apeCompressCreate.finish(terminatingData, terminatingBytes, wavTerminatingBytes);
    }

    @Override
    public void kill() {
        if (output != null) {
            try {
                if (ownsOutputIO)
                    output.close();
            } catch (IOException e) {
                throw new JMACException("Error while closing output stream", e);
            }
        }
        output = null;
    }

    private void processBuffer() throws IOException {
        processBuffer(false);
    }

    private final ByteArrayReader byteReader = new ByteArrayReader();

    private void processBuffer(boolean finalize) throws IOException {
        if (buffer == null)
            throw new JMACException("Error Undefined");

        // process as much as possible
        int threshold = (finalize) ? 0 : apeCompressCreate.getFullFrameBytes();

        while ((bufferTail - bufferHead) >= threshold) {
            int frameBytes = Math.min(apeCompressCreate.getFullFrameBytes(), bufferTail - bufferHead);

            if (frameBytes == 0)
                break;

            byteReader.reset(buffer, bufferHead);
            apeCompressCreate.encodeFrame(byteReader, frameBytes);

            bufferHead += frameBytes;
        }

        // shift the buffer
        if (bufferHead != 0) {
            int bytesLeft = bufferTail - bufferHead;

            if (bytesLeft != 0)
                System.arraycopy(buffer, bufferHead, buffer, 0, bytesLeft);

            bufferTail -= bufferHead;
            bufferHead = 0;
        }
    }

    private final APECompressCreate apeCompressCreate;

    private int bufferHead;
    private int bufferTail;
    private int bufferSize;
    private byte[] buffer;
    private boolean bufferLocked;

    private File output;
    private boolean ownsOutputIO;
    private WaveFormat wfeInput;
}
