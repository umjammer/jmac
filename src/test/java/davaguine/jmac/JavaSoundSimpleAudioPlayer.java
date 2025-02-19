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

package davaguine.jmac;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;


/**
 * @author Dmitry Vaguine
 * @version 12.03.2004 13:35:13
 */
public class JavaSoundSimpleAudioPlayer {

    /**
     * Plays audio from given file names.
     */
    public static void main(String[] args) {
        // Check for given sound file names.
        if (args.length < 1) {
            System.out.println("Play usage:");
            System.out.println("\tjava Play <sound file names>*");
            System.exit(0);
        }

        // Process arguments.
        for (String arg : args) playAudioFile(arg);

        // Must exit explicitly since audio creates non-daemon threads.
        System.exit(0);
    }

    /**
     * Play audio from the given file name.
     */
    public static void playAudioFile(String fileName) {
        try {
            AudioInputStream audioInputStream;
            AudioFileFormat audioFileFormat = null;
            try {
                URL url = new URL(fileName);
                audioFileFormat = AudioSystem.getAudioFileFormat(url);
                audioInputStream = AudioSystem.getAudioInputStream(url);
            } catch (MalformedURLException e) {
                File soundFile = new File(fileName);
                audioFileFormat = AudioSystem.getAudioFileFormat(soundFile);
                audioInputStream = AudioSystem.getAudioInputStream(soundFile);
            }

            // Create a stream from the given file.
            // Throws IOException or UnsupportedAudioFileException
            playAudioStream(audioInputStream);
        } catch (Exception e) {
            System.out.println("Problem with file " + fileName + ":");
            e.printStackTrace();
        }
    }

    /**
     * Plays audio from the given audio input stream.
     */
    public static void playAudioStream(AudioInputStream audioInputStream) {
        // Audio format provides information like sample rate, size, channels.
        AudioFormat audioFormat = audioInputStream.getFormat();

        // Convert compressed audio data to uncompressed PCM format.
        if (audioFormat.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
            AudioFormat newFormat = new AudioFormat(audioFormat.getSampleRate(),
                    audioFormat.getSampleSizeInBits(),
                    audioFormat.getChannels(),
                    true,
                    false);
            System.out.println("Converting audio format to " + newFormat);
            AudioInputStream newStream = AudioSystem.getAudioInputStream(newFormat, audioInputStream);
            audioFormat = newFormat;
            audioInputStream = newStream;
        }

        // Open a data line to play our type of sampled audio.
        // Use SourceDataLine for play and TargetDataLine for record.
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        if (!AudioSystem.isLineSupported(info)) {
            System.out.println("Play.playAudioStream does not handle this type of audio on this system.");
            return;
        }

        try {
            // Create a SourceDataLine for play back (throws LineUnavailableException).
            SourceDataLine dataLine = (SourceDataLine) AudioSystem.getLine(info);
            System.out.println("SourceDataLine class=" + dataLine.getClass());

            // The line acquires system resources (throws LineAvailableException).
            dataLine.open(audioFormat);

            // Adjust the volume on the output line.
            if (dataLine.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl volume = (FloatControl) dataLine.getControl(FloatControl.Type.MASTER_GAIN);
                volume.setValue((volume.getMaximum() - volume.getMinimum()) * 0.8f + volume.getMinimum());
            }

            // Allows the line to move data in and out to a port.
            dataLine.start();

            // Create a buffer for moving data from the audio stream to the line.
            int bufferSize = (int) audioFormat.getSampleRate() * audioFormat.getFrameSize();
            byte[] buffer = new byte[bufferSize];

            // Move the data until done or there is an error.
            try {
                int bytesRead = 0;
                while (bytesRead >= 0) {
                    bytesRead = audioInputStream.read(buffer, 0, buffer.length);
                    if (bytesRead >= 0) {
//System.out.println("Play.playAudioStream bytes read=" + bytesRead +
// ", frame size=" + audioFormat.getFrameSize() + ", frames read=" + bytesRead / audioFormat.getFrameSize());
                        // Odd sized sounds throw an exception if we don't write the same amount.
                        int framesWritten = dataLine.write(buffer, 0, bytesRead);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("Play.playAudioStream draining line.");
            // Continues data line I/O until its buffer is drained.
            dataLine.drain();

            System.out.println("Play.playAudioStream closing line.");
            // Closes the data line, freeing any resources such as the audio device.
            dataLine.close();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }
}
