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

import davaguine.jmac.tools.ByteArrayReader;
import davaguine.jmac.tools.File;
import davaguine.jmac.tools.JMACException;


/**
 * @author Dmitry Vaguine
 * @version 04.03.2004 14:51:31
 */
public class APEHeader {

    /** is 8-bit */
    public final static int MAC_FORMAT_FLAG_8_BIT = 1;
    /** uses the new CRC32 error detection */
    public final static int MAC_FORMAT_FLAG_CRC = 2;
    /** unsigned __int32 Peak_Level after the header */
    public final static int MAC_FORMAT_FLAG_HAS_PEAK_LEVEL = 4;
    /** is 24-bit */
    public final static int MAC_FORMAT_FLAG_24_BIT = 8;
    /** has the number of seek elements after the peak level */
    public final static int MAC_FORMAT_FLAG_HAS_SEEK_ELEMENTS = 16;
    /** create the wave header on decompression (not stored) */
    public final static int MAC_FORMAT_FLAG_CREATE_WAV_HEADER = 32;

    public APEHeader(File file) {
        io = file;
    }

    public void analyze(APEFileInfo info) throws IOException {
        // find the descriptor
        info.junkHeaderBytes = findDescriptor(true);
        if (info.junkHeaderBytes < 0)
            throw new JMACException("Unsupported Format");

        // read the first 8 bytes of the descriptor (ID and version)
        io.mark(10);
        ByteArrayReader reader = new ByteArrayReader(io, 8);
        if (!reader.readString(4, "US-ASCII").equals("MAC "))
            throw new JMACException("Unsupported Format");

        int version = reader.readUnsignedShort();

        io.reset();

        if (version >= 3980) {
            // current header format
            analyzeCurrent(info);
        } else {
            // legacy support
            AnalyzeOld(info);
        }
    }

    protected void analyzeCurrent(APEFileInfo apeFileInfo) throws IOException {
        apeFileInfo.apeDescriptor = APEDescriptor.read(io);

        if ((apeFileInfo.apeDescriptor.descriptorBytes - APEDescriptor.APE_DESCRIPTOR_BYTES) > 0)
            io.skipBytes((int) (apeFileInfo.apeDescriptor.descriptorBytes - APEDescriptor.APE_DESCRIPTOR_BYTES));

        APEHeaderNew apeHeader = APEHeaderNew.read(io);

        if ((apeFileInfo.apeDescriptor.headerBytes - APEHeaderNew.APE_HEADER_BYTES) > 0)
            io.skipBytes((int) (apeFileInfo.apeDescriptor.headerBytes - APEHeaderNew.APE_HEADER_BYTES));

        // fill the APE info structure
        apeFileInfo.version = apeFileInfo.apeDescriptor.version;
        apeFileInfo.compressionLevel = apeHeader.compressionLevel;
        apeFileInfo.formatFlags = apeHeader.formatFlags;
        apeFileInfo.totalFrames = (int) apeHeader.totalFrames;
        apeFileInfo.finalFrameBlocks = (int) apeHeader.finalFrameBlocks;
        apeFileInfo.blocksPerFrame = (int) apeHeader.blocksPerFrame;
        apeFileInfo.channels = apeHeader.channels;
        apeFileInfo.sampleRate = (int) apeHeader.sampleRate;
        apeFileInfo.bitsPerSample = apeHeader.bitsPerSample;
        apeFileInfo.bytesPerSample = apeFileInfo.bitsPerSample / 8;
        apeFileInfo.blockAlign = apeFileInfo.bytesPerSample * apeFileInfo.channels;
        apeFileInfo.totalBlocks = (int) ((apeHeader.totalFrames == 0) ? 0 : ((apeHeader.totalFrames - 1) * apeFileInfo.blocksPerFrame) + apeHeader.finalFrameBlocks);
        apeFileInfo.wavHeaderBytes = (int) ((apeHeader.formatFlags & MAC_FORMAT_FLAG_CREATE_WAV_HEADER) > 0 ? WaveHeader.WAVE_HEADER_BYTES : apeFileInfo.apeDescriptor.headerDataBytes);
        apeFileInfo.wavTerminatingBytes = (int) apeFileInfo.apeDescriptor.terminatingDataBytes;
        apeFileInfo.wavDataBytes = apeFileInfo.totalBlocks * apeFileInfo.blockAlign;
        apeFileInfo.wavTotalBytes = apeFileInfo.wavDataBytes + apeFileInfo.wavHeaderBytes + apeFileInfo.wavTerminatingBytes;
        apeFileInfo.apeTotalBytes = io.isLocal() ? (int) io.length() : -1;
        apeFileInfo.lengthMS = (int) ((apeFileInfo.totalBlocks * 1000L) / apeFileInfo.sampleRate);
        apeFileInfo.averageBitrate = (apeFileInfo.lengthMS <= 0) ? 0 : (int) ((apeFileInfo.apeTotalBytes * 8L) / apeFileInfo.lengthMS);
        apeFileInfo.decompressedBitrate = (apeFileInfo.blockAlign * apeFileInfo.sampleRate * 8) / 1000;
        apeFileInfo.seekTableElements = (int) (apeFileInfo.apeDescriptor.seekTableBytes / 4);
        apeFileInfo.peakLevel = -1;

        // get the seek tables (really no reason to get the whole thing if there's extra)
        apeFileInfo.seekByteTable = new int[apeFileInfo.seekTableElements];
        for (int i = 0; i < apeFileInfo.seekTableElements; i++)
            apeFileInfo.seekByteTable[i] = io.readIntBack();

        // get the wave header
        if ((apeHeader.formatFlags & MAC_FORMAT_FLAG_CREATE_WAV_HEADER) <= 0) {
            if (apeFileInfo.wavHeaderBytes > Integer.MAX_VALUE)
                throw new JMACException("The HeaderBytes Parameter Is Too Big");
            apeFileInfo.waveHeaderData = new byte[apeFileInfo.wavHeaderBytes];
            try {
                io.readFully(apeFileInfo.waveHeaderData);
            } catch (EOFException e) {
                throw new JMACException("Can't Read Wave Header Data");
            }
        }
    }

