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
import davaguine.jmac.tools.ByteArrayWriter;
import davaguine.jmac.tools.File;
import davaguine.jmac.tools.JMACException;


/**
 * @author Dmitry Vaguine
 * @version 04.03.2004 14:51:31
 */
public class APETagFooter {

    /** should equal 'APETAGEX' (char[8]) */
    public String id;
    /** equals CURRENT_APE_TAG_VERSION (int) */
    public int version;
    /** the complete size of the tag, including this footer (int) */
    public int size;
    /** the number of fields in the tag (int) */
    public int fields;
    /** the tag flags (none currently defined) (int) */
    public int flags;

    public final static int APE_TAG_FOOTER_BYTES = 32;

    public final static int CURRENT_APE_TAG_VERSION = 2000;

    APETagFooter() {
        this(0, 0);
    }

    APETagFooter(int fields) {
        this(fields, 0);
    }

    APETagFooter(int fields, int fieldBytes) {
        id = "APETAGEX";
        this.fields = fields;
        flags = APETag.APE_TAG_FLAGS_DEFAULT;
        size = fieldBytes + APE_TAG_FOOTER_BYTES;
        version = CURRENT_APE_TAG_VERSION;
    }

    public int getTotalTagBytes() {
        return size + (hasHeader() ? APE_TAG_FOOTER_BYTES : 0);
    }

    public int getFieldBytes() {
        return size - APE_TAG_FOOTER_BYTES;
    }

    public int getFieldsOffset() {
        return hasHeader() ? APE_TAG_FOOTER_BYTES : 0;
    }

    public int getNumberFields() {
        return fields;
    }

    public boolean hasHeader() {
        return (flags & APETag.APE_TAG_FLAG_CONTAINS_HEADER) > 0;
    }

    public boolean isHeader() {
        return (flags & APETag.APE_TAG_FLAG_IS_HEADER) > 0;
    }

    public int getVersion() {
        return version;
    }

    public boolean isValid(boolean allowHeader) {
        boolean valid = id.equals("APETAGEX") &&
                (version <= CURRENT_APE_TAG_VERSION) &&
                (fields <= 65536) &&
                (getFieldBytes() <= (1024 * 1024 * 16));

        if (valid && !allowHeader && isHeader())
            valid = false;

        return valid;
    }


    public static APETagFooter read(File file) throws IOException {
        file.seek(file.length() - APE_TAG_FOOTER_BYTES);
        APETagFooter tag = new APETagFooter();
        try {
            ByteArrayReader reader = new ByteArrayReader(file, APE_TAG_FOOTER_BYTES);
            tag.id = reader.readString(8, StandardCharsets.US_ASCII.name());
            tag.version = reader.readInt();
            tag.size = reader.readInt();
            tag.fields = reader.readInt();
            tag.flags = reader.readInt();
            return tag;
        } catch (EOFException e) {
            throw new JMACException("Unsupported Format");
        }
    }

    public void write(ByteArrayWriter writer) {
        writer.writeString(id, 8, StandardCharsets.US_ASCII.name());
        writer.writeInt(version);
        writer.writeInt(size);
        writer.writeInt(fields);
        writer.writeInt(flags);
        writer.writeInt(0);
        writer.writeInt(0);
    }
}
