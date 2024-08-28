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

import davaguine.jmac.tools.JMACException;
import davaguine.jmac.tools.RollBufferShort;


/**
 * @author Dmitry Vaguine
 * @version 04.03.2004 14:51:31
 */
public abstract class NNFilter {

    public final static int NN_WINDOW_ELEMENTS = 512;

    public NNFilter(int order, int shift, int version) {
        if ((order <= 0) || ((order % 16) != 0))
            throw new JMACException("Wrong Order");
        this.order = order;
        this.shift = shift;
        this.version = version;
        input.create(512 /* NN_WINDOW_ELEMENTS */, order);
        deltaM.create(512 /*NN_WINDOW_ELEMENTS */, order);
        m = new short[order];
    }

    public int compress(int input) {
        RollBufferShort input_ = this.input;
        RollBufferShort delta = deltaM;
        int order = this.order;
        int shift = this.shift;
        short[] m_ = m;
        short[] inputData = input_.data;
        int inputIndex = input_.index;
        short[] deltaData = delta.data;
        int deltaIndex = delta.index;

        // convert the input to a short and store it
        inputData[inputIndex] = (short) ((input >= Short.MIN_VALUE && input <= Short.MAX_VALUE) ? input : (input >> 31) ^ 0x7FFF);

        // figure a dot product
        int dotProduct = calculateDotProductNoMMX(inputData, inputIndex - order, m_, 0);

        // calculate the output
        int output = input - ((dotProduct + (1 << (this.shift - 1))) >> this.shift);

        // adapt
        adaptNoMMX(m_, 0, deltaData, deltaIndex - order, output);

        int tempABS = Math.abs(input);

        if (tempABS > (runningAverage * 3))
            deltaData[deltaIndex] = (short) (((input >> 25) & 64) - 32);
        else if (tempABS > (runningAverage * 4) / 3)
            deltaData[deltaIndex] = (short) (((input >> 26) & 32) - 16);
        else if (tempABS > 0)
            deltaData[deltaIndex] = (short) (((input >> 27) & 16) - 8);
        else
            deltaData[deltaIndex] = (short) 0;

        runningAverage += (tempABS - runningAverage) / 16;

        deltaData[deltaIndex - 1] >>= 1;
        deltaData[deltaIndex - 2] >>= 1;
        deltaData[deltaIndex - 8] >>= 1;

        // increment and roll if necessary
//        input.incrementSafe();
        if ((++input_.index) == orderPlusWindow) {
            System.arraycopy(inputData, input_.index - order, inputData, 0, order);
            input_.index = order;
        }
//        delta.incrementSafe();
        if ((++delta.index) == orderPlusWindow) {
            System.arraycopy(deltaData, delta.index - order, deltaData, 0, order);
            delta.index = order;
        }

        return output;
    }

    public int decompress(int input) {
        // figure a dot product
        RollBufferShort input_ = this.input;
        RollBufferShort delta = deltaM;
        int order = this.order;
        int shift = this.shift;
        short[] m_ = m;
        short[] inputData = input_.data;
        int inputIndex = input_.index;
        short[] deltaData = delta.data;
        int deltaIndex = delta.index;
        int dotProduct = calculateDotProductNoMMX(inputData, inputIndex - order, m_, 0);

        // adapt
        adaptNoMMX(m_, 0, deltaData, deltaIndex - order, input);

        // store the output value
        int output = input + ((dotProduct + (1 << (shift - 1))) >> shift);

        // update the input buffer
        inputData[inputIndex] = (short) ((output >= Short.MIN_VALUE && output <= Short.MAX_VALUE) ? output : (output >> 31) ^ 0x7FFF);

        if (version >= 3980) {
            int tempABS = Math.abs(output);

            if (tempABS > (runningAverage * 3))
                deltaData[deltaIndex] = (short) (((output >> 25) & 64) - 32);
            else if (tempABS > (runningAverage * 4) / 3)
                deltaData[deltaIndex] = (short) (((output >> 26) & 32) - 16);
            else if (tempABS > 0)
                deltaData[deltaIndex] = (short) (((output >> 27) & 16) - 8);
            else
                deltaData[deltaIndex] = 0;

            runningAverage += (tempABS - runningAverage) / 16;

            deltaData[deltaIndex - 1] >>= 1;
            deltaData[deltaIndex - 2] >>= 1;
            deltaData[deltaIndex - 8] >>= 1;
        } else {
            deltaData[deltaIndex] = (short) ((output == 0) ? 0 : ((output >> 28) & 8) - 4);
            deltaData[deltaIndex - 4] >>= 1;
            deltaData[deltaIndex - 8] >>= 1;
        }

        // increment and roll if necessary
//        input.incrementSafe();
        if ((++input_.index) == orderPlusWindow) {
            System.arraycopy(inputData, input_.index - order, inputData, 0, order);
            input_.index = order;
        }
//        delta.incrementSafe();
        if ((++delta.index) == orderPlusWindow) {
            System.arraycopy(deltaData, delta.index - order, deltaData, 0, order);
            delta.index = order;
        }

        return output;
    }

    public void flush() {
        Arrays.fill(m, (short) 0);
        input.flush();
        deltaM.flush();
        runningAverage = 0;
    }

    protected int order;
    protected int shift;
    protected int version;
    protected int orderPlusWindow;
    private int runningAverage;

    private final RollBufferShort input = new RollBufferShort();
    private final RollBufferShort deltaM = new RollBufferShort();

    private final short[] m;

    protected abstract int calculateDotProductNoMMX(short[] a, int indexA, short[] b, int indexB);

    protected abstract void adaptNoMMX(short[] m, int indexM, short[] adapt, int indexA, int direction);
}
