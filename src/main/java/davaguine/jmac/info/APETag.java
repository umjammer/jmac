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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import davaguine.jmac.tools.ByteArrayReader;
import davaguine.jmac.tools.ByteArrayWriter;
import davaguine.jmac.tools.File;
import davaguine.jmac.tools.JMACException;
import davaguine.jmac.tools.RandomAccessFile;


/**
 * @author Dmitry Vaguine
 * @version 04.03.2004 14:51:31
 */
public class APETag implements Comparator<APETagField> {

    public final static String APE_TAG_FIELD_TITLE = "Title";
    public final static String APE_TAG_FIELD_ARTIST = "Artist";
    public final static String APE_TAG_FIELD_ALBUM = "Album";
    public final static String APE_TAG_FIELD_COMMENT = "Comment";
    public final static String APE_TAG_FIELD_YEAR = "Year";
    public final static String APE_TAG_FIELD_TRACK = "Track";
    public final static String APE_TAG_FIELD_GENRE = "Genre";
    public final static String APE_TAG_FIELD_COVER_ART_FRONT = "Cover Art (front)";
    public final static String APE_TAG_FIELD_NOTES = "Notes";
    public final static String APE_TAG_FIELD_LYRICS = "Lyrics";
    public final static String APE_TAG_FIELD_COPYRIGHT = "Copyright";
    public final static String APE_TAG_FIELD_BUY_URL = "Buy URL";
    public final static String APE_TAG_FIELD_ARTIST_URL = "Artist URL";
    public final static String APE_TAG_FIELD_PUBLISHER_URL = "Publisher URL";
    public final static String APE_TAG_FIELD_FILE_URL = "File URL";
    public final static String APE_TAG_FIELD_COPYRIGHT_URL = "Copyright URL";
    public final static String APE_TAG_FIELD_MJ_METADATA = "Media Jukebox Metadata";
    public final static String APE_TAG_FIELD_TOOL_NAME = "Tool Name";
    public final static String APE_TAG_FIELD_TOOL_VERSION = "Tool Version";
    public final static String APE_TAG_FIELD_PEAK_LEVEL = "Peak Level";
    public final static String APE_TAG_FIELD_REPLAY_GAIN_RADIO = "Replay Gain (radio)";
    public final static String APE_TAG_FIELD_REPLAY_GAIN_ALBUM = "Replay Gain (album)";
    public final static String APE_TAG_FIELD_COMPOSER = "Composer";
    public final static String APE_TAG_FIELD_KEYWORDS = "Keywords";

    // Footer (and header) flags

    public final static int APE_TAG_FLAG_CONTAINS_HEADER = (1 << 31);
    public final static int APE_TAG_FLAG_CONTAINS_FOOTER = (1 << 30);
    public final static int APE_TAG_FLAG_IS_HEADER = (1 << 29);

    public final static int APE_TAG_FLAGS_DEFAULT = APE_TAG_FLAG_CONTAINS_FOOTER;

    public final static String APE_TAG_GENRE_UNDEFINED = "Undefined";

    /**
     * create an APE tag
     * be careful with multiple threads / file pointer movement if you don't analyze immediately
     */
    public APETag(File io) throws IOException {
        this(io, true);
    }

    /**
     * create an APE tag
     * be careful with multiple threads / file pointer movement if you don't analyze immediately
     *
     * @param analyze determines whether it will analyze immediately or on the first request
     */
    public APETag(File io, boolean analyze) throws IOException {
        this.io = io; // we don't own the IO source

        if (analyze)
            analyze();
    }

    public APETag(String filename) throws IOException {
        this(filename, true);
    }

    public APETag(String filename, boolean analyze) throws IOException {
        io = new RandomAccessFile(new java.io.File(filename), "r");

        if (analyze)
            analyze();
    }

    // save the tag to the I/O source (bUseOldID3 forces it to save as an ID3v1.1 tag instead of an APE tag)
    public void save() throws IOException {
        save(false);
    }

