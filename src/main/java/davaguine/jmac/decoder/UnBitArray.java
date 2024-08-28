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

import davaguine.jmac.tools.File;


/**
 * @author Dmitry Vaguine
 * @version 04.03.2004 14:51:31
 */
public class UnBitArray extends UnBitArrayBase {

    private final static long[] RANGE_TOTAL_1 = {
            0, 14824, 28224, 39348, 47855, 53994, 58171, 60926,
            62682, 63786, 64463, 64878, 65126, 65276, 65365, 65419,
            65450, 65469, 65480, 65487, 65491, 65493, 65494, 65495,
            65496, 65497, 65498, 65499, 65500, 65501, 65502, 65503,
            65504, 65505, 65506, 65507, 65508, 65509, 65510, 65511,
            65512, 65513, 65514, 65515, 65516, 65517, 65518, 65519,
            65520, 65521, 65522, 65523, 65524, 65525, 65526, 65527,
            65528, 65529, 65530, 65531, 65532, 65533, 65534, 65535,
            65536
    };
    private final static long[] RANGE_WIDTH_1 = {
            14824, 13400, 11124, 8507, 6139, 4177, 2755, 1756,
            1104, 677, 415, 248, 150, 89, 54, 31,
            19, 11, 7, 4, 2, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1
    };

    private final static long[] RANGE_TOTAL_2 = {
            0, 19578, 36160, 48417, 56323, 60899, 63265, 64435,
            64971, 65232, 65351, 65416, 65447, 65466, 65476, 65482,
            65485, 65488, 65490, 65491, 65492, 65493, 65494, 65495,
            65496, 65497, 65498, 65499, 65500, 65501, 65502, 65503,
            65504, 65505, 65506, 65507, 65508, 65509, 65510, 65511,
            65512, 65513, 65514, 65515, 65516, 65517, 65518, 65519,
            65520, 65521, 65522, 65523, 65524, 65525, 65526, 65527,
            65528, 65529, 65530, 65531, 65532, 65533, 65534, 65535,
            65536
    };
    private final static long[] RANGE_WIDTH_2 = {
            19578, 16582, 12257, 7906, 4576, 2366, 1170, 536,
            261, 119, 65, 31, 19, 10, 6, 3,
            3, 2, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1,
    };

    private final static long[] K_SUM_MIN_BOUNDARY = {
            0, 32, 64, 128, 256, 512, 1024, 2048,
            4096, 8192, 16384, 32768, 65536, 131072, 262144, 524288,
            1048576, 2097152, 4194304, 8388608, 16777216, 33554432, 67108864, 134217728,
            268435456, 536870912, 1073741824, 2147483648L, 0, 0, 0, 0
    };

    private final static long CODE_BITS = 32;
    private final static long TOP_VALUE = ((long) 1 << (CODE_BITS - 1));
    private final static long EXTRA_BITS = ((CODE_BITS - 2) % 8 + 1);
    private final static long BOTTOM_VALUE = (TOP_VALUE >> 8);
    private final static int RANGE_OVERFLOW_SHIFT = 16;

    private final static int MODEL_ELEMENTS = 64;

    // construction/destruction
    public UnBitArray(File io, int version) {
        createHelper(io, 16384, version);
    }

    @Override
    public long decodeValue(int decodeMethod, int param1, int param2) throws IOException {
        if (decodeMethod == DecodeValueMethod.DECODE_VALUE_METHOD_UNSIGNED_INT)
            return decodeValueXBits(32);

        return 0;
    }

    @Override
    public void generateArray(int[] outputArray, int elements) throws IOException {
        generateArray(outputArray, elements, -1);
    }

    @Override
    public void generateArray(int[] outputArray, int elements, int bytesRequired) throws IOException {
        generateArrayRange(outputArray, elements);
    }

