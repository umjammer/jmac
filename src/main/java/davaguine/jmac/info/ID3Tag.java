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


/**
 * @author Dmitry Vaguine
 * @version 04.03.2004 14:51:31
 */
public class ID3Tag {

    /** should equal 'TAG' */
    public String header;
    /** title */
    public String title;
    /** artist */
    public String artist;
    /** album */
    public String album;
    /** year */
    public String year;
    /** comment */
    public String comment;
    /** track */
    public short track;
    /** genre */
    public short genre;

    public final static int ID3_TAG_BYTES = 128;

    public static ID3Tag read(File file) throws IOException {
        file.seek(file.length() - ID3_TAG_BYTES);
        try {
            ID3Tag tag = new ID3Tag();
            ByteArrayReader reader = new ByteArrayReader(file, ID3_TAG_BYTES);
            tag.header = reader.readString(3, "US-ASCII");
            tag.title = reader.readString(30, "US-ASCII");
            tag.artist = reader.readString(30, "US-ASCII");
            tag.album = reader.readString(30, "US-ASCII");
            tag.year = reader.readString(4, "US-ASCII");
            tag.comment = reader.readString(29, "US-ASCII");
            tag.track = reader.readUnsignedByte();
            tag.genre = reader.readUnsignedByte();
            return tag.header.equals("TAG") ? tag : null;
        } catch (EOFException e) {
            return null;
        }
    }

    public final void write(ByteArrayWriter writer) {
        writer.writeString(header, 3, "US-ASCII");
        writer.writeString(title, 30, "US-ASCII");
        writer.writeString(artist, 30, "US-ASCII");
        writer.writeString(album, 30, "US-ASCII");
        writer.writeString(year, 4, "US-ASCII");
        writer.writeString(comment, 29, "US-ASCII");
        writer.writeUnsignedByte(track);
        writer.writeUnsignedByte(genre);
    }
}
