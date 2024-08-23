/*
 *  21.04.2004 Original verion. davagin@udm.ru.
 *-----------------------------------------------------------------------
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
 *----------------------------------------------------------------------
 */

package davaguine.jmac.spi.tritonus;

import org.tritonus.share.sampled.TAudioFormat;

import javax.sound.sampled.AudioFormat;
import java.util.Map;

/**
 * Author: Dmitry Vaguine
 * Date: 31.03.2004
 * Time: 19:09:05
 */

/**
 * APE audio format parameters.
 * Some parameters might be unavailable. So availability test is required before reading any parameter.
 * <p/>
 * <br>APE parameters.
 * <ul>
 * <li><b>bitrate</b> [Integer], average bit rate in kilobits per second (Standard Property)
 * <li><b>vbr</b> [Boolean], always true, the file is encoded in variable bit rate (VBR)
 * <li><b>quality</b> [Integer], always 10, encoding/conversion quality
 * <p/>
 * <li><b>ape.version</b> [Integer], ape version : 3800, 3970 and etc
 * <li><b>ape.compressionlevel</b> [Integer], the compression level : 1000, 2000, 3000, 4000 (fast, normal, high, extrahigh)
 * <li><b>ape.formatflags</b> [Integer], format flags
 * <li><b>ape.totalframes</b> [Integer], total frames
 * <li><b>ape.blocksperframe</b> [Integer], blocks per frame
 * <li><b>ape.finalframeblocks</b> [Integer], final frame blocks
 * <li><b>ape.blockalign</b> [Integer], block align
 * <li><b>ape.totalblocks</b> [Integer], total blocks
 * <li><b>ape.peaklevel</b> [Integer], peak level
 * </ul>
 */
public class APEAudioFormat extends TAudioFormat {

    /**
     * Constructs an APEAudioFormat with the given parameters. The encoding specifies the
     * convention used to represent the data. The other parameters are further explained
     * in the class description.
     *
     * @param encoding         - the audio encoding technique
     * @param sampleRate       - the number of samples per second
     * @param sampleSizeInBits - the number of bits in each sample
     * @param channels         - the number of channels (1 for mono, 2 for stereo, and so on)
     * @param frameSize        - the number of bytes in each frame
     * @param frameRate        - the number of frames per second
     * @param bigEndian        - indicates whether the data for a single sample is stored in big-endian byte order (false means little-endian)
     * @param properties       - audio properties
     */
    public APEAudioFormat(AudioFormat.Encoding encoding, float sampleRate, int sampleSizeInBits, int channels, int frameSize, float frameRate, boolean bigEndian, Map properties) {
        super(encoding, sampleRate, sampleSizeInBits, channels, frameSize, frameRate, bigEndian, properties);
    }

}
