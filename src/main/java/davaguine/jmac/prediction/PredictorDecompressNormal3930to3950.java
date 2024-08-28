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

package davaguine.jmac.prediction;

import java.util.Arrays;

import davaguine.jmac.info.CompressionLevel;
import davaguine.jmac.tools.JMACException;


/**
 * @author Dmitry Vaguine
 * @version 04.03.2004 14:51:31
 */
public class PredictorDecompressNormal3930to3950 extends IPredictorDecompress {

    private final static int HISTORY_ELEMENTS = 8;
    private final static int WINDOW_BLOCKS = 512;

    private final static int BUFFER_COUNT = 1;
    private final static int M_COUNT = 8;

    public PredictorDecompressNormal3930to3950(int compressionLevel, int version) {
        super(compressionLevel, version);
        buffer[0] = new int[HISTORY_ELEMENTS + WINDOW_BLOCKS];

        if (compressionLevel == CompressionLevel.COMPRESSION_LEVEL_FAST) {
            nnFilter = null;
            nnFilter1 = null;
        } else if (compressionLevel == CompressionLevel.COMPRESSION_LEVEL_NORMAL) {
            nnFilter = new NNFilter16(11, version);
            nnFilter1 = null;
        } else if (compressionLevel == CompressionLevel.COMPRESSION_LEVEL_HIGH) {
            nnFilter = new NNFilter64(11, version);
            nnFilter1 = null;
        } else if (compressionLevel == CompressionLevel.COMPRESSION_LEVEL_EXTRA_HIGH) {
            nnFilter = new NNFilter256(13, version);
            nnFilter1 = new NNFilter32(10, version);
        } else {
            throw new JMACException("Unknown Compression Type");
        }
    }

    @Override
    public int decompressValue(int a, int b) {
        if (currentIndex == WINDOW_BLOCKS) {
            // copy forward and adjust pointers
            System.arraycopy(buffer[0], WINDOW_BLOCKS, buffer[0], 0, HISTORY_ELEMENTS);
            inputBufferI = 0;
            inputBufferJ = HISTORY_ELEMENTS;

            currentIndex = 0;
        }

        // stage 2: NNFilter
        if (nnFilter1 != null)
            a = nnFilter1.decompress(a);
        if (nnFilter != null)
            a = nnFilter.decompress(a);

        // stage 1: multiple predictors (order 2 and offset 1)

        int p1 = buffer[inputBufferI][inputBufferJ - 1];
        int p2 = buffer[inputBufferI][inputBufferJ - 1] - buffer[inputBufferI][inputBufferJ - 2];
        int p3 = buffer[inputBufferI][inputBufferJ - 2] - buffer[inputBufferI][inputBufferJ - 3];
        int p4 = buffer[inputBufferI][inputBufferJ - 3] - buffer[inputBufferI][inputBufferJ - 4];

        buffer[inputBufferI][inputBufferJ] = a + (((p1 * m[0]) + (p2 * m[1]) + (p3 * m[2]) + (p4 * m[3])) >> 9);

        if (a > 0) {
            m[0] -= ((p1 >> 30) & 2) - 1;
            m[1] -= ((p2 >> 30) & 2) - 1;
            m[2] -= ((p3 >> 30) & 2) - 1;
            m[3] -= ((p4 >> 30) & 2) - 1;
        } else if (a < 0) {
            m[0] += ((p1 >> 30) & 2) - 1;
            m[1] += ((p2 >> 30) & 2) - 1;
            m[2] += ((p3 >> 30) & 2) - 1;
            m[3] += ((p4 >> 30) & 2) - 1;
        }

        int retVal = buffer[inputBufferI][inputBufferJ] + ((lastValue * 31) >> 5);
        lastValue = retVal;

        currentIndex++;
        inputBufferJ++;

        return retVal;
    }

    @Override
    public void flush() {
        if (nnFilter != null) nnFilter.flush();
        if (nnFilter1 != null) nnFilter1.flush();

        Arrays.fill(buffer[0], 0, HISTORY_ELEMENTS, 0);
        Arrays.fill(m, 0);

        m[0] = 360;
        m[1] = 317;
        m[2] = -109;
        m[3] = 98;

        inputBufferI = 0;
        inputBufferJ = HISTORY_ELEMENTS;

        lastValue = 0;
        currentIndex = 0;
    }

    // buffer information
    protected final int[][] buffer = new int[BUFFER_COUNT][];

    // adaption
    protected final int[] m = new int[M_COUNT];

    // buffer pointers
    protected int inputBufferI;
    protected int inputBufferJ;

    // other
    protected int currentIndex;
    protected int lastValue;
    protected NNFilter nnFilter;
    protected NNFilter nnFilter1;
}