    public void save(boolean useOldID3) throws IOException {
        remove(false);

        if (fields.size() == 0)
            return;

        if (!useOldID3) {
            int z = 0;

            // calculate the size of the whole tag
            int fieldBytes = 0;
            for (z = 0; z < fields.size(); z++)
                fieldBytes += fields.get(z).getFieldSize();

            // sort the fields
            sortFields();

            // build the footer
            APETagFooter apeTagFooter = new APETagFooter(fields.size(), fieldBytes);

            // make a buffer for the tag
            int totalTagBytes = apeTagFooter.getTotalTagBytes();

            // save the fields
            ByteArrayWriter writer = new ByteArrayWriter(totalTagBytes);
            for (z = 0; z < fields.size(); z++)
                fields.get(z).saveField(writer);

            // add the footer to the buffer
            apeTagFooter.write(writer);

            // dump the tag to the I/O source
            writeBufferToEndOfIO(writer.getBytes());
        } else {
            // build the ID3 tag
            ID3Tag id3tag = new ID3Tag();
            createID3Tag(id3tag);
            ByteArrayWriter writer = new ByteArrayWriter(ID3Tag.ID3_TAG_BYTES);
            id3tag.write(writer);
            writeBufferToEndOfIO(writer.getBytes());
        }
    }

    /** removes any tags from the file (bUpdate determines whether is should re-analyze after removing the tag) */
    public void remove() throws IOException {
        remove(true);
    }

    public void remove(boolean update) throws IOException {
        // variables
        long originalPosition = io.getFilePointer();

        boolean id3Removed = true;
        boolean apeTagRemoved = true;

        while (id3Removed || apeTagRemoved) {
            id3Removed = false;
            apeTagRemoved = false;

            // ID3 tag
            ID3Tag id3tag = ID3Tag.read(io);
            if (id3tag != null) {
                io.setLength(io.length() - ID3Tag.ID3_TAG_BYTES);
                id3Removed = true;
            }

            // APE Tag
            APETagFooter footer = APETagFooter.read(io);
            if (footer.isValid(true)) {
                io.setLength(io.length() - footer.getTotalTagBytes());
                apeTagRemoved = true;
            }
        }

        io.seek(originalPosition);

        if (update)
            analyze();
    }

    public void setFieldString(String fieldName, String fieldValue) throws IOException {
        // remove if empty
        if ((fieldValue == null) || (fieldValue.isEmpty()))
            removeField(fieldName);
        else {
            byte[] fieldValue_ = fieldValue.getBytes(StandardCharsets.UTF_8);
            byte[] value = new byte[fieldValue_.length];
            System.arraycopy(fieldValue_, 0, value, 0, fieldValue_.length);
            setFieldBinary(fieldName, value, APETagField.TAG_FIELD_FLAG_DATA_TYPE_TEXT_UTF8);
        }
    }

    public void setFieldBinary(String fieldName, byte[] fieldValue, int fieldFlags) throws IOException {
        if (!analyzed)
            analyze();

        if (fieldName == null)
            return;

        // check to see if we're trying to remove the field (by setting it to NULL or an empty string)
        boolean removing = (fieldValue == null) || (fieldValue.length == 0);

        // get the index
        int fieldIndex = getTagFieldIndex(fieldName);
        if (fieldIndex >= 0) {
            // existing field

            // fail if we're read-only (and not ignoring the read-only flag)
            if ((!ignoreReadOnly) && fields.get(fieldIndex).isReadOnly())
                return;

            // erase the existing field
            if (removing)
                removeField(fieldIndex);

            fields.set(fieldIndex, new APETagField(fieldName, fieldValue, fieldFlags));
        } else {
            if (removing)
                return;

            fields.add(new APETagField(fieldName, fieldValue, fieldFlags));
        }
    }

    // gets the value of a field (returns -1 and an empty buffer if the field doesn't exist)
    public byte[] getFieldBinary(String fieldName) throws IOException {
        if (!analyzed)
            analyze();

        APETagField apeTagField = getTagField(fieldName);
        if (apeTagField == null)
            return null;
        else
            return apeTagField.getFieldValue();
    }

