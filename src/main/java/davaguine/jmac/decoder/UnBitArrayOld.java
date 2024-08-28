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


/**
 * @author Dmitry Vaguine
 * @version 04.03.2004 14:51:31
 */
public class UnBitArrayOld extends UnBitArrayBase {

    public final static long[] K_SUM_MIN_BOUNDARY_OLD = {
            0, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536, 131072, 262144, 524288, 1048576, 2097152, 4194304, 8388608, 16777216, 33554432, 67108864, 134217728, 268435456, 536870912, 1073741824, 2147483648L, 0, 0, 0, 0, 0, 0};
    public final static long[] K_SUM_MAX_BOUNDARY_OLD = {
            128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536, 131072, 262144, 524288, 1048576, 2097152, 4194304, 8388608, 16777216, 33554432, 67108864, 134217728, 268435456, 536870912, 1073741824, 2147483648L, 0, 0, 0, 0, 0, 0, 0};
    public final static long[] Powers_of_Two = {
            1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536, 131072, 262144, 524288, 1048576, 2097152, 4194304, 8388608, 16777216, 33554432, 67108864, 134217728, 268435456, 536870912, 1073741824, 2147483648L};
    public final static long[] POWERS_OF_TWO_REVERSED = {
            2147483648L, 1073741824, 536870912, 268435456, 134217728, 67108864, 33554432, 16777216, 8388608, 4194304, 2097152, 1048576, 524288, 262144, 131072, 65536, 32768, 16384, 8192, 4096, 2048, 1024, 512, 256, 128, 64, 32, 16, 8, 4, 2, 1};
    public final static long[] Powers_of_Two_Minus_One = {
            0, 1, 3, 7, 15, 31, 63, 127, 255, 511, 1023, 2047, 4095, 8191, 16383, 32767, 65535, 131071, 262143, 524287, 1048575, 2097151, 4194303, 8388607, 16777215, 33554431, 67108863, 134217727, 268435455, 536870911, 1073741823, 2147483647, 4294967295L};
    public final static long[] POWERS_OF_TWO_MINUS_ONE_REVERSED = {
            4294967295L, 2147483647, 1073741823, 536870911, 268435455, 134217727, 67108863, 33554431, 16777215, 8388607, 4194303, 2097151, 1048575, 524287, 262143, 131071, 65535, 32767, 16383, 8191, 4095, 2047, 1023, 511, 255, 127, 63, 31, 15, 7, 3, 1, 0};

    public final static long[] K_SUM_MIN_BOUNDARY = {
            0, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536, 131072, 262144, 524288, 1048576, 2097152, 4194304, 8388608, 16777216, 33554432, 67108864, 134217728, 268435456, 536870912, 1073741824, 2147483648L, 0, 0, 0, 0};
    public final static long[] K_SUM_MAX_BOUNDARY = {
            32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536, 131072, 262144, 524288, 1048576, 2097152, 4194304, 8388608, 16777216, 33554432, 67108864, 134217728, 268435456, 536870912, 1073741824, 2147483648L, 0, 0, 0, 0, 0};

    // construction/destruction

    public UnBitArrayOld(IAPEDecompress apeDecompress, int version) {
        int bitArrayBytes = 262144;

        // calculate the bytes
        if (version <= 3880) {
            int maxFrameBytes = (apeDecompress.getApeInfoBlocksPerFrame() * 50) / 8;
            bitArrayBytes = 65536;
            while (bitArrayBytes < maxFrameBytes)
                bitArrayBytes <<= 1;

            bitArrayBytes = Math.max(bitArrayBytes, 262144);
        } else if (version <= 3890)
            bitArrayBytes = 65536;

        createHelper(apeDecompress.getApeInfoIoSource(), bitArrayBytes, version);

        // set the refill threshold
        if (version <= 3880)
            refillBitThreshold = (bits - (16384 * 8));
        else
            refillBitThreshold = (bits - 512);
    }

    // functions

    @Override
    public void generateArray(int[] outputArray, int elements, int bytesRequired) throws IOException {
        if (version < 3860)
            generateArrayOld(outputArray, elements, bytesRequired);
        else if (version <= 3890)
            generateArrayRice(outputArray, elements, bytesRequired);
    }

