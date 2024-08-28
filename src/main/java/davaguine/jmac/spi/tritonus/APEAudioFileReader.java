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

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import davaguine.jmac.decoder.IAPEDecompress;
import davaguine.jmac.spi.APEAudioFileFormatType;
import davaguine.jmac.spi.APEEncoding;
import davaguine.jmac.spi.APEPropertiesHelper;
import davaguine.jmac.tools.InputStreamFile;
import davaguine.jmac.tools.JMACException;
import davaguine.jmac.tools.RandomAccessFile;
import org.tritonus.share.sampled.file.TAudioFileReader;

/**
 * @author Dmitry Vaguine
 * @version 12.03.2004 13:35:13
 */

/**
 * Provider for MAC audio file reading.
 */
public class APEAudioFileReader extends TAudioFileReader {

    private final static int MAX_HEADER_SIZE = 16384;
    private final static int MARK_LIMIT = MAX_HEADER_SIZE + 1;

    /**
     * Constructs new instance of reader.
     */
    public APEAudioFileReader() {
        super(MARK_LIMIT, true);
    }

    /**
     * Obtains the audio file format of the input stream provided. The stream must point to valid MAC audio file data.
     *
     * @param stream      - the input stream from which file format information should be extracted
     * @param mediaLength - is the size of audio file
     * @return an APEAudioFileFormat object describing the MAC audio file format
     * @throws UnsupportedAudioFileException - if the stream does not point to valid MAC audio file
     * @throws IOException                   - if an I/O exception occurs
     */
    public AudioFileFormat getAudioFileFormat(InputStream stream, long mediaLength) throws UnsupportedAudioFileException, IOException {
        IAPEDecompress decoder;
        try {
            decoder = IAPEDecompress.CreateIAPEDecompress(new InputStreamFile(stream));
        } catch (JMACException e) {
            throw new UnsupportedAudioFileException("Unsupported audio file");
        } catch (EOFException e) {
            throw new UnsupportedAudioFileException("Unsupported audio file");
        }

        Map fileProperties = new HashMap();
        Map formatProperties = new HashMap();
        APEPropertiesHelper.readProperties(decoder, fileProperties, formatProperties);

        AudioFormat format = new APEAudioFormat(APEEncoding.APE, decoder.getApeInfoSampleRate(),
                decoder.getApeInfoBitsPerSample(),
                decoder.getApeInfoChannels(),
                AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, false, formatProperties);

        return new APEAudioFileFormat(APEAudioFileFormatType.APE, format, AudioSystem.NOT_SPECIFIED, (int) mediaLength, fileProperties);
    }

    /**
     * Obtains the audio file format of the File provided. The File must point to valid audio file data.
     *
     * @param file - the File from which file format information should be extracted
     * @return an APEAudioFileFormat object describing the MAC audio file format
     * @throws UnsupportedAudioFileException - if the File does not point to valid MAC audio file
     * @throws IOException                   - if an I/O exception occurs
     */
    public AudioFileFormat getAudioFileFormat(File file) throws UnsupportedAudioFileException, IOException {
        IAPEDecompress decoder = null;
        davaguine.jmac.tools.File io = new RandomAccessFile(file, "r");
        try {
            decoder = IAPEDecompress.CreateIAPEDecompress(io);
        } catch (JMACException e) {
            throw new UnsupportedAudioFileException("Unsupported audio file");
        } catch (EOFException e) {
            throw new UnsupportedAudioFileException("Unsupported audio file");
        } finally {
            io.close();
        }

        Map fileProperties = new HashMap();
        Map formatProperties = new HashMap();
        APEPropertiesHelper.readProperties(decoder, fileProperties, formatProperties);

        AudioFormat format = new APEAudioFormat(APEEncoding.APE, decoder.getApeInfoSampleRate(),
                decoder.getApeInfoBitsPerSample(),
                decoder.getApeInfoChannels(),
                AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, false, formatProperties);

        return new APEAudioFileFormat(APEAudioFileFormatType.APE, format, AudioSystem.NOT_SPECIFIED, (int) file.length(), fileProperties);
    }

    /**
     * Obtains an audio input stream from the File provided. The File must point to valid MAC audio file data.
     *
     * @param file - the File for which the AudioInputStream should be constructed
     * @return an AudioInputStream object based on the audio file data contained in the input stream.
     * @throws UnsupportedAudioFileException - if the file does not point to valid MAC audio file data recognized by the system
     * @throws IOException                   - if an I/O exception occurs
     */
    public AudioInputStream getAudioInputStream(File file) throws UnsupportedAudioFileException, IOException {
        InputStream inputStream = new FileInputStream(file);
        AudioInputStream audioInputStream;
        try {
            AudioFileFormat audioFileFormat = getAudioFileFormat(file);
            inputStream = new BufferedInputStream(inputStream, MARK_LIMIT);
            audioInputStream = new AudioInputStream(inputStream, audioFileFormat.getFormat(), audioFileFormat.getFrameLength());
        } catch (UnsupportedAudioFileException e) {
            inputStream.close();
            throw e;
        } catch (IOException e) {
            inputStream.close();
            throw e;
        }
        return audioInputStream;
    }

    /**
     * Obtains an audio input stream from the input stream provided. The stream must point to valid MAC audio file data.
     *
     * @param stream - the input stream from which the AudioInputStream should be constructed
     * @return an AudioInputStream object based on the audio file data contained in the input stream.
     * @throws UnsupportedAudioFileException - if the stream does not point to valid MAC audio file data recognized by the system
     * @throws IOException                   - if an I/O exception occurs
     */
    public AudioInputStream getAudioInputStream(InputStream stream) throws UnsupportedAudioFileException, IOException {
        if (!stream.markSupported()) stream = new BufferedInputStream(stream, MARK_LIMIT);
        return super.getAudioInputStream(stream);
    }

}
