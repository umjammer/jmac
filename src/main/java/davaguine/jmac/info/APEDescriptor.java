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
 * @version 07.04.2004 14:36:53
 */
public class APEDescriptor {

    /** should equal 'MAC ' (char[4]) */
    public String id;
    /** version number * 1000 (3.81 = 3810) (unsigned short) */
    public int version;

    /** the number of descriptor bytes (allows later expansion of this header) (unsigned int32) */
    public long descriptorBytes;
    /** the number of header APE_HEADER bytes (unsigned int32) */
    public long headerBytes;
    /** the number of bytes of the seek table (unsigned int32) */
    public long seekTableBytes;
    /** the number of header data bytes (from original file) (unsigned int32) */
    public long headerDataBytes;
    /** the number of bytes of APE frame data (unsigned int32) */
    public long apeFrameDataBytes;
    /** the high order number of APE frame data bytes (unsigned int32) */
    public long apeFrameDataBytesHigh;
    /** the terminating data of the file (not including tag data) (unsigned int32) */
    public long terminatingDataBytes;

    /** the MD5 hash of the file (see notes for usage... it's a littly tricky) (unsigned char[16]) */
    public byte[] fileMD5 = new byte[16];

    public final static int APE_DESCRIPTOR_BYTES = 52;

    public static APEDescriptor read(File file) throws IOException {
        try {
            APEDescriptor header = new APEDescriptor();
            ByteArrayReader reader = new ByteArrayReader(file, APE_DESCRIPTOR_BYTES - 16);
            header.id = reader.readString(4, "US-ASCII");
            header.version = reader.readUnsignedShort();
            reader.skipBytes(2);
            header.descriptorBytes = reader.readUnsignedInt();
            header.headerBytes = reader.readUnsignedInt();
            header.seekTableBytes = reader.readUnsignedInt();
            header.headerDataBytes = reader.readUnsignedInt();
            header.apeFrameDataBytes = reader.readUnsignedInt();
            header.apeFrameDataBytesHigh = reader.readUnsignedInt();
            header.terminatingDataBytes = reader.readUnsignedInt();
            file.readFully(header.fileMD5);
            return header;
        } catch (EOFException e) {
            throw new JMACException("Unsupported Format");
        }
    }

    public void write(ByteArrayWriter writer) {
        writer.writeString(id, 4, "US-ASCII");
        writer.writeUnsignedShort(version);
        writer.writeUnsignedShort(0);
        writer.writeUnsignedInt(descriptorBytes);
        writer.writeUnsignedInt(headerBytes);
        writer.writeUnsignedInt(seekTableBytes);
        writer.writeUnsignedInt(headerDataBytes);
        writer.writeUnsignedInt(apeFrameDataBytes);
        writer.writeUnsignedInt(apeFrameDataBytesHigh);
        writer.writeUnsignedInt(terminatingDataBytes);
        writer.writeBytes(fileMD5);
    }
}
