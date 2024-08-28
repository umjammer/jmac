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

import davaguine.jmac.tools.ByteArrayReader;
import davaguine.jmac.tools.File;
import davaguine.jmac.tools.JMACException;


/**
 * @author Dmitry Vaguine
 * @version 04.03.2004 14:51:31
 */
public class UnBitArrayBase {

    private final static long[] POWERS_OF_TWO_MINUS_ONE = {
            0, 1, 3, 7, 15, 31, 63, 127,
            255, 511, 1023, 2047, 4095, 8191, 16383, 32767,
            65535, 131071, 262143, 524287, 1048575, 2097151, 4194303, 8388607,
            16777215, 33554431, 67108863, 134217727, 268435455, 536870911, 1073741823, 2147483647,
            4294967295L
    };

    // construction/destruction

    public UnBitArrayBase() {
    }

    // functions

    public void fillBitArray() throws IOException {
        // get the bit array index
        long bitArrayIndex = currentBitIndex >> 5;
        long[] al;
        int j;

        // move the remaining data to the front
        System.arraycopy(al = bitArray, j = (int) bitArrayIndex, al, 0, (int) (al.length - bitArrayIndex));

        // read the new data
        ByteArrayReader reader = this.reader;
        reader.reset(io, j << 2);
        long l1;
        int i = (int) ((l1 = elements) - bitArrayIndex);
        if ((long) i < l1)
            do {
                al[i] = reader.readUnsignedInt();
                i++;
            } while ((long) i < l1);

        // adjust the bit pointer
        currentBitIndex &= 31;
    }

    public void fillAndResetBitArray() throws IOException {
        fillAndResetBitArray(-1, 0);
    }

    public void fillAndResetBitArray(int fileLocation) throws IOException {
        fillAndResetBitArray(fileLocation, 0);
    }

    public void fillAndResetBitArray(int fileLocation, int newBitIndex) throws IOException {
        // reset the bit index
        currentBitIndex = newBitIndex;

        // seek if necessary
        if (fileLocation != -1)
            io.seek(fileLocation);

        // read the new data into the bit array
        ByteArrayReader reader = this.reader;
        reader.reset(io, (int) bytes);
        long[] al = bitArray;
        long l = elements;
        for (int i = 0; i < l; i++)
            al[i] = reader.readUnsignedInt();
    }

    public void generateArray(int[] outputArray, int elements) throws IOException {
        generateArray(outputArray, elements, -1);
    }

    public void generateArray(int[] outputArray, int elements, int bytesRequired) throws IOException {
    }

    public long decodeValue(int decodeMethod) throws IOException {
        return decodeValue(decodeMethod, 0, 0);
    }

    public long decodeValue(int decodeMethod, int param1) throws IOException {
        return decodeValue(decodeMethod, param1, 0);
    }

    public long decodeValue(int decodeMethod, int param1, int param2) throws IOException {
        return 0;
    }

    public void advanceToByteBoundary() {
        long mod = currentBitIndex % 8L;
        if (mod != 0)
            currentBitIndex += 8L - mod;
    }

    public int decodeValueRange(UnBitArrayState bitArrayState) throws IOException {
        return 0;
    }

    public void flushState(UnBitArrayState bitArrayState) {
    }

    public void flushBitArray() {
    }

    protected void finalize_() {
    }

    protected void createHelper(File io, int bytes, int version) {
        // check the parameters
        if ((io == null) || (bytes <= 0))
            throw new JMACException("Bad Parameter");

        // save the size
        elements = bytes / 4;
        this.bytes = elements * 4;
        bits = this.bytes * 8;

        // set the variables
        this.io = io;
        this.version = version;
        currentBitIndex = 0;

        // create the bitarray
        bitArray = new long[(int) elements];
        reader = new ByteArrayReader((int) this.bytes);
    }

    protected long decodeValueXBits(long bits) throws IOException {
        // get more data if necessary
        long bitArrayIndex;
        if (((bitArrayIndex = currentBitIndex) + bits) >= this.bits)
            fillBitArray();

        // variable declares
        long leftBits = 32 - (bitArrayIndex & 31);
        bitArrayIndex >>= 5;
        currentBitIndex += bits;

        // if there isn't an overflow to the right value, get the value and exit
        if (leftBits >= bits)
            return (bitArray[(int) bitArrayIndex] & (POWERS_OF_TWO_MINUS_ONE[(int) leftBits])) >> (leftBits - bits);

        // must get the "split" value from left and right
        long rightBits = bits - leftBits;

        long leftValue = (bitArray[(int) bitArrayIndex] & POWERS_OF_TWO_MINUS_ONE[(int) leftBits]) << rightBits;
        long rightValue = (bitArray[(int) bitArrayIndex + 1] >> (32 - rightBits));
        return (leftValue | rightValue);
    }

    public static UnBitArrayBase createUnBitArray(IAPEDecompress apeDecompress, int version) {
        if (version >= 3900)
            return new UnBitArray(apeDecompress.getApeInfoIoSource(), version);
        else
            return new UnBitArrayOld(apeDecompress, version);
    }

    protected long elements;
    protected long bytes;
    protected long bits;

    protected int version;
    protected File io;

    protected long currentBitIndex;
    protected long[] bitArray;
    protected ByteArrayReader reader;
}
