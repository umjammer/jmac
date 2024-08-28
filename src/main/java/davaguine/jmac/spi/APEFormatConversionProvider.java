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

import java.io.PrintStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.spi.FormatConversionProvider;

import static java.lang.System.getLogger;


/**
 * A format conversion provider for APE audio file format.
 *
 * @author Dmitry Vaguine
 * @version 12.03.2004 13:35:13
 */
public class APEFormatConversionProvider extends FormatConversionProvider {

    private static final Logger logger = getLogger(APEFormatConversionProvider.class.getName());

    /**
     * Source formats of provider.
     */
    protected static final AudioFormat[] SOURCE_FORMATS = {
            // encoding, rate, bits, channels, frameSize, frameRate, big endian
            new AudioFormat(APEEncoding.APE, AudioSystem.NOT_SPECIFIED, 8, 1, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, false),
            new AudioFormat(APEEncoding.APE, AudioSystem.NOT_SPECIFIED, 8, 2, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, false),
            new AudioFormat(APEEncoding.APE, AudioSystem.NOT_SPECIFIED, 16, 1, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, false),
            new AudioFormat(APEEncoding.APE, AudioSystem.NOT_SPECIFIED, 16, 2, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, false),
            new AudioFormat(APEEncoding.APE, AudioSystem.NOT_SPECIFIED, 24, 1, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, false),
            new AudioFormat(APEEncoding.APE, AudioSystem.NOT_SPECIFIED, 24, 2, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, false)};

    /**
     * Source encodings of provider.
     */
    protected static AudioFormat.Encoding[] SOURCE_ENCODINGS;

    /**
     * Target formats of provider.
     */
    protected static final AudioFormat[] TARGET_FORMATS = {
            // rate, bits, channels, signed, big endian
            new AudioFormat(AudioSystem.NOT_SPECIFIED, 8, 1, true, false),
            new AudioFormat(AudioSystem.NOT_SPECIFIED, 8, 2, true, false),
            new AudioFormat(AudioSystem.NOT_SPECIFIED, 16, 1, true, false),
            new AudioFormat(AudioSystem.NOT_SPECIFIED, 16, 2, true, false),
            new AudioFormat(AudioSystem.NOT_SPECIFIED, 24, 1, true, false),
            new AudioFormat(AudioSystem.NOT_SPECIFIED, 24, 2, true, false)};

    /**
     * Target encodings of provider.
     */
    protected static AudioFormat.Encoding[] TARGET_ENCODINGS;

    /**
     * An internal field used to map from source AudioFormats to target AudioFormat.Encodings.
     * Map: key=source format, value=AudioFormat.Encoding [] of unique target encodings
     */
    protected Map<AudioFormat, AudioFormat.Encoding[]> sourceFormatTargetEncodings;

    /**
     * An internal field used to map from source AudioFormats to target AudioFormats.
     * Map: key=target format, value=hashtable: key=encoding, value=Vector of unique target formats
     */
    protected Map<AudioFormat, Map<AudioFormat.Encoding, List<AudioFormat>>> sourceFormatTargetFormats;

    /**
     * Constructor of conversion provider.
     */
    public APEFormatConversionProvider() {
        // Create sets of encodings from formats.
        SOURCE_ENCODINGS = createEncodings(SOURCE_FORMATS);
        TARGET_ENCODINGS = createEncodings(TARGET_FORMATS);
        createConversions(SOURCE_FORMATS, TARGET_FORMATS);
    }

    /**
     * This helper method creates encodings from the list of AudioFormats.
     *
     * @param sourceFormats the source formats
     * @param targetFormats the target formats
     */
    protected void createConversions(AudioFormat[] sourceFormats, AudioFormat[] targetFormats) {

        sourceFormatTargetEncodings = new HashMap<>();
        sourceFormatTargetFormats = new HashMap<>();

        for (AudioFormat sourceFormat : sourceFormats) {
            List<Encoding> supportedTargetEncodings = new ArrayList<>();
            Map<Encoding, List<AudioFormat>> targetEncodingTargetFormats = new HashMap<>();
            sourceFormatTargetFormats.put(sourceFormat, targetEncodingTargetFormats);

            for (AudioFormat targetFormat : targetFormats) {
                // Simplistic: Assume conversion possible if sampling rate and channels match.
                // Depends on what streams can be decoded by the APE subsystem.
                boolean conversionPossible =
                        (sourceFormat.getSampleRate() == targetFormat.getSampleRate()) &&
                                (sourceFormat.getChannels() == targetFormat.getChannels()) &&
                                (sourceFormat.getSampleSizeInBits() == targetFormat.getSampleSizeInBits());

                if (conversionPossible) {
                    Encoding targetEncoding = targetFormat.getEncoding();

                    if (!supportedTargetEncodings.contains(targetEncoding))
                        supportedTargetEncodings.add(targetEncoding);

                    // Will be converted to an AudioFormat [] when queried
                    List<AudioFormat> supportedTargetFormats = targetEncodingTargetFormats.computeIfAbsent(targetEncoding, k -> new ArrayList<>());
                    supportedTargetFormats.add(targetFormat);
                }
            }

            // Convert supported target encodings from vector to []
            Encoding[] targetEncodings = supportedTargetEncodings.toArray(Encoding[]::new);
            sourceFormatTargetEncodings.put(sourceFormat, targetEncodings);
        }
    }

