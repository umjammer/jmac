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

package davaguine.jmac.spi.tritonus;

import java.util.Map;
import javax.sound.sampled.AudioFormat;

import org.tritonus.share.sampled.file.TAudioFileFormat;


/**
 * An instance of the APEAudioFileFormat class describes MAC audio file, including the file type,
 * the file's length in bytes, the length in sample frames of the audio data contained in the file,
 * and the format of the audio data.
 *
 * @author Dmitry Vaguine
 * @version 12.03.2004 13:35:13
 */
public class APEAudioFileFormat extends TAudioFileFormat {

    /**
     * Constructs an audio file format object for MAC audio format.
     *
     * @param type        - the type of the audio file
     * @param format      - the format of the audio data contained in the file
     * @param byteLength  - the length of the file in bytes, or AudioSystem.NOT_SPECIFIED
     * @param frameLength - the audio data length in sample frames, or AudioSystem.NOT_SPECIFIED
     * @param properties  - the audio file format properties
     */
    public APEAudioFileFormat(Type type, AudioFormat format, int byteLength, int frameLength, Map properties) {
        super(type, format, byteLength, frameLength, properties);
    }

    /**
     * APE audio file format parameters.
     * Some parameters might be unavailable. So availability test is required before reading any parameter.
     * <p/>
     * <br>AudioFileFormat parameters.
     * <ul>
     * <li><b>duration</b> [Long], Duration in microseconds. (standard property)
     * <li><b>author</b> [String], Name of the author of the stream. (standard property)
     * <li><b>title</b> [String], Title of the stream. (standard property)
     * <li><b>copyright</b> [String], Copyright message of the stream. (standard property)
     * <li><b>date</b> [Date], The date (year) of the recording or release of the stream. (standard property)
     * <li><b>comment</b> [String], Comment of the stream. (standard property)
     * <li><b>album</b> [String], Name of the album of the stream.
     * <li><b>track</b> [String], The track number of the stream
     * <li><b>genre</b> [String], The genre of the stream
     * </ul>
     */
    public Map properties() {
        return super.properties();
    }
}
