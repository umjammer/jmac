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

package davaguine.jmac.spi;

import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import davaguine.jmac.decoder.IAPEDecompress;
import davaguine.jmac.tools.File;
import davaguine.jmac.tools.InputStreamFile;

import static java.lang.System.getLogger;


/**
 * Decoded APE audio input stream.
 *
 * @author Dmitry Vaguine
 * @version 12.03.2004 13:35:13
 */
public class APEAudioInputStream extends AudioInputStream {

    private static final Logger logger = getLogger(APEAudioInputStream.class.getName());

    private final static int BLOCKS_PER_DECODE = 9216;
    private IAPEDecompress decoder = null;
    private File file = null;
    private byte[] buffer = null;
    private int blocksLeft;
    private int blockAlign;
    private int pos = 0;
    private int size = 0;

    /**
     * Constructs an audio input stream that has the requested format, using audio data
     * from the specified input stream.
     *
     * @param format the format of this stream's audio data
     * @param stream the stream on which this APEAudioInputStream object is based
     */
    public APEAudioInputStream(AudioFormat format, InputStream stream) {
        super(stream, format, AudioSystem.NOT_SPECIFIED);
        try {
            file = new InputStreamFile(stream);
            decoder = IAPEDecompress.createAPEDecompress(file);

            blocksLeft = decoder.getApeInfoDecompressTotalBlocks();
            blockAlign = decoder.getApeInfoBlockAlign();

            // allocate space for decompression
            buffer = new byte[blockAlign * BLOCKS_PER_DECODE];
        } catch (IOException e) {
            logger.log(Level.ERROR, e.getMessage(), e);
        }
    }

    private void ensureOpen() throws IOException {
        if (decoder == null)
            throw new IOException("Stream closed");
    }

    private void fill() throws IOException {
        pos = 0;
        if (blocksLeft > 0) {
            int blocksDecoded = decoder.getData(buffer, BLOCKS_PER_DECODE);

            blocksLeft -= blocksDecoded;
            size = blocksDecoded * blockAlign;
        } else
            size = 0;
    }

    /**
     * Reads the next byte of data from the audio input stream.
     *
     * @return the next byte of data, or -1 if the end of the stream is reached
     * @throws IOException - if an input or output error occurs
     */
    @Override
    public synchronized int read() throws IOException {
        ensureOpen();
        if (pos < size)
            return buffer[pos++] & 0xff;
        else {
            fill();
            return pos < size ? (buffer[pos++] & 0xff) : -1;
        }
    }

    /**
     * Reads up to a specified maximum number of bytes of data from the audio stream, putting
     * them into the given byte array.
     *
     * @param b   the buffer into which the data is read
     * @param off the offset, from the beginning of array b, at which the data will be written
     * @param len the maximum number of bytes to read
     * @return the total number of bytes read into the buffer, or -1 if there is no more data because the end of the stream has been reached
     * @throws IOException if an input or output error occurs
     */
    @Override
    public synchronized int read(byte[] b, int off, int len) throws IOException {
        ensureOpen();
        if (b == null) {
            throw new NullPointerException();
        } else if ((off < 0) || (off > b.length) || (len < 0) ||
                ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        }
        if (pos >= size)
            fill();
        if (pos >= size)
            return -1;
        if (pos + len < size) {
            System.arraycopy(buffer, pos, b, off, len);
            pos += len;
            return len;
        }
        int avail = size - pos;
        System.arraycopy(buffer, pos, b, off, avail);
        fill();
        if (pos >= size)
            return avail;
        if (pos + len - avail < size) {
            System.arraycopy(buffer, pos, b, off + avail, len - avail);
            pos += (len - avail);
            return len;
        }
        len = size - pos;
        System.arraycopy(buffer, pos, b, off + avail, len);
        pos += len;
        return len + avail;
    }

    /**
     * Skips over and discards a specified number of bytes from this audio input stream.
     *
     * @param n the requested number of bytes to be skipped
     * @return the actual number of bytes skipped
     * @throws IOException if an input or output error occurs
     */
    @Override
    public synchronized long skip(long n) throws IOException {
        ensureOpen();
        if (n <= 0) {
            return 0;
        }
        if (pos >= size)
            fill();
        if (pos >= size)
            return 0;
        if (pos + n < size) {
            pos += (int) n;
            return n;
        }
        int avail = size - pos;
        fill();
        if (pos >= size)
            return avail;
        if (pos + n - avail < size) {
            pos += (int) (n - avail);
            return n;
        }
        n = size - pos;
        pos += (int) n;
        return n + avail;
    }

    /**
     * Returns the maximum number of bytes that can be read (or skipped over) from this
     * audio input stream without blocking. This limit applies only to the next invocation
     * of a read or skip method for this audio input stream; the limit can vary each time
     * these methods are invoked. Depending on the underlying stream,an IOException may be
     * thrown if this stream is closed.
     *
     * @return the number of bytes that can be read from this audio input stream without blocking
     * @throws IOException if an input or output error occurs
     */
    @Override
    public int available() throws IOException {
        ensureOpen();
        return size - pos;
    }

    /**
     * Tests whether this audio input stream supports the mark and reset methods.
     * This method always returns false.
     *
     * @return returns false.
     */
    @Override
    public boolean markSupported() {
        return false;
    }

    /**
     * Marks the current position in this audio input stream. This method does nothing.
     *
     * @param readlimit the maximum number of bytes that can be read before the mark position becomes invalid.
     */
    @Override
    public void mark(int readlimit) {
    }

    /**
     * Repositions this audio input stream to the position it had at the time its mark method was last invoked.
     * This method always throws IOException since this stream doesn't support mark feature.
     *
     * @throws IOException if an input or output error occurs.
     */
    @Override
    public void reset() throws IOException {
        throw new IOException("mark not supported");
    }

    /**
     * Closes this audio input stream and releases any system resources associated with the stream.
     *
     * @throws IOException if an input or output error occurs
     */
    @Override
    public void close() throws IOException {
        if (file == null)
            return;
        file.close();
        file = null;
        decoder = null;
        buffer = null;
    }
}
