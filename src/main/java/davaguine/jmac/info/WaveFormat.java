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

import java.io.IOException;

import davaguine.jmac.tools.ByteArrayReader;
import davaguine.jmac.tools.File;


/**
 * @author Dmitry Vaguine
 * @version 04.03.2004 14:51:31
 */
public class WaveFormat {

    /** format type */
    public short formatTag;
    /** number of channels (i.e. mono, stereo...) */
    public short channels;
    /** sample rate */
    public int samplesPerSec;
    /** for buffer estimation */
    public int avgBytesPerSec;
    /** block size of data */
    public short blockAlign;
    /** number of bits per sample of mono data */
    public short bitsPerSample;
    /** the count in bytes of the size of */
    public short size;

    public final static int WAV_HEADER_SIZE = 16;

    public static void fillWaveFormatEx(WaveFormat waveFormatEx, int sampleRate, int bitsPerSample, int channels) {
        waveFormatEx.size = 0;
        waveFormatEx.samplesPerSec = sampleRate;
        waveFormatEx.bitsPerSample = (short) bitsPerSample;
        waveFormatEx.channels = (short) channels;
        waveFormatEx.formatTag = 1;

        waveFormatEx.blockAlign = (short) ((waveFormatEx.bitsPerSample / 8) * waveFormatEx.channels);
        waveFormatEx.avgBytesPerSec = waveFormatEx.blockAlign * waveFormatEx.samplesPerSec;
    }

    public void readHeader(File io) throws IOException {
        ByteArrayReader reader = new ByteArrayReader(io, WAV_HEADER_SIZE);
        formatTag = reader.readShort();
        channels = reader.readShort();
        samplesPerSec = reader.readInt();
        avgBytesPerSec = reader.readInt();
        blockAlign = reader.readShort();
        bitsPerSample = reader.readShort();
    }
}
