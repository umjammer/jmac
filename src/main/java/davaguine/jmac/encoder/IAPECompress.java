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

package davaguine.jmac.encoder;

import java.io.IOException;

import davaguine.jmac.info.CompressionLevel;
import davaguine.jmac.info.InputSource;
import davaguine.jmac.info.WaveFormat;
import davaguine.jmac.tools.ByteBuffer;
import davaguine.jmac.tools.File;
import davaguine.jmac.tools.IntegerPointer;


/**
 * @author Dmitry Vaguine
 * @version 07.05.2004 13:10:46
 */
public abstract class IAPECompress {

    public final static int CREATE_WAV_HEADER_ON_DECOMPRESSION = -1;
    public final static int MAX_AUDIO_BYTES_UNKNOWN = -1;

    //
    // Start
    //

    /**
     * starts encoding
     *
     * @param outputFilename the output... either a filename or an I/O source
     * @param wfeInput       format of the audio to encode (use FillWaveFormatEx() if necessary)
     */
    public void start(String outputFilename, WaveFormat wfeInput) throws IOException {
        start(outputFilename, wfeInput, MAX_AUDIO_BYTES_UNKNOWN, CompressionLevel.COMPRESSION_LEVEL_NORMAL, null, CREATE_WAV_HEADER_ON_DECOMPRESSION);
    }

    /**
     * starts encoding
     *
     * @param outputFilename the output... either a filename or an I/O source
     * @param wfeInput       format of the audio to encode (use FillWaveFormatEx() if necessary)
     * @param maxAudioBytes  the absolute maximum audio bytes that will be encoded... encoding fails with a
     *                        ERROR_APE_COMPRESS_TOO_MUCH_DATA if you attempt to encode more than specified here
     *                        (if unknown, use MAX_AUDIO_BYTES_UNKNOWN to allocate as much storage in the seek table as
     *                        possible... limit is then 2 GB of data (~4 hours of CD music)... this wastes around
     *                        30kb, so only do it if completely necessary)
     */
    public void start(String outputFilename, WaveFormat wfeInput, int maxAudioBytes) throws IOException {
        start(outputFilename, wfeInput, maxAudioBytes, CompressionLevel.COMPRESSION_LEVEL_NORMAL, null, CREATE_WAV_HEADER_ON_DECOMPRESSION);
    }

    /**
     * starts encoding
     *
     * @param outputFilename   the output... either a filename or an I/O source
     * @param wfeInput         format of the audio to encode (use FillWaveFormatEx() if necessary)
     * @param maxAudioBytes    the absolute maximum audio bytes that will be encoded... encoding fails with a
     *                          ERROR_APE_COMPRESS_TOO_MUCH_DATA if you attempt to encode more than specified here
     *                          (if unknown, use MAX_AUDIO_BYTES_UNKNOWN to allocate as much storage in the seek table as
     *                          possible... limit is then 2 GB of data (~4 hours of CD music)... this wastes around
     *                          30kb, so only do it if completely necessary)
     * @param compressionLevel the compression level for the APE file (fast - extra high)
     *                          (note: extra-high is much slower for little gain)
     */
    public void start(String outputFilename, WaveFormat wfeInput, int maxAudioBytes, int compressionLevel) throws IOException {
        start(outputFilename, wfeInput, maxAudioBytes, compressionLevel, null, CREATE_WAV_HEADER_ON_DECOMPRESSION);
    }

    /**
     * starts encoding
     *
     * @param outputFilename   the output... either a filename or an I/O source
     * @param wfeInput         format of the audio to encode (use FillWaveFormatEx() if necessary)
     * @param maxAudioBytes    the absolute maximum audio bytes that will be encoded... encoding fails with a
     *                          ERROR_APE_COMPRESS_TOO_MUCH_DATA if you attempt to encode more than specified here
     *                          (if unknown, use MAX_AUDIO_BYTES_UNKNOWN to allocate as much storage in the seek table as
     *                          possible... limit is then 2 GB of data (~4 hours of CD music)... this wastes around
     *                          30kb, so only do it if completely necessary)
     * @param compressionLevel the compression level for the APE file (fast - extra high)
     *                          (note: extra-high is much slower for little gain)
     * @param headerData       a pointer to a buffer containing the WAV header (data before the data block in the WAV)
     *                          (note: use NULL for on-the-fly encoding... see next parameter)
     */
    public void start(String outputFilename, WaveFormat wfeInput, int maxAudioBytes, int compressionLevel, byte[] headerData) throws IOException {
        start(outputFilename, wfeInput, maxAudioBytes, compressionLevel, headerData, CREATE_WAV_HEADER_ON_DECOMPRESSION);
    }

