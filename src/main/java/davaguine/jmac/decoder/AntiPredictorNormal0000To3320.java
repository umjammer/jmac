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


/**
 * @author Dmitry Vaguine
 * @version 04.03.2004 14:51:31
 */
public class AntiPredictorNormal0000To3320 extends AntiPredictor {

    @Override
    public void antiPredict(int[] inputArray, int[] outputArray, int numberOfElements) {
        // variable declares
        int ip, op, op1, op2;
        int p, pw;
        int m;

        // short frame handling
        if (numberOfElements < 32) {
            System.arraycopy(inputArray, 0, outputArray, 0, numberOfElements);
            return;
        }

        //
        // order 3
        //
        System.arraycopy(inputArray, 0, outputArray, 0, 8);

        // initialize values
        m = 300;
        op = 8;
        op1 = 7;
        op2 = 6;

        // make the first prediction
        p = (outputArray[7] * 3) - (outputArray[6] * 3) + outputArray[5];
        pw = (p * m) >> 12;

        // loop through the array
        for (ip = 8; ip < numberOfElements; ip++, op++, op1++, op2++) {

            // figure the output value
            outputArray[op] = inputArray[ip] + pw;

            // adjust m
            if (inputArray[ip] > 0)
                m += (p > 0) ? 4 : -4;
            else if (inputArray[ip] < 0)
                m += (p > 0) ? -4 : 4;

            // make the next prediction
            p = (outputArray[op] * 3) - (outputArray[op1] * 3) + outputArray[op2];
            pw = (p * m) >> 12;
        }


        //
        // order 2
        //
        System.arraycopy(inputArray, 0, outputArray, 0, 8);
        m = 3000;

        op1 = 7;
        p = (inputArray[op1] * 2) - inputArray[6];
        pw = (p * m) >> 12;

        for (op = 8, ip = 8; ip < numberOfElements; ip++, op++, op1++) {
            inputArray[op] = outputArray[ip] + pw;

            // adjust m
            if (outputArray[ip] > 0)
                m += (p > 0) ? 12 : -12;
            else if (outputArray[ip] < 0)
                m += (p > 0) ? -12 : 12;

            p = (inputArray[op] * 2) - inputArray[op1];
            pw = (p * m) >> 12;

        }

        //
        // order 1
        //
        outputArray[0] = inputArray[0];
        outputArray[1] = inputArray[1] + outputArray[0];
        outputArray[2] = inputArray[2] + outputArray[1];
        outputArray[3] = inputArray[3] + outputArray[2];
        outputArray[4] = inputArray[4] + outputArray[3];
        outputArray[5] = inputArray[5] + outputArray[4];
        outputArray[6] = inputArray[6] + outputArray[5];
        outputArray[7] = inputArray[7] + outputArray[6];

        m = 3900;

        p = outputArray[7];
        pw = (p * m) >> 12;

        for (op = 8, ip = 8; ip < numberOfElements; ip++, op++) {
            outputArray[op] = inputArray[ip] + pw;

            // adjust m
            if (inputArray[ip] > 0)
                m += (p > 0) ? 1 : -1;
            else if (inputArray[ip] < 0)
                m += (p > 0) ? -1 : 1;

            p = outputArray[op];
            pw = (p * m) >> 12;
        }
    }
}