    @Override
    public int decodeValueRange(UnBitArrayState bitArrayState) throws IOException {
        // make sure there is room for the data
        // this is a little slower than ensuring a huge block to start with, but it's safer
        if (currentBitIndex > refillBitThreshold)
            fillBitArray();

        int value = 0;

        if (version >= 3990) {
            // figure the pivot value
            int pivotValue = Math.max(bitArrayState.kSum / 32, 1);

            // get the overflow
            int overflow = 0;
            {
                // decode
                int rangeTotal = rangeDecodeFast(RANGE_OVERFLOW_SHIFT);

                // lookup the symbol (must be a faster way than this)
                long[] al1 = RANGE_TOTAL_2;
                if (rangeTotal > 65416) {
                    int low = 12;
                    overflow = 64;
                    int mid = 38;
                    long midVal = al1[38];
                    do {
                        if (midVal < rangeTotal)
                            low = mid + 1;
                        else if (midVal > rangeTotal)
                            overflow = mid - 1;
                        else {
                            overflow = mid;
                            break;
                        }
                        mid = (low + overflow) >> 1;
                        midVal = al1[mid];
                    } while (low <= overflow);
                } else {
                    overflow = 1;
                    while (rangeTotal >= al1[overflow])
                        overflow++;
                    overflow--;
                }

                // update
                RangeCoderStructDecompress range = rangeCoderInfo;
                range.low -= range.range * al1[overflow];
                range.range *= RANGE_WIDTH_2[overflow];

                // get the working k
                if (overflow == (MODEL_ELEMENTS - 1)) {
                    overflow = rangeDecodeFastWithUpdate(16);
                    overflow <<= 16;
                    overflow |= rangeDecodeFastWithUpdate(16);
                }
            }

            // get the value
            int base = 0;
            {
                if (pivotValue >= (1 << 16)) {
                    int pivotValueBits = 0;
                    while ((pivotValue >> pivotValueBits) > 0) {
                        pivotValueBits++;
                    }
                    int splitFactor = 1 << (pivotValueBits - 16);

                    int pivotValueA = (pivotValue / splitFactor) + 1;
                    int pivotValueB = splitFactor;

                    RangeCoderStructDecompress range = rangeCoderInfo;
                    while (range.range <= BOTTOM_VALUE) {
                        range.buffer = (range.buffer << 8) | ((bitArray[(int) (currentBitIndex >> 5)] >> (24 - (currentBitIndex & 31))) & 0xFF);
                        currentBitIndex += 8;
                        range.low = (range.low << 8) | ((range.buffer >> 1) & 0xFF);
                        range.range <<= 8;
                    }
                    range.range = range.range / pivotValueA;
                    int baseA = (int) (range.low / range.range);
                    range.low -= range.range * baseA;

                    while (range.range <= BOTTOM_VALUE) {
                        range.buffer = (range.buffer << 8) | ((bitArray[(int) (currentBitIndex >> 5)] >> (24 - (currentBitIndex & 31))) & 0xFF);
                        currentBitIndex += 8;
                        range.low = (range.low << 8) | ((range.buffer >> 1) & 0xFF);
                        range.range <<= 8;
                    }
                    range.range = range.range / pivotValueB;
                    int baseB = (int) (range.low / range.range);
                    range.low -= range.range * baseB;

                    base = baseA * splitFactor + baseB;
                } else {
                    RangeCoderStructDecompress range = rangeCoderInfo;
                    while (range.range <= BOTTOM_VALUE) {
                        range.buffer = (range.buffer << 8) | ((bitArray[(int) (currentBitIndex >> 5)] >> (24 - (currentBitIndex & 31))) & 0xFF);
                        currentBitIndex += 8;
                        range.low = (range.low << 8) | ((range.buffer >> 1) & 0xFF);
                        range.range <<= 8;
                    }

                    // decode
                    range.range = range.range / pivotValue;
                    int baseLower = (int) (range.low / range.range);
                    range.low -= range.range * baseLower;

                    base = baseLower;
                }
            }

            // build the value
            value = base + (overflow * pivotValue);
        } else {
            // decode
            int rangeTotal = rangeDecodeFast(RANGE_OVERFLOW_SHIFT);

            // lookup the symbol (must be a faster way than this)
            long[] al1 = RANGE_TOTAL_1;
            int overflow;
            if (rangeTotal > 64878) {
                int low = 12;
                overflow = 64;
                int mid = 38;
                long midVal = al1[38];
                do {
                    if (midVal < rangeTotal)
                        low = mid + 1;
                    else if (midVal > rangeTotal)
                        overflow = mid - 1;
                    else {
                        overflow = mid;
                        break;
                    }
                    mid = (low + overflow) >> 1;
                    midVal = al1[mid];
                } while (low <= overflow);
            } else {
                overflow = 1;
                while (rangeTotal >= al1[overflow])
                    overflow++;
                overflow--;
            }

            // update
            RangeCoderStructDecompress range = rangeCoderInfo;
            range.low -= range.range * al1[overflow];
            range.range *= RANGE_WIDTH_1[overflow];

            // get the working k
            int tempK;
            if (overflow == (MODEL_ELEMENTS - 1)) {
                tempK = rangeDecodeFastWithUpdate(5);
                overflow = 0;
            } else
                tempK = (bitArrayState.k < 1) ? 0 : bitArrayState.k - 1;

            // figure the extra bits on the left and the left value
            if (tempK <= 16 || version < 3910)
                value = rangeDecodeFastWithUpdate(tempK);
            else {
                int x1 = rangeDecodeFastWithUpdate(16);
                int x2 = rangeDecodeFastWithUpdate(tempK - 16);
                value = x1 | (x2 << 16);
            }

            // build the value and output it
            value += (overflow << tempK);
        }

        // update kSum
        bitArrayState.kSum += ((value + 1) / 2) - ((bitArrayState.kSum + 16) >> 5);

        // update k
        if (bitArrayState.kSum < K_SUM_MIN_BOUNDARY[bitArrayState.k])
            bitArrayState.k--;
        else if (bitArrayState.kSum >= K_SUM_MIN_BOUNDARY[bitArrayState.k + 1])
            bitArrayState.k++;

        // output the value (converted to signed)
        return (value & 1) > 0 ? (value >> 1) + 1 : -(value >> 1);
    }

