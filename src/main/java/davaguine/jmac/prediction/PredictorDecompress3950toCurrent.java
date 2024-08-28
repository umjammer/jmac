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
import davaguine.jmac.tools.RollBufferFastInt;


/**
 * @author Dmitry Vaguine
 * @version 04.03.2004 14:51:31
 */
public class PredictorDecompress3950toCurrent extends IPredictorDecompress {

    public final static int M_COUNT = 8;
    private final static int WINDOW_BLOCKS = 512;

    public PredictorDecompress3950toCurrent(int compressionLevel, int version) {
        super(compressionLevel, version);
        this.version = version;

        if (compressionLevel == CompressionLevel.COMPRESSION_LEVEL_FAST) {
            nnFilter = null;
            nnFilter1 = null;
            nnFilter2 = null;
        } else if (compressionLevel == CompressionLevel.COMPRESSION_LEVEL_NORMAL) {
            nnFilter = new NNFilter16(11, version);
            nnFilter1 = null;
            nnFilter2 = null;
        } else if (compressionLevel == CompressionLevel.COMPRESSION_LEVEL_HIGH) {
            nnFilter = new NNFilter64(11, version);
            nnFilter1 = null;
            nnFilter2 = null;
        } else if (compressionLevel == CompressionLevel.COMPRESSION_LEVEL_EXTRA_HIGH) {
            nnFilter = new NNFilter256(13, version);
            nnFilter1 = new NNFilter32(10, version);
            nnFilter2 = null;
        } else if (compressionLevel == CompressionLevel.COMPRESSION_LEVEL_INSANE) {
            nnFilter = new NNFilter1280(15, version);
            nnFilter1 = new NNFilter256(13, version);
            nnFilter2 = new NNFilter16(11, version);
        } else {
            throw new JMACException("Unknown Compression Type");
        }
    }

    @Override
    public int decompressValue(int a) {
        return decompressValue(a, 0);
    }

    @Override
    public int decompressValue(int a, int b) {
        if (currentIndex == WINDOW_BLOCKS) {
            // copy forward and adjust pointers
            predictionA.roll();
            predictionB.roll();
            adaptA.roll();
            adaptB.roll();

            currentIndex = 0;
        }

        // stage 2: NNFilter
        Object obj;
        if ((obj = nnFilter2) != null)
            a = ((NNFilter) (obj)).decompress(a);
        if ((obj = nnFilter1) != null)
            a = ((NNFilter) (obj)).decompress(a);
        if ((obj = nnFilter) != null)
            a = ((NNFilter) (obj)).decompress(a);

        // stage 1: multiple predictors (order 2 and offset 1)
        int indexA = ((RollBufferFastInt) (obj = predictionA)).index;
        RollBufferFastInt predictB;
        int indexB = (predictB = predictionB).index;
        int[] ai;
        int l;
        (ai = ((RollBufferFastInt) (obj)).data)[indexA] = l = lastValueA;
        int l1 = indexA - 1;
        ai[l1] = l - ai[l1];

        int[] ai3 = predictB.data;

        ai3[indexB] = b - ((scaledFilterBLV * 31) >> 5);
        scaledFilterBLV = b;

//        ai3[indexB] = stage1FilterB.compress(nB);
        int k2 = indexB - 1;
        ai3[k2] = ai3[indexB] - ai3[k2];

        int[] ai2;
        int[] ai4;
        int predictionA = (l * (ai2 = ma)[0]) + (ai[l1] * ai2[1]) + (ai[indexA - 2] * ai2[2]) + (ai[indexA - 3] * ai2[3]);
        int predictionB = (ai3[indexB] * (ai4 = mb)[0]) + (ai3[k2] * ai4[1]) + (ai3[indexB - 2] * ai4[2]) + (ai3[indexB - 3] * ai4[3]) + (ai3[indexB - 4] * ai4[4]);

        int currentA = a + ((predictionA + (predictionB >> 1)) >> 10);

        RollBufferFastInt adaptA;
        RollBufferFastInt adaptB;
        int indexAA = (adaptA = this.adaptA).index;
        int indexAB = (adaptB = this.adaptB).index;
        int[] ai1;
        (ai1 = this.adaptA.data)[indexAA] = (l != 0) ? ((l >> 30) & 2) - 1 : 0;
        ai1[indexAA - 1] = (ai[l1] != 0) ? ((ai[l1] >> 30) & 2) - 1 : 0;

        int[] ai5;
        (ai5 = this.adaptB.data)[indexAB] = (ai3[indexB] != 0) ? ((ai3[indexB] >> 30) & 2) - 1 : 0;
        ai5[indexAB - 1] = (ai3[k2] != 0) ? ((ai3[k2] >> 30) & 2) - 1 : 0;

        if (a > 0) {
            ai2[0] -= ai1[indexAA];
            ai2[1] -= ai1[indexAA - 1];
            ai2[2] -= ai1[indexAA - 2];
            ai2[3] -= ai1[indexAA - 3];

            ai4[0] -= ai5[indexAB];
            ai4[1] -= ai5[indexAB - 1];
            ai4[2] -= ai5[indexAB - 2];
            ai4[3] -= ai5[indexAB - 3];
            ai4[4] -= ai5[indexAB - 4];
        } else if (a < 0) {
            ai2[0] += ai1[indexAA];
            ai2[1] += ai1[indexAA - 1];
            ai2[2] += ai1[indexAA - 2];
            ai2[3] += ai1[indexAA - 3];

            ai4[0] += ai5[indexAB];
            ai4[1] += ai5[indexAB - 1];
            ai4[2] += ai5[indexAB - 2];
            ai4[3] += ai5[indexAB - 3];
            ai4[4] += ai5[indexAB - 4];
        }

//        int retVal = stage1FilterA.decompress(currentA);
        scaledFilterALV = currentA + ((scaledFilterALV * 31) >> 5);
        lastValueA = currentA;

        ((RollBufferFastInt) (obj)).index++;
        predictB.index++;
        adaptA.index++;
        adaptB.index++;

        currentIndex++;

        return scaledFilterALV;
    }