    @Override
    public long decodeValue(int decodeMethod, int param1, int param2) throws IOException {
        return switch (decodeMethod) {
            case DecodeValueMethod.DECODE_VALUE_METHOD_UNSIGNED_INT -> decodeValueXBits(32);
            case DecodeValueMethod.DECODE_VALUE_METHOD_UNSIGNED_RICE -> decodeValueRiceUnsigned(param1);
            case DecodeValueMethod.DECODE_VALUE_METHOD_X_BITS -> decodeValueXBits(param1);
            default -> 0;
        };
    }

    private void generateArrayOld(int[] outputArray, long numberOfElements, int minimumCurrentBitIndexArrayBytes) throws IOException {
        // variable declarations
        long kSum;
        long q;
        long kmin, kmax;
        long k;
        long max;
        int p1, p2;

        // fill bit array if necessary
        // could use seek information to determine what the max was...
        long maxBitsNeeded = numberOfElements * 50;

        if (minimumCurrentBitIndexArrayBytes > 0)
            // this is actually probably double what is really needed
            // we can only calculate the space needed for both arrays in multichannel
            maxBitsNeeded = ((minimumCurrentBitIndexArrayBytes + 4) * 8L);

        if (maxBitsNeeded > getBitsRemaining())
            fillBitArray();

        // decode the first 5 elements (all k = 10)
        max = (numberOfElements < 5) ? numberOfElements : 5;
        for (q = 0; q < max; q++)
            outputArray[(int) q] = (int) decodeValueRiceUnsigned(10);

        // quit if that was all
        if (numberOfElements <= 5) {
            int tvi;
            for (int i = 0; i < numberOfElements; i++) {
                tvi = outputArray[i];
                outputArray[i] = (tvi & 1) > 0 ? (tvi >> 1) + 1 : -(tvi >> 1);
            }
            return;
        }

        // update k and kSum
        kSum = outputArray[0] + outputArray[1] + outputArray[2] + outputArray[3] + outputArray[4];
        k = getK(kSum / 10);

        // work through the rest of the elements before the primary loop
        max = (numberOfElements < 64) ? numberOfElements : 64;
        for (q = 5; q < max; q++) {
            outputArray[(int) q] = (int) decodeValueRiceUnsigned(k);
            kSum += outputArray[(int) q];
            k = getK(kSum / (q + 1) / 2);
        }

        // quit if that was all
        if (numberOfElements <= 64) {
            int tvi;
            for (int i = 0; i < numberOfElements; i++) {
                tvi = outputArray[i];
                outputArray[i] = (tvi & 1) > 0 ? (tvi >> 1) + 1 : -(tvi >> 1);
            }
            return;
        }

        // set all of the variables up for the primary loop
        long v, bitArrayIndex;
        k = getK(kSum >> 7);
        kmin = K_SUM_MIN_BOUNDARY_OLD[(int) k];
        kmax = K_SUM_MAX_BOUNDARY_OLD[(int) k];

        // the primary loop
        for (p1 = 64, p2 = 0; p1 < numberOfElements; p1++, p2++) {
            // plug through the string of 0's (the overflow)
            long Bit_Initial = currentBitIndex;
            while ((bitArray[(int) (currentBitIndex >> 5)] & POWERS_OF_TWO_REVERSED[(int) (currentBitIndex++ & 31)]) == 0)
                ;

            // if k = 0, your done
            if (k == 0)
                v = (currentBitIndex - Bit_Initial - 1);
            else {
                // put the overflow value into v
                v = (currentBitIndex - Bit_Initial - 1) << k;

                // store the bit information and incement the bit pointer by 'k'
                bitArrayIndex = currentBitIndex >> 5;
                long bitIndex = currentBitIndex & 31;
                currentBitIndex += k;

                // figure the extra bits on the left and the left value
                int leftExtraBits = (int) ((32 - k) - bitIndex);
                long leftValue = bitArray[(int) bitArrayIndex] & POWERS_OF_TWO_MINUS_ONE_REVERSED[(int) bitIndex];

                if (leftExtraBits >= 0)
                    v |= (leftValue >> leftExtraBits);
                else
                    v |= (leftValue << -leftExtraBits) | (bitArray[(int) (bitArrayIndex + 1)] >> (32 + leftExtraBits));
            }

            outputArray[p1] = (int) v;
            kSum += outputArray[p1] - outputArray[p2];

            // convert *p2 to unsigned
            outputArray[p2] = (outputArray[p2] % 2) > 0 ? (outputArray[p2] >> 1) + 1 : -(outputArray[p2] >> 1);

            // adjust k if necessary
            if ((kSum < kmin) || (kSum >= kmax)) {
                if (kSum < kmin)
                    while (kSum < K_SUM_MIN_BOUNDARY_OLD[(int) (--k)]) ;
                else
                    while (kSum >= K_SUM_MAX_BOUNDARY_OLD[(int) (++k)]) ;

                kmax = K_SUM_MAX_BOUNDARY_OLD[(int) k];
                kmin = K_SUM_MIN_BOUNDARY_OLD[(int) k];
            }
        }

        for (; p2 < numberOfElements; p2++)
            outputArray[p2] = (outputArray[p2] & 1) > 0 ? (outputArray[p2] >> 1) + 1 : -(outputArray[p2] >> 1);
    }

