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
import davaguine.jmac.tools.Globals;
import davaguine.jmac.tools.JMACException;
import davaguine.jmac.tools.RollBufferFastInt;
import davaguine.jmac.tools.ScaledFirstOrderFilter;


/**
 * @author Dmitry Vaguine
 * @version 02.05.2004 13:08:34
 */
public class PredictorCompressNormal extends IPredictorCompress {

    private final static int WINDOW_BLOCKS = 512;

    public PredictorCompressNormal(int compressionLevel) {
        super(compressionLevel);
        if (compressionLevel == CompressionLevel.COMPRESSION_LEVEL_FAST) {
            nnFilter = null;
            nnFilter1 = null;
            nnFilter2 = null;
        } else if (compressionLevel == CompressionLevel.COMPRESSION_LEVEL_NORMAL) {
            nnFilter = new NNFilter16(11, Globals.MAC_VERSION_NUMBER);
            nnFilter1 = null;
            nnFilter2 = null;
        } else if (compressionLevel == CompressionLevel.COMPRESSION_LEVEL_HIGH) {
            nnFilter = new NNFilter64(11, Globals.MAC_VERSION_NUMBER);
            nnFilter1 = null;
            nnFilter2 = null;
        } else if (compressionLevel == CompressionLevel.COMPRESSION_LEVEL_EXTRA_HIGH) {
            nnFilter = new NNFilter256(13, Globals.MAC_VERSION_NUMBER);
            nnFilter1 = new NNFilter32(10, Globals.MAC_VERSION_NUMBER);
            nnFilter2 = null;
        } else if (compressionLevel == CompressionLevel.COMPRESSION_LEVEL_INSANE) {
            nnFilter = new NNFilter1280(15, Globals.MAC_VERSION_NUMBER);
            nnFilter1 = new NNFilter256(13, Globals.MAC_VERSION_NUMBER);
            nnFilter2 = new NNFilter16(11, Globals.MAC_VERSION_NUMBER);
        } else {
            throw new JMACException("Unknown Compression Type");
        }
    }

    @Override
    public int compressValue(int a, int b) {
        // roll the buffers if necessary
        if (currentIndex == WINDOW_BLOCKS) {
            prediction.roll();
            adapt.roll();
            currentIndex = 0;
        }

        // stage 1: simple, non-adaptive order 1 prediction
        a = stage1FilterA.compress(a);
        b = stage1FilterB.compress(b);

        // stage 2: adaptive offset filter(s)
        int predictIndex = prediction.index;
        prediction.data[predictIndex] = a;
        prediction.data[predictIndex - 2] = prediction.data[predictIndex - 1] - prediction.data[predictIndex - 2];

        prediction.data[predictIndex - 5] = b;
        prediction.data[predictIndex - 6] = prediction.data[predictIndex - 5] - prediction.data[predictIndex - 6];

        int predictionA = (prediction.data[predictIndex - 1] * m[8]) + (prediction.data[predictIndex - 2] * m[7]) + (prediction.data[predictIndex - 3] * m[6]) + (prediction.data[predictIndex - 4] * m[5]);
        int predictionB = (prediction.data[predictIndex - 5] * m[4]) + (prediction.data[predictIndex - 6] * m[3]) + (prediction.data[predictIndex - 7] * m[2]) + (prediction.data[predictIndex - 8] * m[1]) + (prediction.data[predictIndex - 9] * m[0]);

        int output = a - ((predictionA + (predictionB >> 1)) >> 10);

        // adapt
        int adaptIndex = adapt.index;
        adapt.data[adaptIndex] = (prediction.data[predictIndex - 1] != 0) ? ((prediction.data[predictIndex - 1] >> 30) & 2) - 1 : 0;
        adapt.data[adaptIndex - 1] = (prediction.data[predictIndex - 2] != 0) ? ((prediction.data[predictIndex - 2] >> 30) & 2) - 1 : 0;
        adapt.data[adaptIndex - 4] = (prediction.data[predictIndex - 5] != 0) ? ((prediction.data[predictIndex - 5] >> 30) & 2) - 1 : 0;
        adapt.data[adaptIndex - 5] = (prediction.data[predictIndex - 6] != 0) ? ((prediction.data[predictIndex - 6] >> 30) & 2) - 1 : 0;

        if (output > 0) {
            int indexM = 0;
            int indexA = adaptIndex - 8;
            m[indexM++] -= adapt.data[indexA++];
            m[indexM++] -= adapt.data[indexA++];
            m[indexM++] -= adapt.data[indexA++];
            m[indexM++] -= adapt.data[indexA++];
            m[indexM++] -= adapt.data[indexA++];
            m[indexM++] -= adapt.data[indexA++];
            m[indexM++] -= adapt.data[indexA++];
            m[indexM++] -= adapt.data[indexA++];
            m[indexM++] -= adapt.data[indexA++];
        } else if (output < 0) {
            int indexM = 0;
            int indexA = adaptIndex - 8;
            m[indexM++] += adapt.data[indexA++];
            m[indexM++] += adapt.data[indexA++];
            m[indexM++] += adapt.data[indexA++];
            m[indexM++] += adapt.data[indexA++];
            m[indexM++] += adapt.data[indexA++];
            m[indexM++] += adapt.data[indexA++];
            m[indexM++] += adapt.data[indexA++];
            m[indexM++] += adapt.data[indexA++];
            m[indexM++] += adapt.data[indexA++];
        }

        // stage 3: NNFilters
        if (nnFilter != null) {
            output = nnFilter.compress(output);

            if (nnFilter1 != null) {
                output = nnFilter1.compress(output);

                if (nnFilter2 != null)
                    output = nnFilter2.compress(output);
            }
        }

        prediction.index++;
        adapt.index++;
        currentIndex++;

        return output;
    }

    @Override
    public void flush() {
        if (nnFilter != null) nnFilter.flush();
        if (nnFilter1 != null) nnFilter1.flush();
        if (nnFilter2 != null) nnFilter2.flush();

        prediction.flush();
        adapt.flush();
        stage1FilterA.flush();
        stage1FilterB.flush();

        Arrays.fill(m, 0);

        m[8] = 360;
        m[7] = 317;
        m[6] = -109;
        m[5] = 98;

        currentIndex = 0;
    }

    // buffer information
    protected final RollBufferFastInt prediction = new RollBufferFastInt(WINDOW_BLOCKS, 10);
    protected final RollBufferFastInt adapt = new RollBufferFastInt(WINDOW_BLOCKS, 9);

    protected final ScaledFirstOrderFilter stage1FilterA = new ScaledFirstOrderFilter(31, 5);
    protected final ScaledFirstOrderFilter stage1FilterB = new ScaledFirstOrderFilter(31, 5);

    // adaption
    protected final int[] m = new int[9];

    // other
    protected int currentIndex;
    protected NNFilter nnFilter;
    protected NNFilter nnFilter1;
    protected NNFilter nnFilter2;
}