    @Override
    public void flush() {
        NNFilter nnfilter;
        if ((nnfilter = nnFilter) != null)
            nnfilter.flush();
        if ((nnfilter = nnFilter1) != null)
            nnfilter.flush();
        if ((nnfilter = nnFilter2) != null)
            nnfilter.flush();

        Arrays.fill(ma, 0);
        Arrays.fill(mb, 0);

        predictionA.flush();
        predictionB.flush();
        adaptA.flush();
        adaptB.flush();

        int[] ai;
        (ai = ma)[0] = 360;
        ai[1] = 317;
        ai[2] = -109;
        ai[3] = 98;

//        stage1FilterA.flush();
        scaledFilterALV = 0;
//        stage1FilterB.flush();
        scaledFilterBLV = 0;

        lastValueA = 0;

        currentIndex = 0;
    }

    // adaption
    protected final int[] ma = new int[M_COUNT];
    protected final int[] mb = new int[M_COUNT];

    // buffer pointers
    protected final RollBufferFastInt predictionA = new RollBufferFastInt(WINDOW_BLOCKS, 8);
    protected final RollBufferFastInt predictionB = new RollBufferFastInt(WINDOW_BLOCKS, 8);

    protected final RollBufferFastInt adaptA = new RollBufferFastInt(WINDOW_BLOCKS, 8);
    protected final RollBufferFastInt adaptB = new RollBufferFastInt(WINDOW_BLOCKS, 8);

//    protected ScaledFirstOrderFilter stage1FilterA = new ScaledFirstOrderFilter(31, 5);
    protected int scaledFilterALV;
//    protected ScaledFirstOrderFilter stage1FilterB = new ScaledFirstOrderFilter(31, 5);
    protected int scaledFilterBLV;

    // other
    protected int currentIndex;
    protected int lastValueA;
    protected final int version;
    protected NNFilter nnFilter;
    protected NNFilter nnFilter1;
    protected NNFilter nnFilter2;
}
