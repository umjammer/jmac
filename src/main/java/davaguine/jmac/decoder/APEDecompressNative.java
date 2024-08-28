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

package davaguine.jmac.decoder;

import java.io.IOException;

import davaguine.jmac.info.APEInfo;
import davaguine.jmac.tools.File;
import davaguine.jmac.tools.JMACException;


/**
 * @author Dmitry Vaguine
 * @version 04.03.2004 14:51:31
 */
public class APEDecompressNative extends APEDecompress {

    private final int id;
    private final int realFrame_;

    static {
        System.loadLibrary("jmac");
    }

    public APEDecompressNative(APEInfo apeInfo) {
        this(apeInfo, -1, -1);
    }

    public APEDecompressNative(APEInfo apeInfo, int startBlock) {
        this(apeInfo, startBlock, -1);
    }

    public APEDecompressNative(APEInfo apeInfo, int startBlock, int finishBlock) {
        super(apeInfo, startBlock, finishBlock);
        id = registerDecoder(
                this.getApeInfoIoSource(),
                this.getApeInfoFileVersion(),
                this.getApeInfoCompressionLevel(),
                startBlock,
                finishBlock,
                this.getApeInfoTotalBlocks(),
                this.getApeInfoBlockAlign(),
                this.getApeInfoBlocksPerFrame(),
                this.getApeInfoSampleRate(),
                this.getApeInfoBitsPerSample(),
                this.getApeInfoChannels());
        if (id < 0)
            throw new JMACException("The Native APE Decoder Can't Be Instantiated");
        realFrame_ = 0;
    }

    @Override
    protected void finalize() {
        finalize(id, this.getApeInfoIoSource());
    }

    @Override
    public int getData(byte[] buffer, int blocks) throws IOException {
        int blocksRetrieved = getData(id, this.getApeInfoIoSource(), buffer, blocks);
        currentBlock += blocksRetrieved;
        return blocksRetrieved;
    }

    @Override
    public void seek(int blockOffset) throws IOException {
        seek(id, this.getApeInfoIoSource(), blockOffset);
    }

    private native int registerDecoder(File io, int version, int compressionLevel, int startBlock, int finishBlock,
                                       int totalBlocks, int blockAlign, int blocksPerFrame, int sampleRate,
                                       int bitsPerSample, int channels);

    private native void finalize(int id, File io);

    private native int getData(int id, File io, byte[] buffer, int blocks);

    private native void seek(int id, File io, int blockOffset) throws IOException;
}
