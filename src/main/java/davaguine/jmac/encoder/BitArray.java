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
import java.util.Arrays;

import davaguine.jmac.tools.ByteArrayWriter;
import davaguine.jmac.tools.File;
import davaguine.jmac.tools.JMACException;
import davaguine.jmac.tools.MD5;


/**
 * @author Dmitry Vaguine
 * @version 04.05.2004 16:41:39
 */
public class BitArray {

    private final static int BIT_ARRAY_ELEMENTS = 4096;  // the number of elements in the bit array (4 MB)
    private final static int BIT_ARRAY_BYTES = BIT_ARRAY_ELEMENTS * 4;  // the number of bytes in the bit array
    private final static int BIT_ARRAY_BITS = BIT_ARRAY_BYTES * 8;  // the number of bits in the bit array
    private final static int MAX_ELEMENT_BITS = 128;
    private final static int REFILL_BIT_THRESHOLD = BIT_ARRAY_BITS - MAX_ELEMENT_BITS;

    private final static long CODE_BITS = 32;
    private final static long TOP_VALUE = (((long) 1) << (CODE_BITS - 1));
    private final static long SHIFT_BITS = (CODE_BITS - 9);
    private final static long BOTTOM_VALUE = (TOP_VALUE >> 8);

    private final static long[] K_SUM_MIN_BOUNDARY = {0, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536, 131072, 262144, 524288, 1048576, 2097152, 4194304, 8388608, 16777216, 33554432, 67108864, 134217728, 268435456, 536870912, 1073741824, 2147483648L, 0, 0, 0, 0};

    private final static long[] RANGE_TOTAL = {0, 19578, 36160, 48417, 56323, 60899, 63265, 64435, 64971, 65232, 65351, 65416, 65447, 65466, 65476, 65482, 65485, 65488, 65490, 65491, 65492, 65493, 65494, 65495, 65496, 65497, 65498, 65499, 65500, 65501, 65502, 65503, 65504, 65505, 65506, 65507, 65508, 65509, 65510, 65511, 65512, 65513, 65514, 65515, 65516, 65517, 65518, 65519, 65520, 65521, 65522, 65523, 65524, 65525, 65526, 65527, 65528, 65529, 65530, 65531, 65532, 65533, 65534, 65535};
    private final static long[] RANGE_WIDTH = {19578, 16582, 12257, 7906, 4576, 2366, 1170, 536, 261, 119, 65, 31, 19, 10, 6, 3, 3, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};

    private final static int MODEL_ELEMENTS = 64;
    private final static int RANGE_OVERFLOW_SHIFT = 16;

    // construction / destruction

    public BitArray(File io) {
        // allocate memory for the bit array
        bitArray = new int[BIT_ARRAY_ELEMENTS];
        Arrays.fill(bitArray, 0);

        // initialize other variables
        currentBitIndex = 0;
        this.io = io;
    }

    @Override
    protected void finalize() {
        bitArray = null;
    }

    private void normalizeRangeCoder() {
        while (rangeCoderInfo.range <= BOTTOM_VALUE) {
            if (rangeCoderInfo.low < (0xFF << SHIFT_BITS)) {
                putc(rangeCoderInfo.buffer);
                for (; rangeCoderInfo.help > 0; rangeCoderInfo.help--) {
                    putcNocap(0xFF);
                }
                rangeCoderInfo.buffer = (short) ((rangeCoderInfo.low >> SHIFT_BITS) & 0xff);
            } else if ((rangeCoderInfo.low & TOP_VALUE) > 0) {
                putc(rangeCoderInfo.buffer + 1);
                currentBitIndex += (rangeCoderInfo.help * 8);
                rangeCoderInfo.help = 0;
                rangeCoderInfo.buffer = (short) ((rangeCoderInfo.low >> SHIFT_BITS) & 0xff);
            } else {
                rangeCoderInfo.help++;
            }

            rangeCoderInfo.low = (rangeCoderInfo.low << 8) & (TOP_VALUE - 1);
            rangeCoderInfo.range <<= 8;
        }
    }

