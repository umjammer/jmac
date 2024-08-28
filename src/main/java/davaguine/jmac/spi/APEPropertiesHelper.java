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
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import davaguine.jmac.decoder.IAPEDecompress;
import davaguine.jmac.info.APETag;

import static java.lang.System.getLogger;


/**
 * APE properties helper.
 *
 * @author Dmitry Vaguine
 * @version 13.05.2004 11:48:13
 */
public class APEPropertiesHelper {

    private static final Logger logger = getLogger(APEPropertiesHelper.class.getName());

    /**
     * Reads the properties from APE file and creates two Map's (for audio file format and for audio format).
     *
     * @param decoder          decoder from which this method should get the properties.
     * @param fileProperties   properties Map for audio file format properties
     * @param formatProperties properties Map for audio format properties
     * @throws IOException in case of IO error occured
     */
    public static void readProperties(IAPEDecompress decoder, Map<String, Object> fileProperties, Map<String, Object> formatProperties) throws IOException {
        formatProperties.put("bitrate", decoder.getApeInfoDecompressAverageBitrate());
        formatProperties.put("vbr", Boolean.TRUE);
        formatProperties.put("quality", 10);

        formatProperties.put("ape.version", decoder.getApeInfoFileVersion());
        formatProperties.put("ape.compressionlevel", decoder.getApeInfoCompressionLevel());
        formatProperties.put("ape.formatflags", decoder.getApeInfoFormatFlags());
        formatProperties.put("ape.totalframes", decoder.getApeInfoTotalFrames());
        formatProperties.put("ape.blocksperframe", decoder.getApeInfoBlocksPerFrame());
        formatProperties.put("ape.finalframeblocks", decoder.getApeInfoFinalFrameBlocks());
        formatProperties.put("ape.blockalign", decoder.getApeInfoBlockAlign());
        formatProperties.put("ape.totalblocks", decoder.getApeInfoTotalBlocks());
        formatProperties.put("ape.peaklevel", decoder.getApeInfoPeakLevel());

        fileProperties.put("duration", (long) decoder.getApeInfoLengthMs());
        if (decoder.getApeInfoIoSource().isLocal()) {
            APETag tag = decoder.getApeInfoTag();
            fileProperties.put("author", tag.getFieldString(APETag.APE_TAG_FIELD_ARTIST));
            fileProperties.put("title", tag.getFieldString(APETag.APE_TAG_FIELD_TITLE));
            fileProperties.put("copyright", tag.getFieldString(APETag.APE_TAG_FIELD_COPYRIGHT));
            String year = tag.getFieldString(APETag.APE_TAG_FIELD_YEAR);
            Date date = null;
            try {
                Calendar c = Calendar.getInstance();
                c.clear();
                c.set(Calendar.YEAR, Integer.parseInt(year));
                date = c.getTime();
            } catch (Exception ignored) {
            }
            fileProperties.put("date", date);
            fileProperties.put("comment", tag.getFieldString(APETag.APE_TAG_FIELD_COMMENT));

            fileProperties.put("album", tag.getFieldString(APETag.APE_TAG_FIELD_ALBUM));
            fileProperties.put("track", tag.getFieldString(APETag.APE_TAG_FIELD_TRACK));
            fileProperties.put("genre", tag.getFieldString(APETag.APE_TAG_FIELD_GENRE));
        }
        if (logger.isLoggable(Level.DEBUG)) {
            System.err.println("File Properties");
            System.err.println("duration: " + fileProperties.get("duration"));
            System.err.println("author: " + fileProperties.get("author"));
            System.err.println("title: " + fileProperties.get("title"));
            System.err.println("copyright: " + fileProperties.get("copyright"));
            System.err.println("date: " + fileProperties.get("date"));
            System.err.println("comment: " + fileProperties.get("comment"));
            System.err.println("album: " + fileProperties.get("album"));
            System.err.println("track: " + fileProperties.get("track"));
            System.err.println("genre: " + fileProperties.get("genre"));

            System.err.println("Format Properties");
            System.err.println("bitrate: " + formatProperties.get("bitrate"));
            System.err.println("vbr: " + formatProperties.get("vbr"));
            System.err.println("quality: " + formatProperties.get("quality"));

            System.err.println("ape.version: " + formatProperties.get("ape.version"));
            System.err.println("ape.compressionlevel: " + formatProperties.get("ape.compressionlevel"));
            System.err.println("ape.formatflags: " + formatProperties.get("ape.formatflags"));
            System.err.println("ape.totalframes: " + formatProperties.get("ape.totalframes"));
            System.err.println("ape.blocksperframe: " + formatProperties.get("ape.blocksperframe"));
            System.err.println("ape.finalframeblocks: " + formatProperties.get("ape.finalframeblocks"));
            System.err.println("ape.blockalign: " + formatProperties.get("ape.blockalign"));
            System.err.println("ape.totalblocks: " + formatProperties.get("ape.totalblocks"));
            System.err.println("ape.peaklevel: " + formatProperties.get("ape.peaklevel"));
        }
    }
}
