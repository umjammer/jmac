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

import java.nio.charset.StandardCharsets;

import davaguine.jmac.tools.ByteArrayWriter;


/**
 * @author Dmitry Vaguine
 * @version 04.03.2004 14:51:31
 */
public class APETagField {

    public final static int TAG_FIELD_FLAG_READ_ONLY = 1 << 0;

    public final static int TAG_FIELD_FLAG_DATA_TYPE_MASK = 6;
    public final static int TAG_FIELD_FLAG_DATA_TYPE_TEXT_UTF8 = 0 << 1;
    public final static int TAG_FIELD_FLAG_DATA_TYPE_BINARY = 1 << 1;
    public final static int TAG_FIELD_FLAG_DATA_TYPE_EXTERNAL_INFO = 2 << 1;
    public final static int TAG_FIELD_FLAG_DATA_TYPE_RESERVED = 3 << 1;

    /** create a tag field (use fieldBytes = -1 for null-terminated strings) */
    public APETagField(String fieldName, byte[] fieldValue) {
        this(fieldName, fieldValue, 0);
    }

    public APETagField(String fieldName, byte[] fieldValue, int flags) {
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
        fieldFlags = flags;

        // data (we'll always allocate two extra bytes and memset to 0 so we're safely NULL terminated)
        this.fieldValue = new byte[fieldValue.length];
        System.arraycopy(fieldValue, 0, this.fieldValue, 0, fieldValue.length);

        // flags
        fieldFlags = flags;
    }

    public int getFieldSize() {
        return fieldName.getBytes(StandardCharsets.US_ASCII).length + 1 + fieldValue.length + 4 + 4;
    }

    /** get the name of the field */
    public String getFieldName() {
        return fieldName;
    }

    /** get the value of the field */
    public byte[] getFieldValue() {
        return fieldValue;
    }

    public int getFieldValueSize() {
        return fieldValue.length;
    }

    /** get any special flags */
    public int getFieldFlags() {
        return fieldFlags;
    }

    /** output the entire field to a buffer (GetFieldSize() bytes) */
    public int saveField(ByteArrayWriter writer) {
        writer.writeInt(fieldValue.length);
        writer.writeInt(fieldFlags);
        writer.writeZString(fieldName, "US-ASCII");
        writer.writeBytes(fieldValue);

        return getFieldSize();
    }

    /** checks to see if the field is read-only */
    public boolean isReadOnly() {
        return (fieldFlags & TAG_FIELD_FLAG_READ_ONLY) > 0;
    }

    public boolean getIsUTF8Text() {
        return ((fieldFlags & TAG_FIELD_FLAG_DATA_TYPE_MASK) == TAG_FIELD_FLAG_DATA_TYPE_TEXT_UTF8);
    }

    /** set helpers (use with EXTREME caution) */
    void setFieldFlags(int flags) {
        fieldFlags = flags;
    }

    private final String fieldName;
    private byte[] fieldValue;
    private int fieldFlags;
}