    private void encodeFast(long rangeWidth, long rangeTotal, int shift) {
        normalizeRangeCoder();
        long temp = rangeCoderInfo.range >> shift;
        rangeCoderInfo.range = temp * rangeWidth;
        rangeCoderInfo.low += temp * rangeTotal;
    }

    private void encodeDirect(long value, int shift) {
        normalizeRangeCoder();
        rangeCoderInfo.range = rangeCoderInfo.range >> shift;
        rangeCoderInfo.low += rangeCoderInfo.range * value;
    }

    private void putc(long value) {
        bitArray[(int) (currentBitIndex >> 5)] |= (int) ((value & 0xFF) << (24 - (currentBitIndex & 31)));
        currentBitIndex += 8;
    }

    private void putcNocap(long value) {
        bitArray[(int) (currentBitIndex >> 5)] |= (int) (value << (24 - (currentBitIndex & 31)));
        currentBitIndex += 8;
    }

    public void checkValue(long value) {
        if (value < 0 || value > 4294967295L)
            throw new JMACException("Wrong Value: " + value);
    }

    // encoding
    public void encodeUnsignedLong(long n) throws IOException {
        // make sure there are at least 8 bytes in the buffer
        if (currentBitIndex > (BIT_ARRAY_BYTES - 8))
            outputBitArray();

        // encode the value
        int bitArrayIndex = (int) (currentBitIndex >> 5);
        int bitIndex = (int) (currentBitIndex & 31);

        if (bitIndex == 0)
            bitArray[bitArrayIndex] = (int) n;
        else {
            bitArray[bitArrayIndex] |= (int) (n >> bitIndex);
            bitArray[bitArrayIndex + 1] = (int) (n << (32 - bitIndex));
        }

        currentBitIndex += 32;
    }

    public void encodeValue(int encode, BitArrayState bitArrayState) throws IOException {
        // make sure there is room for the data
        // this is a little slower than ensuring a huge block to start with, but it's safer
        if (currentBitIndex > REFILL_BIT_THRESHOLD)
            outputBitArray();

        // convert to unsigned
        encode = (encode > 0) ? encode * 2 - 1 : -encode * 2;

        int originalKSum = bitArrayState.kSum;

        // update kSum
        bitArrayState.kSum += ((encode + 1) / 2) - ((bitArrayState.kSum + 16) >> 5);

        // update k
        if (bitArrayState.kSum < K_SUM_MIN_BOUNDARY[bitArrayState.k])
            bitArrayState.k--;
        else if (bitArrayState.kSum >= K_SUM_MIN_BOUNDARY[bitArrayState.k + 1])
            bitArrayState.k++;

        // figure the pivot value
        int pivotValue = Math.max(originalKSum / 32, 1);
        int overflow = encode / pivotValue;
        int base = encode - (overflow * pivotValue);

        // store the overflow
        if (overflow < (MODEL_ELEMENTS - 1))
            encodeFast(RANGE_WIDTH[overflow], RANGE_TOTAL[overflow], RANGE_OVERFLOW_SHIFT);
        else {
            // store the "special" overflow (tells that perfect k is encoded next)
            encodeFast(RANGE_WIDTH[MODEL_ELEMENTS - 1], RANGE_TOTAL[MODEL_ELEMENTS - 1], RANGE_OVERFLOW_SHIFT);

            // code the overflow using straight bits
            encodeDirect((overflow >> 16) & 0xFFFF, 16);
            encodeDirect(overflow & 0xFFFF, 16);
        }

        // code the base
        {
            if (pivotValue >= (1 << 16)) {
                int pivotValueBits = 0;
                while ((pivotValue >> pivotValueBits) > 0)
                    pivotValueBits++;
                int splitFactor = 1 << (pivotValueBits - 16);

                // we know that base is smaller than pivot coming into this
                // however, after we divide both by an integer, they could be the same
                // we account by adding one to the pivot, but this hurts compression
                // by (1 / splitFactor) -- therefore we maximize the split factor
                // that gets one added to it

                // encode the pivot as two pieces
                int pivotValueA = (pivotValue / splitFactor) + 1;
                int pivotValueB = splitFactor;

                int baseA = base / splitFactor;
                int baseB = base % splitFactor;

                {
                    normalizeRangeCoder();
                    long temp = rangeCoderInfo.range / pivotValueA;
                    rangeCoderInfo.range = temp;
                    rangeCoderInfo.low += temp * baseA;
                }

                {
                    normalizeRangeCoder();
                    long temp = rangeCoderInfo.range / pivotValueB;
                    rangeCoderInfo.range = temp;
                    rangeCoderInfo.low += temp * baseB;
                }
            } else {
                normalizeRangeCoder();
                long temp = rangeCoderInfo.range / pivotValue;
                rangeCoderInfo.range = temp;
                rangeCoderInfo.low += temp * base;
            }
        }
    }