    protected void AnalyzeOld(APEFileInfo apeFileInfo) throws IOException {
        APEHeaderOld header = APEHeaderOld.read(io);

        // fail on 0 length APE files (catches non-finalized APE files)
        if (header.totalFrames == 0)
            throw new JMACException("Unsupported Format");

        int peakLevel = -1;
        if ((header.formatFlags & MAC_FORMAT_FLAG_HAS_PEAK_LEVEL) > 0)
            peakLevel = io.readIntBack();

        if ((header.formatFlags & MAC_FORMAT_FLAG_HAS_SEEK_ELEMENTS) > 0)
            apeFileInfo.seekTableElements = io.readIntBack();
        else
            apeFileInfo.seekTableElements = (int) header.totalFrames;

        // fill the APE info structure
        apeFileInfo.version = header.version;
        apeFileInfo.compressionLevel = header.compressionLevel;
        apeFileInfo.formatFlags = header.formatFlags;
        apeFileInfo.totalFrames = (int) header.totalFrames;
        apeFileInfo.finalFrameBlocks = (int) header.finalFrameBlocks;
        apeFileInfo.blocksPerFrame = ((header.version >= 3900) || ((header.version >= 3800) && (header.compressionLevel == CompressionLevel.COMPRESSION_LEVEL_EXTRA_HIGH))) ? 73728 : 9216;
        if (header.version >= 3950)
            apeFileInfo.blocksPerFrame = 73728 * 4;
        apeFileInfo.channels = header.channels;
        apeFileInfo.sampleRate = (int) header.sampleRate;
        apeFileInfo.bitsPerSample = (apeFileInfo.formatFlags & MAC_FORMAT_FLAG_8_BIT) > 0 ? 8 : ((apeFileInfo.formatFlags & MAC_FORMAT_FLAG_24_BIT) > 0 ? 24 : 16);
        apeFileInfo.bytesPerSample = apeFileInfo.bitsPerSample / 8;
        apeFileInfo.blockAlign = apeFileInfo.bytesPerSample * apeFileInfo.channels;
        apeFileInfo.totalBlocks = (int) ((header.totalFrames == 0) ? 0 : ((header.totalFrames - 1) * apeFileInfo.blocksPerFrame) + header.finalFrameBlocks);
        apeFileInfo.wavHeaderBytes = (int) ((header.formatFlags & MAC_FORMAT_FLAG_CREATE_WAV_HEADER) > 0 ? WaveHeader.WAVE_HEADER_BYTES : header.headerBytes);
        apeFileInfo.wavTerminatingBytes = (int) header.terminatingBytes;
        apeFileInfo.wavDataBytes = apeFileInfo.totalBlocks * apeFileInfo.blockAlign;
        apeFileInfo.wavTotalBytes = apeFileInfo.wavDataBytes + apeFileInfo.wavHeaderBytes + apeFileInfo.wavTerminatingBytes;
        apeFileInfo.apeTotalBytes = io.isLocal() ? (int) io.length() : -1;
        apeFileInfo.lengthMS = (int) ((apeFileInfo.totalBlocks * 1000L) / apeFileInfo.sampleRate);
        apeFileInfo.averageBitrate = (int) ((apeFileInfo.lengthMS <= 0) ? 0 : ((apeFileInfo.apeTotalBytes * 8L) / apeFileInfo.lengthMS));
        apeFileInfo.decompressedBitrate = (apeFileInfo.blockAlign * apeFileInfo.sampleRate * 8) / 1000;
        apeFileInfo.peakLevel = peakLevel;

        // get the wave header
        if ((header.formatFlags & MAC_FORMAT_FLAG_CREATE_WAV_HEADER) <= 0) {
            if (header.headerBytes > Integer.MAX_VALUE)
                throw new JMACException("The HeaderBytes Parameter Is Too Big");
            apeFileInfo.waveHeaderData = new byte[(int) header.headerBytes];
            try {
                io.readFully(apeFileInfo.waveHeaderData);
            } catch (EOFException e) {
                throw new JMACException("Can't Read Wave Header Data");
            }
        }

        // get the seek tables (really no reason to get the whole thing if there's extra)
        apeFileInfo.seekByteTable = new int[apeFileInfo.seekTableElements];
        for (int i = 0; i < apeFileInfo.seekTableElements; i++)
            apeFileInfo.seekByteTable[i] = io.readIntBack();

        if (header.version <= 3800) {
            apeFileInfo.seekBitTable = new byte[apeFileInfo.seekTableElements];
            try {
                io.readFully(apeFileInfo.seekBitTable);
            } catch (EOFException e) {
                throw new JMACException("Can't Read Seek Bit Table");
            }
        }
    }

