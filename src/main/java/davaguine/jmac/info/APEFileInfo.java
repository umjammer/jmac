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


/**
 * @author Dmitry Vaguine
 * @version 04.03.2004 14:51:31
 */
public class APEFileInfo {

    /** file version number * 1000 (3.93 = 3930) */
    public int version;
    /** the compression level */
    public int compressionLevel;
    /** format flags */
    public int formatFlags;
    /** the total number frames (frames are used internally) */
    public int totalFrames;
    /** the samples in a frame (frames are used internally) */
    public int blocksPerFrame;
    /** the number of samples in the final frame */
    public int finalFrameBlocks;
    /** audio channels */
    public int channels;
    /** audio samples per second */
    public int sampleRate;
    /** audio bits per sample */
    public int bitsPerSample;
    /** audio bytes per sample */
    public int bytesPerSample;
    /** audio block align (channels * bytes per sample) */
    public int blockAlign;
    /** header bytes of the original WAV */
    public int wavHeaderBytes;
    /** data bytes of the original WAV */
    public int wavDataBytes;
    /** terminating bytes of the original WAV */
    public int wavTerminatingBytes;
    /** total bytes of the original WAV */
    public int wavTotalBytes;
    /** total bytes of the APE file */
    public int apeTotalBytes;
    /** the total number audio blocks */
    public int totalBlocks;
    /** the length in milliseconds */
    public int lengthMS;
    /** the kbps (i.e. 637 kpbs) */
    public int averageBitrate;
    /** the kbps of the decompressed audio (i.e. 1440 kpbs for CD audio) */
    public int decompressedBitrate;
    /** the peak audio level (-1 if unknown) */
    public int peakLevel;

    /** used for ID3v2, etc. */
    public int junkHeaderBytes;
    /** the number of elements in the seek table(s) */
    public int seekTableElements;

    /** the seek table (byte) */
    public int[] seekByteTable;
    /** the seek table (bits -- legacy) */
    public byte[] seekBitTable;
    /** the pre-audio header data */
    public byte[] waveHeaderData;
    /** the descriptor (only with newer files) */
    public APEDescriptor apeDescriptor;
}