    public void encodeBits(long value, int bits) throws IOException {
        // make sure there is room for the data
        // this is a little slower than ensuring a huge block to start with, but it's safer
        if (currentBitIndex > REFILL_BIT_THRESHOLD)
            outputBitArray();

        encodeDirect(value, bits);
    }

    // output (saving)

    public void outputBitArray() throws IOException {
        outputBitArray(false);
    }

    private final ByteArrayWriter writer = new ByteArrayWriter();

    public void outputBitArray(boolean finalize) throws IOException {
        // write the entire file to disk
        long bytesToWrite = 0;

        writer.reset(bitArray.length * 4);
        for (int j : bitArray) writer.writeInt(j);

        if (finalize) {
            bytesToWrite = ((currentBitIndex >> 5) * 4) + 4;

            md5.update(writer.getBytes(), (int) bytesToWrite);

            io.write(writer.getBytes(), 0, (int) bytesToWrite);

            // reset the bit pointer
            currentBitIndex = 0;
        } else {
            bytesToWrite = (currentBitIndex >> 5) * 4;

            md5.update(writer.getBytes(), (int) bytesToWrite);

            io.write(writer.getBytes(), 0, (int) bytesToWrite);

            // move the last value to the front of the bit array
            bitArray[0] = bitArray[(int) (currentBitIndex >> 5)];
            currentBitIndex = (currentBitIndex & 31);

            // zero the rest of the memory (may not need the +1 because of frame byte alignment)
            Arrays.fill(bitArray, 1, (int) (Math.min(bytesToWrite + 1, BIT_ARRAY_BYTES - 1) / 4 + 1), 0);
        }
    }

    // other functions

    public void finalize_() {
        normalizeRangeCoder();

        long temp = (rangeCoderInfo.low >> SHIFT_BITS) + 1;

        if (temp > 0xFF) { // we have a carry
            putc(rangeCoderInfo.buffer + 1);
            for (; rangeCoderInfo.help > 0; rangeCoderInfo.help--)
                putc(0);
        } else { // no carry
            putc(rangeCoderInfo.buffer);
            for (; rangeCoderInfo.help > 0; rangeCoderInfo.help--)
                putc(((char) 0xFF));
        }

        // we must output these bytes so the core can properly work at the end of the stream
        putc(temp & 0xFF);
        putc(0);
        putc(0);
        putc(0);
    }

    public void advanceToByteBoundary() {
        while ((currentBitIndex % 8) > 0)
            currentBitIndex++;
    }

    public long getCurrentBitIndex() {
        return currentBitIndex;
    }

    public void flushState(BitArrayState bitArrayState) {
        // k and ksum
        bitArrayState.k = 10;
        bitArrayState.kSum = (1 << bitArrayState.k) * 16;
    }

    public void flushBitArray() {
        // advance to a byte boundary (for alignment)
        advanceToByteBoundary();

        // the range coder
        rangeCoderInfo.low = 0;  // full code range
        rangeCoderInfo.range = TOP_VALUE;
        rangeCoderInfo.buffer = 0;
        rangeCoderInfo.help = 0;  // no bytes to follow
    }

    public MD5 getMD5Helper() {
        return md5;
    }

    // data members
    private int[] bitArray;
    private final File io;
    private long currentBitIndex;
    private final RangeCoderStructCompress rangeCoderInfo = new RangeCoderStructCompress();
    private final MD5 md5 = new MD5();
}
