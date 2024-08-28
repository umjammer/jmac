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

import davaguine.jmac.tools.ByteArrayReader;
import davaguine.jmac.tools.ByteArrayWriter;
import davaguine.jmac.tools.File;


/**
 * @author Dmitry Vaguine
 * @version 04.03.2004 14:51:31
 */
public class WaveHeader {

    public final static int WAVE_HEADER_BYTES = 44;

    // RIFF header
    public String riffHeader;
    public long riffBytes;

    // data type
    public String dataTypeID;

    // wave format
    public String formatHeader;
    public long formatBytes;

    public int formatTag;
    public int channels;
    public long samplesPerSec;
    public long avgBytesPerSec;
    public int blockAlign;
    public int bitsPerSample;

    // data chunk header
    public String dataHeader;
    public long dataBytes;

    public static void fillWaveHeader(WaveHeader wavHeader, int audioBytes, WaveFormat waveFormatEx, int terminatingBytes) {
        // RIFF header
        wavHeader.riffHeader = "RIFF";
        wavHeader.riffBytes = (audioBytes + 44) - 8 + terminatingBytes;

        // format header
        wavHeader.dataTypeID = "WAVE";
        wavHeader.formatHeader = "fmt ";

        // the format chunk is the first 16 bytes of a waveformatex
        wavHeader.formatBytes = 16;
        wavHeader.formatTag = waveFormatEx.formatTag;
        wavHeader.channels = waveFormatEx.channels;
        wavHeader.samplesPerSec = waveFormatEx.samplesPerSec;
        wavHeader.avgBytesPerSec = waveFormatEx.avgBytesPerSec;
        wavHeader.blockAlign = waveFormatEx.blockAlign;
        wavHeader.bitsPerSample = waveFormatEx.bitsPerSample;

        // the data header
        wavHeader.dataHeader = "data";
        wavHeader.dataBytes = audioBytes;
    }

    public static WaveHeader read(File file) throws IOException {
        try {
            ByteArrayReader reader = new ByteArrayReader(file, WAVE_HEADER_BYTES);
            return read(reader);
        } catch (EOFException e) {
            return null;
        }
    }

    public static WaveHeader read(byte[] data) {
        ByteArrayReader reader = new ByteArrayReader(data);
        return read(reader);
    }

    private static WaveHeader read(ByteArrayReader reader) {
        WaveHeader header = new WaveHeader();
        header.riffHeader = reader.readString(4, StandardCharsets.US_ASCII.name());
        header.riffBytes = reader.readUnsignedInt();
        header.dataTypeID = reader.readString(4, StandardCharsets.US_ASCII.name());
        header.formatHeader = reader.readString(4, StandardCharsets.US_ASCII.name());
        header.formatBytes = reader.readUnsignedInt();
        header.formatTag = reader.readUnsignedShort();
        header.channels = reader.readUnsignedShort();
        header.samplesPerSec = reader.readUnsignedInt();
        header.avgBytesPerSec = reader.readUnsignedInt();
        header.blockAlign = reader.readUnsignedShort();
        header.bitsPerSample = reader.readUnsignedShort();
        header.dataHeader = reader.readString(4, StandardCharsets.US_ASCII.name());
        header.dataBytes = reader.readUnsignedInt();
        return header;
    }

    public final byte[] write() {
        ByteArrayWriter writer = new ByteArrayWriter(WAVE_HEADER_BYTES);
        writer.writeString(riffHeader, 4, StandardCharsets.US_ASCII.name());
        writer.writeUnsignedInt(riffBytes);
        writer.writeString(dataTypeID, 4, StandardCharsets.US_ASCII.name());
        writer.writeString(formatHeader, 4, StandardCharsets.US_ASCII.name());
        writer.writeUnsignedInt(formatBytes);
        writer.writeUnsignedShort(formatTag);
        writer.writeUnsignedShort(channels);
        writer.writeUnsignedInt(samplesPerSec);
        writer.writeUnsignedInt(avgBytesPerSec);
        writer.writeUnsignedShort(blockAlign);
        writer.writeUnsignedShort(bitsPerSample);
        writer.writeString(dataHeader, 4, StandardCharsets.US_ASCII.name());
        writer.writeUnsignedInt(dataBytes);
        return writer.getBytes();
    }
}