    @Override
    public void flushState(UnBitArrayState bitArrayState) {
        bitArrayState.k = 10;
        bitArrayState.kSum = (1 << bitArrayState.k) * 16;
    }

    @Override
    public void flushBitArray() {
        advanceToByteBoundary();
        RangeCoderStructDecompress struct = rangeCoderInfo;
        currentBitIndex += 8; // ignore the first byte... (slows compression too much to not output this dummy byte)
        struct.buffer = GetC();
        struct.low = struct.buffer >> (8 - EXTRA_BITS);
        struct.range = (long) 1 << EXTRA_BITS;

        refillBitThreshold = (bits - 512);
    }

    @Override
    public void finalize_() {
        RangeCoderStructDecompress struct = rangeCoderInfo;
        long i = currentBitIndex;
        long range = struct.range;
        // normalize
        while (range <= BOTTOM_VALUE) {
            i += 8;
            range <<= 8;
        }

        // used to back-pedal the last two bytes out
        // this should never have been a problem because we've outputted and normalized beforehand
        // but stopped doing it as of 3.96 in case it accounted for rare decompression failures
        if (version <= 3950)
            i -= 16;
        currentBitIndex = i;
        struct.range = range;
    }

    private final UnBitArrayState generateArrayRangeBitArrayState = new UnBitArrayState();

    private void generateArrayRange(int[] outputArray, int elements) throws IOException {
        flushState(generateArrayRangeBitArrayState);
        flushBitArray();

        for (int z = 0; z < elements; z++)
            outputArray[z] = decodeValueRange(generateArrayRangeBitArrayState);

        finalize_();
    }

    private final RangeCoderStructDecompress rangeCoderInfo = new RangeCoderStructDecompress();

    private long refillBitThreshold;

    // functions

    private int rangeDecodeFast(int shift) {
        RangeCoderStructDecompress struct = rangeCoderInfo;
        long[] a1 = bitArray;
        long i = currentBitIndex;
        long buffer = struct.buffer;
        long low = struct.low;
        long range = struct.range;
        while (range <= BOTTOM_VALUE) {
            buffer = (buffer << 8) | ((a1[(int) (i >> 5)] >> (24 - (i & 31))) & 0xFF);
            i += 8;
            low = (low << 8) | ((buffer >> 1) & 0xFF);
            range <<= 8;
        }
        currentBitIndex = i;
        struct.low = low;
        struct.buffer = buffer;

        // decode
        range >>= shift;
        struct.range = range;
        return (int) (low / range);
    }

    private int rangeDecodeFastWithUpdate(int shift) {
        RangeCoderStructDecompress struct = rangeCoderInfo;
        long[] a1 = bitArray;
        long i = currentBitIndex;
        long buffer = struct.buffer;
        long low = struct.low;
        long range = struct.range;
        while (range <= BOTTOM_VALUE) {
            buffer = (buffer << 8L) | ((a1[(int) (i >> 5)] >> (24 - (i & 31))) & 0xFF);
            i += 8;
            low = (low << 8) | ((buffer >> 1) & 0xFF);
            range <<= 8;
        }
        currentBitIndex = i;

        // decode
        range >>= shift;
        int retVal = (int) (low / range);
        low -= range * retVal;
        struct.range = range;
        struct.low = low;
        struct.buffer = buffer;
        return retVal;
    }

    private short GetC() {
        long l = currentBitIndex;
        short value = (short) (bitArray[(int) (l >> 5)] >> (24 - (l & 31)));
        currentBitIndex = l + 8;
        return value;
    }
}
