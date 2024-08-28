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
import java.nio.charset.StandardCharsets;

import davaguine.jmac.tools.ByteArrayReader;
import davaguine.jmac.tools.File;
import davaguine.jmac.tools.JMACException;


/**
 * @author Dmitry Vaguine
 * @version 04.03.2004 14:51:31
 */
public class APEHeaderOld {

    /** should equal 'MAC ' */
    public String id;
    /** version number * 1000 (3.81 = 3810) */
    public int version;
    /** the compression level */
    public int compressionLevel;
    /** any format flags (for future use) */
    public int formatFlags;
    /** the number of channels (1 or 2) */
    public int channels;
    /** the sample rate (typically 44100) */
    public long sampleRate;
    /** the bytes after the MAC header that compose the WAV header */
    public long headerBytes;
    /** the bytes after that raw data (for extended info) */
    public long terminatingBytes;
    /** the number of frames in the file */
    public long totalFrames;
    /** the number of samples in the final frame */
    public long finalFrameBlocks;

    public final static int APE_HEADER_OLD_BYTES = 32;

    public static APEHeaderOld read(File file) throws IOException {
        try {
            APEHeaderOld header = new APEHeaderOld();
            ByteArrayReader reader = new ByteArrayReader(file, APE_HEADER_OLD_BYTES);
            header.id = reader.readString(4, StandardCharsets.US_ASCII.name());
            header.version = reader.readUnsignedShort();
            header.compressionLevel = reader.readUnsignedShort();
            header.formatFlags = reader.readUnsignedShort();
            header.channels = reader.readUnsignedShort();
            header.sampleRate = reader.readUnsignedInt();
            header.headerBytes = reader.readUnsignedInt();
            header.terminatingBytes = reader.readUnsignedInt();
            header.totalFrames = reader.readUnsignedInt();
            header.finalFrameBlocks = reader.readUnsignedInt();
            return header;
        } catch (EOFException e) {
            throw new JMACException("Unsupported Format");
        }
    }
}