    /**
     * Returns the source AudioFormat.Encodings that this class can read from.
     *
     * @return the source AudioFormat.Encodings
     */
    @Override
    public AudioFormat.Encoding[] getSourceEncodings() {
        return SOURCE_ENCODINGS;
    }

    /**
     * Returns the target AudioFormat.Encodings that this class can convert to.
     *
     * @return the target AudioFormat.Encodings
     */
    @Override
    public AudioFormat.Encoding[] getTargetEncodings() {
        return TARGET_ENCODINGS;
    }

    /**
     * Returns the target AudioFormat.Encodings that this class can convert to from the given format.
     *
     * @param sourceFormat is the source format
     * @return the target AudioFormat.Encodings
     */
    @Override
    @SuppressWarnings("WhileLoopReplaceableByForEach")
    public AudioFormat.Encoding[] getTargetEncodings(AudioFormat sourceFormat) {
        logger.log(Level.TRACE, "APEFormatConversionProvider.getTargetEncodings( sourceFormat )");
        logger.log(Level.TRACE, "   sourceFormat=" + sourceFormat);

        // Must use iteration since Hashtable contains and get uses equals
        // and AudioFormat does not implement this and finalizes it.
        Iterator<Map.Entry<AudioFormat, AudioFormat.Encoding[]>> iterator = sourceFormatTargetEncodings.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<AudioFormat, AudioFormat.Encoding[]> entry = iterator.next();
            AudioFormat format = entry.getKey();
            if (format.matches(sourceFormat)) {
                AudioFormat.Encoding[] targetEncodings = entry.getValue();
                logger.log(Level.DEBUG, "   targetEncodings:");
                if (logger.isLoggable(Level.DEBUG)) printAudioEncodings(targetEncodings, System.err);
                return targetEncodings;
            }
        }
        return new AudioFormat.Encoding[0];
    }

    /**
     * Returns the target AudioFormat.Encodings that this class can convert to
     * from the given format and encoding.
     *
     * @param targetEncoding - the target encoding
     * @param sourceFormat   - the source format
     * @return the target AudioFormat.Encodings
     */
    @Override
    @SuppressWarnings("WhileLoopReplaceableByForEach")
    public AudioFormat[] getTargetFormats(AudioFormat.Encoding targetEncoding, AudioFormat sourceFormat) {
        logger.log(Level.TRACE, "APEFormatConversionProvider.getTargetFormats( sourceFormat )");
        logger.log(Level.TRACE, "   sourceFormat=" + sourceFormat);

        // Must use iteration since Hashtable contains and get uses equals
        // and AudioFormat does not implement this and finalizes it.
        Iterator<Entry<AudioFormat, Map<Encoding, List<AudioFormat>>>> iterator = sourceFormatTargetFormats.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<AudioFormat, Map<Encoding, List<AudioFormat>>> entry = iterator.next();
            AudioFormat format = entry.getKey();

            if (sourceFormat.matches(format)) {
                Map<Encoding, List<AudioFormat>> targetEncodings = entry.getValue();
                List<AudioFormat> targetFormats = targetEncodings.get(targetEncoding);
                AudioFormat[] targetFormatArray = new AudioFormat[targetFormats.size()];
                AudioFormat ft;
                for (int i = 0; i < targetFormats.size(); i++) {
                    ft = targetFormats.get(i);
                    targetFormatArray[i] = new AudioFormat(ft.getEncoding(), sourceFormat.getSampleRate(), ft.getSampleSizeInBits(), ft.getChannels(), ft.getFrameSize(), ft.getFrameRate(), ft.isBigEndian());
                }
                logger.log(Level.TRACE, "   targetFormats");
                if (logger.isLoggable(Level.TRACE)) printAudioFormats(targetFormatArray, System.err);
                return targetFormatArray;
            }
        }
        return new AudioFormat[0];
    }

    /**
     * Returns a decoded AudioInputStream in the given target AudioFormat.Encoding.
     *
     * @param targetEncoding   is the target encoding
     * @param audioInputStream is the source input stream
     * @return a decoded AudioInputStream
     */
    @Override
    public AudioInputStream getAudioInputStream(AudioFormat.Encoding targetEncoding,
                                                AudioInputStream audioInputStream) {
        AudioFormat sourceFormat = audioInputStream.getFormat();
        AudioFormat targetFormat = new AudioFormat(targetEncoding,
                sourceFormat.getSampleRate(),
                sourceFormat.getSampleSizeInBits(),
                sourceFormat.getChannels(),
                sourceFormat.getFrameSize(),
                sourceFormat.getFrameRate(),
                sourceFormat.isBigEndian());
        return getAudioInputStream(targetFormat, audioInputStream);
    }

    /**
     * Returns a decoded AudioInputStream in the given target AudioFormat.
     *
     * @param targetFormat     is the target format
     * @param audioInputStream is the source input stream
     * @return a decoded AudioInputStream
     */
    @Override
    public AudioInputStream getAudioInputStream(AudioFormat targetFormat,
                                                AudioInputStream audioInputStream) {
        if (isConversionSupported(targetFormat, audioInputStream.getFormat())) {
            logger.log(Level.TRACE, "APEFormatConversionProvider.getAudioInputStream( targetEnc, audioInputStream )");
            return new APEAudioInputStream(targetFormat, audioInputStream);
        }
        throw new IllegalArgumentException("conversion not supported");
    }

    /**
     * Returns whether the given source AudioFormat.Encoding is supported.
     *
     * @param sourceEncoding is the source encoding
     * @return true if the given source AudioFormat.Encoding is supported
     */
    @Override
    public boolean isSourceEncodingSupported(AudioFormat.Encoding sourceEncoding) {
        return containsEncoding(SOURCE_ENCODINGS, sourceEncoding);
    }

    /**
     * Returns whether the given target AudioFormat.Encoding is supported.
     *
     * @param targetEncoding is the target encoding
     * @return true if the given target AudioFormat.Encoding is supported
     */
    @Override
    public boolean isTargetEncodingSupported(AudioFormat.Encoding targetEncoding) {
        return containsEncoding(TARGET_ENCODINGS, targetEncoding);
    }

    /**
     * Creates the array of encodings for given array of target formats
     *
     * @param formats is the array of target formats
     * @return the array of encodings for given array of target formats
     */
    protected static AudioFormat.Encoding[] createEncodings(AudioFormat[] formats) {
        if ((formats == null) || (formats.length == 0)) return new AudioFormat.Encoding[0];
        List<AudioFormat.Encoding> encodings = new ArrayList<>();
        for (AudioFormat format : formats) {
            Encoding encoding = format.getEncoding();
            if (!encodings.contains(encoding))
                encodings.add(encoding);
        }
        AudioFormat.Encoding[] encodingArray = encodings.toArray(AudioFormat.Encoding[]::new);
        return encodingArray;
    }

    /**
     * This method determines is the given array of encodings contains the specified encoding
     *
     * @param encodings is an array of encodings
     * @param encoding  is a specified encoding
     * @return true if the given array of encodings contains the specified encoding
     */
    public static boolean containsEncoding(AudioFormat.Encoding[] encodings, AudioFormat.Encoding encoding) {
        if ((encodings == null) || (encoding == null)) return false;
        for (Encoding value : encodings) {
            if (value.equals(encoding)) return true;
        }
        return false;
    }

    /**
     * Prints the array of audio formats to the given PrintStream
     *
     * @param audioFormats is a given array og audio formats
     * @param stream       is a given PrintStream
     */
    public static void printAudioFormats(AudioFormat[] audioFormats, PrintStream stream) {
        for (AudioFormat audioFormat : audioFormats) stream.println("   " + audioFormat);
    }

    /**
     * Prints the array of audio encodings to the given PrintStream
     *
     * @param audioEncodings is a given array of audio encodings
     * @param stream         is a given PrintStream
     */
    public static void printAudioEncodings(AudioFormat.Encoding[] audioEncodings, PrintStream stream) {
        for (Encoding audioEncoding : audioEncodings) stream.println("   " + audioEncoding);
    }
}