    private void generateArrayRice(int[] outputArray, long numberOfElements, int minimumBitArrayBytes) throws IOException {
        //
        // decode the bit array
        //

        k = 10;
        kSum = 1024 * 16;

        if (version <= 3880) {
            // the primary loop
            for (int i = 0; i < numberOfElements; i++)
                outputArray[i] = decodeValueNew(false);
        } else {
            // the primary loop
            for (int i = 0; i < numberOfElements; i++)
                outputArray[i] = decodeValueNew(true);
        }
    }

    private long decodeValueRiceUnsigned(long k) throws IOException {
        // variable declares
        long v;

        // plug through the string of 0's (the overflow)
        long bitInitial = currentBitIndex;
        while ((bitArray[(int) (currentBitIndex >> 5)] & POWERS_OF_TWO_REVERSED[(int) (currentBitIndex++ & 31)]) == 0)
            ;

        // if k = 0, you're done
        if (k == 0)
            return (currentBitIndex - bitInitial - 1);

        // put the overflow value into v
        v = (currentBitIndex - bitInitial - 1) << k;

        return v | decodeValueXBits(k);
    }

    // data

    private long k;
    private long kSum;
    private final long refillBitThreshold;

    // functions

    private int decodeValueNew(boolean capOverflow) throws IOException {
        // make sure there is room for the data
        // this is a little slower than ensuring a huge block to start with, but it's safer
        if (currentBitIndex > refillBitThreshold)
            fillBitArray();

        long v;

        // plug through the string of 0's (the overflow)
        long bitInitial = currentBitIndex;
        while ((bitArray[(int) (currentBitIndex >> 5)] & POWERS_OF_TWO_REVERSED[(int) (currentBitIndex++ & 31)]) == 0)
            ;

        int overflow = (int) (currentBitIndex - bitInitial - 1);

        if (capOverflow) {
            while (overflow >= 16) {
                k += 4;
                overflow -= 16;
            }
        }

        // if k = 0, you're done
        if (k != 0) {
            // put the overflow value into v
            v = (long) overflow << k;

            // store the bit information and incement the bit pointer by 'k'
            long bitArrayIndex = currentBitIndex >> 5;
            long bitIndex = currentBitIndex & 31;
            currentBitIndex += k;

            // figure the extra bits on the left and the left value
            int leftExtraBits = (int) ((32 - k) - bitIndex);
            long leftValue = bitArray[(int) bitArrayIndex] & POWERS_OF_TWO_MINUS_ONE_REVERSED[(int) bitIndex];

            if (leftExtraBits >= 0)
                v |= (leftValue >> leftExtraBits);
            else
                v |= (leftValue << -leftExtraBits) | (bitArray[(int) (bitArrayIndex + 1)] >> (32 + leftExtraBits));
        } else
            v = overflow;

        // update kSum
        kSum += v - ((kSum + 8) >> 4);

        // update k
        if (kSum < K_SUM_MIN_BOUNDARY[(int) k])
            k--;
        else if (kSum >= K_SUM_MAX_BOUNDARY[(int) k])
            k++;

        // convert to unsigned and save
        return (v & 1) > 0 ? (int) ((v >> 1) + 1) : -((int) (v >> 1));
    }

    private long getBitsRemaining() {
        return (elements * 32 - currentBitIndex);
    }

    private static long getK(long x) {
        if (x == 0) return 0;

        long k = 0;
        while (x >= Powers_of_Two[(int) (++k)]) ;

        return k;
    }
}