    public String getFieldString(String fieldName) throws IOException {
        if (!analyzed)
            analyze();

        String ret = null;

        APETagField apeTagField = getTagField(fieldName);
        if (apeTagField != null) {
            byte[] b = apeTagField.getFieldValue();
            int boundary = 0;
            int index = b.length - 1;
            while (index >= 0 && b[index] == 0) {
                index--;
                boundary--;
            }
            if (index < 0)
                ret = "";
            else {
                if (apeTagField.getIsUTF8Text() || (apeTagVersion < 2000)) {
                    if (apeTagVersion >= 2000)
                        ret = new String(b, 0, b.length + boundary, StandardCharsets.UTF_8);
                    else
                        ret = new String(b, 0, b.length + boundary, StandardCharsets.US_ASCII);
                } else
                    ret = new String(b, 0, b.length + boundary, StandardCharsets.UTF_16);
            }
        }
        return ret;
    }

    /** remove a specific field */
    public void removeField(String fieldName) throws IOException {
        removeField(getTagFieldIndex(fieldName));
    }

    public void removeField(int index) {
        fields.remove(index);
    }

    /** clear all the fields */
    public void clearFields() {
        fields.clear();
    }

    /**
     * get the total tag bytes in the file from the last analyze
     * need to call Save() then Analyze() to update any changes
     */
    public int getTagBytes() throws IOException {
        if (!analyzed)
            analyze();

        return tagBytes;
    }

    /** see whether the file has an ID3 or APE tag */
    public boolean hasID3Tag() throws IOException {
        if (!analyzed)
            analyze();
        return hasID3Tag;
    }

    public boolean hasAPETag() throws IOException {
        if (!analyzed)
            analyze();
        return hasAPETag;
    }

    public int getAPETagVersion() throws IOException {
        return hasAPETag() ? apeTagVersion : -1;
    }

    /**
     * gets a desired tag field (returns NULL if not found)
     * again, be careful, because this a pointer to the actual field in this class
     */
    public APETagField getTagField(String fieldName) throws IOException {
        int index = getTagFieldIndex(fieldName);
        return (index != -1) ? fields.get(index) : null;
    }

    public APETagField getTagField(int index) throws IOException {
        if (!analyzed)
            analyze();

        if ((index >= 0) && (index < fields.size()))
            return fields.get(index);

        return null;
    }

    public void setIgnoreReadOnly(boolean ignoreReadOnly) {
        this.ignoreReadOnly = ignoreReadOnly;
    }

    /** fills in an ID3_TAG using the current fields (useful for quickly converting the tag) */
    public void createID3Tag(ID3Tag id3Tag) throws IOException {
        if (id3Tag == null)
            return;

        if (!analyzed)
            analyze();

        if (fields.isEmpty())
            return;

        id3Tag.header = "TAG";
        id3Tag.artist = getFieldID3String(APE_TAG_FIELD_ARTIST);
        id3Tag.album = getFieldID3String(APE_TAG_FIELD_ALBUM);
        id3Tag.title = getFieldID3String(APE_TAG_FIELD_TITLE);
        id3Tag.comment = getFieldID3String(APE_TAG_FIELD_COMMENT);
        id3Tag.year = getFieldID3String(APE_TAG_FIELD_YEAR);
        String track = getFieldString(APE_TAG_FIELD_TRACK);
        try {
            id3Tag.track = Short.parseShort(track);
        } catch (Exception e) {
            id3Tag.track = 255;
        }
        id3Tag.genre = (short) (new ID3Genre(getFieldString(APE_TAG_FIELD_GENRE)).getGenre());
    }

    // private functions

