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

import java.util.Map;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;


/**
 * An instance of the APEAudioFileFormat class describes MAC audio file, including the file type,
 * the file's length in bytes, the length in sample frames of the audio data contained in the file,
 * and the format of the audio data.
 *
 * @author Dmitry Vaguine
 * @version 12.03.2004 13:35:13
 */
public class APEAudioFileFormat extends AudioFileFormat {

    /**
     * Constructs an audio file format object for MAC audio format.
     *
     * @param type        the type of the audio file
     * @param format      the format of the audio data contained in the file
     * @param frameLength the audio data length in sample frames, or AudioSystem.NOT_SPECIFIED
     * @param properties  file format properties
     */
    public APEAudioFileFormat(Type type, AudioFormat format, int frameLength, Map<String, Object> properties) {
        super(type, format, frameLength, properties);
    }
}
