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

import java.io.IOException;

import davaguine.jmac.tools.ByteBuffer;
import davaguine.jmac.tools.File;
import davaguine.jmac.tools.IntegerPointer;
import davaguine.jmac.tools.JMACException;


/**
 * @author Dmitry Vaguine
 * @version 07.05.2004 14:10:50
 */
public class WAVInputSource extends InputSource {

    // construction / destruction

    public WAVInputSource(File io, WaveFormat wfeSource, IntegerPointer totalBlocks, IntegerPointer headerBytes, IntegerPointer terminatingBytes) throws IOException {
        super(io, wfeSource, totalBlocks, headerBytes, terminatingBytes);
        isValid = false;

        if (io == null || wfeSource == null)
            throw new JMACException("Bad Parameters");

        this.io = io;
        ownsInputIO = false;

        analyzeSource();

        // fill in the parameters
        wfeSource = this.wfeSource;
        if (totalBlocks != null) totalBlocks.value = dataBytes / this.wfeSource.blockAlign;
        if (headerBytes != null) headerBytes.value = this.headerBytes;
        if (terminatingBytes != null) terminatingBytes.value = this.terminatingBytes;

        isValid = true;
    }

    public WAVInputSource(String sourceName, WaveFormat wfeSource, IntegerPointer totalBlocks, IntegerPointer headerBytes, IntegerPointer terminatingBytes) throws IOException {
        super(sourceName, wfeSource, totalBlocks, headerBytes, terminatingBytes);
        isValid = false;

        if (sourceName == null || wfeSource == null)
            throw new JMACException("Bad Parameters");

        io = File.createFile(sourceName, "r");
        ownsInputIO = true;

        analyzeSource();
        // fill in the parameters
        this.wfeSource.formatTag = wfeSource.formatTag;
        this.wfeSource.channels = wfeSource.channels;
        this.wfeSource.samplesPerSec = wfeSource.samplesPerSec;
        this.wfeSource.avgBytesPerSec = wfeSource.avgBytesPerSec;
        this.wfeSource.blockAlign = wfeSource.blockAlign;
        this.wfeSource.bitsPerSample = wfeSource.bitsPerSample;
        if (totalBlocks != null) totalBlocks.value = dataBytes / wfeSource.blockAlign;
        if (headerBytes != null) headerBytes.value = this.headerBytes;
        if (terminatingBytes != null) terminatingBytes.value = this.terminatingBytes;

        isValid = true;
    }

    @Override
    public void close() throws IOException {
        if (isValid && ownsInputIO && io != null)
            io.close();
        io = null;
    }

    @Override
    protected void finalize() {
        try {
            close();
        } catch (IOException e) {
            throw new JMACException("Error while closing input stream.");
        }
    }

    // get data

    @Override
    public int getData(ByteBuffer buffer, int blocks) throws IOException {
        if (!isValid)
            throw new JMACException("Undefined Error");

        int bytes = (wfeSource.blockAlign * blocks);

        int bytesRead = io.read(buffer.getBytes(), buffer.getIndex(), bytes);

        return bytesRead / wfeSource.blockAlign;
    }

    // get header / terminating data

    @Override
    public void getHeaderData(byte[] buffer) throws IOException {
        if (!isValid)
            throw new JMACException("Undefined Error");

        if (headerBytes > 0) {
            long originalFileLocation = io.getFilePointer();

            io.seek(0);

            if (io.read(buffer, 0, headerBytes) != headerBytes)
                throw new JMACException("Undefined Error");

            io.seek(originalFileLocation);
        }
    }

    @Override
    public void getTerminatingData(byte[] buffer) throws IOException {
        if (!isValid)
            throw new JMACException("Undefined Error");

        if (terminatingBytes > 0) {
            long originalFileLocation = io.getFilePointer();

            io.seek(io.length() - terminatingBytes);

            if (io.read(buffer, 0, terminatingBytes) != terminatingBytes)
                throw new JMACException("Undefined Error");

            io.seek(originalFileLocation);
        }
    }

    private void analyzeSource() throws IOException {
        // seek to the beginning (just in case)
        io.seek(0);

        // get the file size
        fileBytes = (int) io.length();

        // get the RIFF header
        int riffSignature = io.readInt();
        int goalSignature = ('R' << 24) | ('I' << 16) | ('F' << 8) | ('F');
        if (riffSignature != goalSignature)
            throw new JMACException("Invalid Input File");

        io.readInt();

        // read the data type header
        int dataTypeSignature = io.readInt();
        goalSignature = ('W' << 24) | ('A' << 16) | ('V' << 8) | ('E');
        // make sure it's the right data type
        if (dataTypeSignature != goalSignature)
            throw new JMACException("Invalid Input File");

        // find the 'fmt ' chunk
        RiffChunkHeader riffChunkHeader = new RiffChunkHeader();
        riffChunkHeader.read(io);
        goalSignature = (' ' << 24) | ('t' << 16) | ('m' << 8) | ('f');
        while (riffChunkHeader.chunkLabel != goalSignature) {
            // move the file pointer to the end of this chunk
            io.seek(io.getFilePointer() + riffChunkHeader.chunkBytes);

            // check again for the data chunk
            riffChunkHeader.read(io);
        }

        // read the format info
        WaveFormat wavFormatHeader = new WaveFormat();
        wavFormatHeader.readHeader(io);

        // error check the header to see if we support it
        if (wavFormatHeader.formatTag != 1)
            throw new JMACException("Invalid Input File");

        // copy the format information to the WAVEFORMATEX passed in
        WaveFormat.fillWaveFormatEx(wfeSource, wavFormatHeader.samplesPerSec, wavFormatHeader.bitsPerSample, wavFormatHeader.channels);

        // skip over any extra data in the header
        int wavFormatHeaderExtra = (int) (riffChunkHeader.chunkBytes - WaveFormat.WAV_HEADER_SIZE);
        if (wavFormatHeaderExtra < 0)
            throw new JMACException("Invalid Input File");
        else
            io.seek(io.getFilePointer() + wavFormatHeaderExtra);

        // find the data chunk
        riffChunkHeader.read(io);
        goalSignature = ('a' << 24) | ('t' << 16) | ('a' << 8) | ('d');

        while (riffChunkHeader.chunkLabel != goalSignature) {
            // move the file pointer to the end of this chunk
            io.seek(io.getFilePointer() + riffChunkHeader.chunkBytes);

            // check again for the data chunk
            riffChunkHeader.read(io);
        }

        // we're at the data block
        headerBytes = (int) io.getFilePointer();
        dataBytes = (int) riffChunkHeader.chunkBytes;
        if (dataBytes < 0)
            dataBytes = fileBytes - headerBytes;

        // make sure the data bytes is a whole number of blocks
        if ((dataBytes % wfeSource.blockAlign) != 0)
            throw new JMACException("Invalid Input File");

        // calculate the terminating byts
        terminatingBytes = fileBytes - dataBytes - headerBytes;
    }

    private File io;
    private final boolean ownsInputIO;

    private final WaveFormat wfeSource = new WaveFormat();
    private int headerBytes;
    private int dataBytes;
    private int terminatingBytes;
    private int fileBytes;
    private boolean isValid;
}