    /**
     * starts encoding
     *
     * @param outputFilename   the output... either a filename or an I/O source
     * @param headerBytes      number of bytes in the header data buffer (use CREATE_WAV_HEADER_ON_DECOMPRESSION and
     *                          NULL for the headerData and MAC will automatically create the appropriate WAV header
     *                          on decompression)
     * @param wfeInput         format of the audio to encode (use FillWaveFormatEx() if necessary)
     * @param maxAudioBytes    the absolute maximum audio bytes that will be encoded... encoding fails with a
     *                          ERROR_APE_COMPRESS_TOO_MUCH_DATA if you attempt to encode more than specified here
     *                          (if unknown, use MAX_AUDIO_BYTES_UNKNOWN to allocate as much storage in the seek table as
     *                          possible... limit is then 2 GB of data (~4 hours of CD music)... this wastes around
     *                          30kb, so only do it if completely necessary)
     * @param compressionLevel the compression level for the APE file (fast - extra high)
     *                          (note: extra-high is much slower for little gain)
     * @param headerData       a pointer to a buffer containing the WAV header (data before the data block in the WAV)
     *                          (note: use NULL for on-the-fly encoding... see next parameter)
     */
    public abstract void start(String outputFilename, WaveFormat wfeInput, int maxAudioBytes, int compressionLevel, byte[] headerData, int headerBytes) throws IOException;

    public void startEx(File output, WaveFormat wfeInput) throws IOException {
        startEx(output, wfeInput, MAX_AUDIO_BYTES_UNKNOWN, CompressionLevel.COMPRESSION_LEVEL_NORMAL, null, CREATE_WAV_HEADER_ON_DECOMPRESSION);
    }

    public void startEx(File output, WaveFormat wfeInput, int maxAudioBytes) throws IOException {
        startEx(output, wfeInput, maxAudioBytes, CompressionLevel.COMPRESSION_LEVEL_NORMAL, null, CREATE_WAV_HEADER_ON_DECOMPRESSION);
    }

    public void startEx(File output, WaveFormat wfeInput, int maxAudioBytes, int compressionLevel) throws IOException {
        startEx(output, wfeInput, maxAudioBytes, compressionLevel, null, CREATE_WAV_HEADER_ON_DECOMPRESSION);
    }

    public void startEx(File output, WaveFormat wfeInput, int maxAudioBytes, int compressionLevel, byte[] headerData) throws IOException {
        startEx(output, wfeInput, maxAudioBytes, compressionLevel, headerData, CREATE_WAV_HEADER_ON_DECOMPRESSION);
    }

    public abstract void startEx(File output, WaveFormat wfeInput, int maxAudioBytes, int compressionLevel, byte[] headerData, int headerBytes) throws IOException;

    /*
     * Add / Compress Data
     * - there are 3 ways to add data:
     * 1) simple call AddData(...)
     * 2) lock MAC's buffer, copy into it, and unlock (LockBuffer(...) / UnlockBuffer(...))
     * 3) from an I/O source (AddDataFromInputSource(...))
     */

    /**
     * Adds data to the encoder
     *
     * @param data  a pointer to a buffer containing the raw audio data
     * @param bytes the number of bytes in the buffer
     */
    public abstract void addData(byte[] data, int bytes) throws IOException;

    /**
     * Returns the number of bytes available in the buffer
     * (helpful when locking)
     */
    public abstract int getBufferBytesAvailable();

    /**
     * Locks MAC's buffer so we can copy into it
     *
     * @param bytesAvailable returns the number of bytes available in the buffer (DO NOT COPY MORE THAN THIS IN)
     * @return pointer to the buffer (add at that location)
     */
    public abstract ByteBuffer lockBuffer(IntegerPointer bytesAvailable);

    /**
     * Releases the buffer
     *
     * @param bytesAdded the number of bytes copied into the buffer
     */
    public void unlockBuffer(int bytesAdded) throws IOException {
        unlockBuffer(bytesAdded, true);
    }

    /**
     * Releases the buffer
     *
     * @param bytesAdded the number of bytes copied into the buffer
     * @param process    whether MAC should process as much as possible of the buffer
     */
    public abstract void unlockBuffer(int bytesAdded, boolean process) throws IOException;

    /**
     * Uses a CInputSource (input source) to add data
     *
     * @param inputSource a pointer to the input source
     */
    public int addDataFromInputSource(InputSource inputSource) throws IOException {
        return addDataFromInputSource(inputSource, -1);
    }

    /**
     * Uses a CInputSource (input source) to add data
     *
     * @param inputSource a pointer to the input source
     * @param maxBytes    the maximum number of bytes to let MAC add (-1 if MAC can add any amount)
     */
    public abstract int addDataFromInputSource(InputSource inputSource, int maxBytes) throws IOException;

    //
    // Finish / Kill
    //

    /**
     * Ends encoding and finalizes the file
     *
     * @param terminatingData     a pointer to a buffer containing the information to place at the end of the APE file
     *                             (comprised of the WAV terminating data (data after the data block in the WAV) followed
     *                             by any tag information)
     * @param terminatingBytes    number of bytes in the terminating data buffer
     * @param wavTerminatingBytes the number of bytes of the terminating data buffer that should be appended to a decoded
     *                             WAV file (it's basically terminatingBytes - the bytes that make up the tag)
     */
    public abstract void finish(byte[] terminatingData, int terminatingBytes, int wavTerminatingBytes) throws IOException;

    /**
     * Stops encoding and deletes the output file
     * <p>
     * --- NOT CURRENTLY IMPLEMENTED ---
     */
    public abstract void kill();

    public static IAPECompress createIAPECompress() {
        return new APECompress();
    }
}