    protected int findDescriptor(boolean seek) throws IOException {
        int junkBytes = 0;

        // We need to limit this method if io is represented as URL
        // We'll not support ID3 tags for such files
        if (io.isLocal()) {

            // figure the extra header bytes
            io.mark(1000);

            // skip an ID3v2 tag (which we really don't support anyway...)
            ByteArrayReader reader = new ByteArrayReader(10);
            reader.reset(io, 10);
            String tag = reader.readString(3, "US-ASCII");
            if (tag.equals("ID3")) {
                // why is it so hard to figure the lenght of an ID3v2 tag ?!?
                reader.readByte();
                reader.readByte();
                int byte5 = reader.readUnsignedByte();

                int syncSafeLength;
                syncSafeLength = (reader.readUnsignedByte() & 127) << 21;
                syncSafeLength += (reader.readUnsignedByte() & 127) << 14;
                syncSafeLength += (reader.readUnsignedByte() & 127) << 7;
                syncSafeLength += (reader.readUnsignedByte() & 127);

                boolean hasTagFooter = false;

                if ((byte5 & 16) > 0) {
                    hasTagFooter = true;
                    junkBytes = syncSafeLength + 20;
                } else {
                    junkBytes = syncSafeLength + 10;
                }

                // error check
                if ((byte5 & 64) > 0) {
                    // this ID3v2 length calculator algorithm can't cope with extended headers
                    // we should be ok though, because the scan for the MAC header below should
                    // really do the trick
                }

                io.skipBytes(junkBytes - 10);

                // scan for padding (slow and stupid, but who cares here...)
                if (!hasTagFooter) {
                    while (io.read() == 0)
                        junkBytes++;
                }
            }
            io.reset();
            io.skipBytes(junkBytes);
        }

        io.mark(1000);

        // scan until we hit the APE header, the end of the file, or 1 MB later
        int goalID = ('M' << 24) | ('A' << 16) | ('C' << 8) | (' ');
        int readID = io.readInt();

        // Also, lets suppose that MAC header placed in beginning of file in case of external source of file
        if (io.isLocal()) {
            int scanBytes = 0;
            while (goalID != readID && scanBytes < (1024 * 1024)) {
                readID = (readID << 8) | io.readByte();
                junkBytes++;
                scanBytes++;
            }
        }

        if (goalID != readID)
            junkBytes = -1;

        // seek to the proper place (depending on result and settings)
        if (seek && (junkBytes != -1)) {
            // successfully found the start of the file (seek to it and return)
            io.reset();
            io.skipBytes(junkBytes);
            io.mark(1000);
        } else {
            // restore the original file pointer
            io.reset();
        }

        return junkBytes;
    }

    protected final File io;
}