    private void analyze() throws IOException {
        // clean-up
        clearFields();
        tagBytes = 0;

        analyzed = true;

        // store the original location
        long originalPosition = io.getFilePointer();

        // check for a tag
        hasID3Tag = false;
        hasAPETag = false;
        apeTagVersion = -1;
        ID3Tag tag = ID3Tag.read(io);

        if (tag != null) {
            hasID3Tag = true;
            tagBytes += ID3Tag.ID3_TAG_BYTES;
        }

        // set the fields
        if (hasID3Tag && tag != null) {
            setFieldID3String(APE_TAG_FIELD_ARTIST, tag.artist);
            setFieldID3String(APE_TAG_FIELD_ALBUM, tag.album);
            setFieldID3String(APE_TAG_FIELD_TITLE, tag.title);
            setFieldID3String(APE_TAG_FIELD_COMMENT, tag.comment);
            setFieldID3String(APE_TAG_FIELD_YEAR, tag.year);
            setFieldString(APE_TAG_FIELD_TRACK, String.valueOf(tag.track));

            if ((tag.genre == ID3Genre.GENRE_UNDEFINED) || (tag.genre >= ID3Genre.genreCount()))
                setFieldString(APE_TAG_FIELD_GENRE, APE_TAG_GENRE_UNDEFINED);
            else
                setFieldString(APE_TAG_FIELD_GENRE, ID3Genre.genreString(tag.genre));
        }

        // try loading the APE tag
        if (!hasID3Tag) {
            footer = APETagFooter.read(io);
            if (footer != null && footer.isValid(false)) {
                hasAPETag = true;
                apeTagVersion = footer.getVersion();

                int rawFieldBytes = footer.getFieldBytes();
                tagBytes += footer.getTotalTagBytes();

                io.seek(io.length() - footer.getTotalTagBytes() - footer.getFieldsOffset());

                try {
                    ByteArrayReader reader = new ByteArrayReader(io, rawFieldBytes);

                    // parse out the raw fields
                    for (int z = 0; z < footer.getNumberFields(); z++)
                        loadField(reader);
                } catch (EOFException e) {
                    throw new JMACException("Can't Read APE Tag Fields");
                }
            }
        }

        // restore the file pointer
        io.seek(originalPosition);
    }

    private int getTagFieldIndex(String fieldName) throws IOException {
        if (!analyzed)
            analyze();
        if (fieldName == null) return -1;

        for (int z = 0; z < fields.size(); z++) {
            if (fieldName.equalsIgnoreCase(fields.get(z).getFieldName()))
                return z;
        }

        return -1;
    }

    private void writeBufferToEndOfIO(byte[] buffer) throws IOException {
        long originalPosition = io.getFilePointer();
        io.seek(io.length());
        io.write(buffer);
        io.seek(originalPosition);
    }

    private void loadField(ByteArrayReader reader) throws IOException {
        // size and flags
        int fieldValueSize = reader.readInt();
        int fieldFlags = reader.readInt();

        String fieldName = reader.readString("UTF-8");

        // value
        byte[] fieldValue = new byte[fieldValueSize];
        reader.readFully(fieldValue);

        // set
        setFieldBinary(fieldName, fieldValue, fieldFlags);
    }

    private void sortFields() {
        // sort the tag fields by size (so that the smallest fields are at the front of the tag)
        Arrays.sort(fields.toArray(APETagField[]::new), this);
    }

    @Override
    public int compare(APETagField a, APETagField b) {
        return a.getFieldSize() - b.getFieldSize();
    }

    // helper set / get field functions
    private String getFieldID3String(String fieldName) throws IOException {
        return getFieldString(fieldName);
    }

    private void setFieldID3String(String fieldName, String fieldValue) throws IOException {
        setFieldString(fieldName, fieldValue.trim());
    }

    public APETagFooter getFooter() {
        return footer;
    }

    // private data
    private final File io;
    private boolean analyzed = false;
    private int tagBytes = 0;
    private final List<APETagField> fields = new ArrayList<>();
    private boolean hasAPETag;
    private int apeTagVersion;
    private boolean hasID3Tag;
    private boolean ignoreReadOnly = false;
    private APETagFooter footer = null;
}
