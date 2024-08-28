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

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Arrays;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.tritonus.share.sampled.Encodings;
import org.tritonus.share.sampled.convert.TEncodingFormatConversionProvider;

import static java.lang.System.getLogger;


/**
 * A format conversion provider for APE audio file format.
 *
 * @author Dmitry Vaguine
 * @version 01.04.2004 12:31:27
 */
public class APEFormatConversionProvider extends TEncodingFormatConversionProvider {

    private static final Logger logger = getLogger("org.tritonus.TraceAudioConverter");

    private static final AudioFormat.Encoding APE = Encodings.getEncoding("APE");
    private static final AudioFormat.Encoding PCM_SIGNED = Encodings.getEncoding("PCM_SIGNED");

    private static final AudioFormat[] INPUT_FORMATS = {
            // encoding, rate, bits, channels, frameSize, frameRate, big endian
            new AudioFormat(APE, AudioSystem.NOT_SPECIFIED, 8, 1, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, false),
            new AudioFormat(APE, AudioSystem.NOT_SPECIFIED, 8, 2, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, false),
            new AudioFormat(APE, AudioSystem.NOT_SPECIFIED, 16, 1, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, false),
            new AudioFormat(APE, AudioSystem.NOT_SPECIFIED, 16, 2, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, false),
            new AudioFormat(APE, AudioSystem.NOT_SPECIFIED, 24, 1, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, false),
            new AudioFormat(APE, AudioSystem.NOT_SPECIFIED, 24, 2, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, false)
    };

    private static final AudioFormat[] OUTPUT_FORMATS = {
            // encoding, rate, bits, channels, frameSize, frameRate, big endian
            new AudioFormat(PCM_SIGNED, AudioSystem.NOT_SPECIFIED, 8, 1, 1, AudioSystem.NOT_SPECIFIED, false),
            new AudioFormat(PCM_SIGNED, AudioSystem.NOT_SPECIFIED, 8, 2, 2, AudioSystem.NOT_SPECIFIED, false),
            new AudioFormat(PCM_SIGNED, AudioSystem.NOT_SPECIFIED, 16, 1, 2, AudioSystem.NOT_SPECIFIED, false),
            new AudioFormat(PCM_SIGNED, AudioSystem.NOT_SPECIFIED, 16, 2, 4, AudioSystem.NOT_SPECIFIED, false),
            new AudioFormat(PCM_SIGNED, AudioSystem.NOT_SPECIFIED, 24, 1, 3, AudioSystem.NOT_SPECIFIED, false),
            new AudioFormat(PCM_SIGNED, AudioSystem.NOT_SPECIFIED, 24, 2, 6, AudioSystem.NOT_SPECIFIED, false)};

    /**
     * Constructor of conversion provider.
     */
    public APEFormatConversionProvider() {
        super(Arrays.asList(INPUT_FORMATS), Arrays.asList(OUTPUT_FORMATS));
        logger.log(Level.TRACE, ">APEFormatConversionProvider()");
    }

    /**
     * Returns a decoded AudioInputStream in the given target AudioFormat.
     *
     * @param targetFormat     is the target format
     * @param audioInputStream is the source input stream
     * @return a decoded AudioInputStream
     */
    @Override
    public AudioInputStream getAudioInputStream(AudioFormat targetFormat, AudioInputStream audioInputStream) {
        logger.log(Level.TRACE, ">APEFormatConversionProvider.getAudioInputStream(AudioFormat targetFormat, AudioInputStream audioInputStream):");
        return new APEAudioInputStream(targetFormat, audioInputStream);
    }
}
