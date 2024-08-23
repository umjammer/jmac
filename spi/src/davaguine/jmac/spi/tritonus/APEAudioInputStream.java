/*
 *  21.04.2004 Original verion. davagin@udm.ru.
 *-----------------------------------------------------------------------
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
 *----------------------------------------------------------------------
 */

package davaguine.jmac.spi.tritonus;

import davaguine.jmac.decoder.IAPEDecompress;
import davaguine.jmac.tools.File;
import davaguine.jmac.tools.InputStreamFile;
import org.tritonus.share.TDebug;
import org.tritonus.share.TCircularBuffer;
import org.tritonus.share.sampled.convert.TAsynchronousFilteredAudioInputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Author: Dmitry Vaguine
 * Date: 12.03.2004
 * Time: 13:35:13
 */

/**
 * Decoded APE audio input stream.
 */
public class APEAudioInputStream extends TAsynchronousFilteredAudioInputStream {
    private final static int BLOCKS_PER_DECODE = 9216;
    protected File file = null;
    protected IAPEDecompress m_decoder = null;
    protected byte[] buffer = null;
    protected byte[] skipbuffer = null;
    protected int nBlocksLeft;
    protected int blockAlign;
    protected int pos = 0;
    protected int size = 0;

    private HashMap properties = null;
    protected int currentBitrate;
    protected int currentBlock;
    protected int currentMs;

    /**
     * Constructs an audio input stream that has the requested format, using audio data
     * from the specified input stream.
     *
     * @param format - the format of this stream's audio data
     * @param stream - the stream on which this APEAudioInputStream object is based
     */
    public APEAudioInputStream(AudioFormat format, InputStream stream) {
        super(format, AudioSystem.NOT_SPECIFIED);
        try {
            file = new InputStreamFile(stream);
            m_decoder = IAPEDecompress.CreateIAPEDecompress(file);

            nBlocksLeft = m_decoder.getApeInfoDecompressTotalBlocks();
            blockAlign = m_decoder.getApeInfoBlockAlign();

            currentBitrate = m_decoder.getApeInfoAverageBitrate();
            currentBlock = 0;
            currentMs = 0;

            // allocate space for decompression
            buffer = new byte[blockAlign * BLOCKS_PER_DECODE];
            buffer = new byte[blockAlign * BLOCKS_PER_DECODE];

            properties = new HashMap();
        } catch (IOException e) {
            if (TDebug.TraceAudioInputStream) {
                TDebug.out(e);
            }
        }
    }

    /**
     * Returns dynamic properties of input stream.
     * <p/>
     * <ul>
     * <li><b>ape.block</b> [Integer], current block position.
     * <li><b>ape.bitrate</b> [Integer], current bitrate.
     * <li><b>ape.position.microseconds</b> [Integer], elapsed microseconds.
     * </ul>
     *
     * @return dynamic properties of input stream.
     */
    public Map properties() {
        properties.put("ape.block", new Integer(currentBlock));
        properties.put("ape.bitrate", new Integer(currentBitrate));
        properties.put("ape.position.microseconds", new Integer(currentMs));
        return properties;
    }

    /**
     * Execute method of input stream
     */
    public void execute() {
        if (TDebug.TraceAudioInputStream) TDebug.out("execute() : begin");
        try {
            if (m_decoder == null)
                throw new IOException("Stream closed");
            pos = 0;
            if (nBlocksLeft > 0) {
                int nBlocksDecoded = m_decoder.GetData(buffer, BLOCKS_PER_DECODE);

                nBlocksLeft -= nBlocksDecoded;
                size = nBlocksDecoded * blockAlign;
                currentBitrate = m_decoder.getApeInfoDecompressCurrentBitRate();
                currentBlock = m_decoder.getApeInfoDecompressCurrentBlock();
                currentMs = m_decoder.getApeInfoDecompressCurrentMS();
                if (TDebug.TraceAudioInputStream) {
                    TDebug.out("ape.block: " + currentBlock);
                    TDebug.out("ape.bitrate: " + currentBitrate);
                    TDebug.out("ape.position.microseconds: " + currentMs);
                }
                getCircularBuffer().write(buffer, 0, size);
            } else {
                size = 0;
                getCircularBuffer().close();
            }
        } catch (IOException e) {
            if (TDebug.TraceAudioInputStream) {
                TDebug.out(e);
            }
        }
        if (TDebug.TraceAudioInputStream) TDebug.out("execute() : end");
    }

    /**
     * Closes the input stream.
     *
     * @throws IOException - in case of IO error occured.
     */
    public void close() throws IOException {
        if (m_decoder == null)
            return;
        file.close();
        file = null;
        m_decoder = null;
        buffer = null;
        properties = null;
    }

}
