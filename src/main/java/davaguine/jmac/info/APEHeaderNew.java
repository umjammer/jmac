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

import davaguine.jmac.tools.ByteArrayReader;
import davaguine.jmac.tools.ByteArrayWriter;
import davaguine.jmac.tools.File;
import davaguine.jmac.tools.JMACException;


/**
 * @author Dmitry Vaguine
 * @version 04.03.2004 14:51:31
 */
public class APEHeaderNew {

    /** the compression level (unsigned short) */
    public int compressionLevel;
    /** any format flags (for future use) (unsigned short) */
    public int formatFlags;

    /** the number of audio blocks in one frame (unsigned int) */
    public long blocksPerFrame;
    /** the number of audio blocks in the final frame (unsigned int) */
    public long finalFrameBlocks;
    /** the total number of frames (unsigned int) */
    public long totalFrames;

    /** the bits per sample (typically 16) (unsigned short) */
    public int bitsPerSample;
    /** the number of channels (1 or 2) (unsigned short) */
    public int channels;
    /** the sample rate (typically 44100) (unsigned int) */
    public long sampleRate;

    public final static int APE_HEADER_BYTES = 24;

    public static APEHeaderNew read(File file) throws IOException {
        try {
            APEHeaderNew header = new APEHeaderNew();
            ByteArrayReader reader = new ByteArrayReader(file, APE_HEADER_BYTES);
            header.compressionLevel = reader.readUnsignedShort();
            header.formatFlags = reader.readUnsignedShort();
            header.blocksPerFrame = reader.readUnsignedInt();
            header.finalFrameBlocks = reader.readUnsignedInt();
            header.totalFrames = reader.readUnsignedInt();
            header.bitsPerSample = reader.readUnsignedShort();
            header.channels = reader.readUnsignedShort();
            header.sampleRate = reader.readUnsignedInt();
            return header;
        } catch (EOFException e) {
            throw new JMACException("Unsupported Format");
        }
    }

    public void write(ByteArrayWriter writer) {
        writer.writeUnsignedShort(compressionLevel);
        writer.writeUnsignedShort(formatFlags);
        writer.writeUnsignedInt(blocksPerFrame);
        writer.writeUnsignedInt(finalFrameBlocks);
        writer.writeUnsignedInt(totalFrames);
        writer.writeUnsignedShort(bitsPerSample);
        writer.writeUnsignedShort(channels);
        writer.writeUnsignedInt(sampleRate);
    }
}
