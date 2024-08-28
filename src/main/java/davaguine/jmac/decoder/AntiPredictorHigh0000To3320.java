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
public class AntiPredictorHigh0000To3320 extends AntiPredictor {

    @Override
    public void antiPredict(int[] inputArray, int[] outputArray, int numberOfElements) {
        // variable declares
        int p, pw;
        int q;
        int m;

        // short frame handling
        if (numberOfElements < 32) {
            System.arraycopy(inputArray, 0, outputArray, 0, numberOfElements);
            return;
        }

        //
        // order 5
        //
        System.arraycopy(inputArray, 0, outputArray, 0, 8);

        // initialize values
        m = 0;

        for (q = 8; q < numberOfElements; q++) {
            p = (5 * outputArray[q - 1]) - (10 * outputArray[q - 2]) + (12 * outputArray[q - 3]) - (7 * outputArray[q - 4]) + outputArray[q - 5];

            pw = (p * m) >> 12;

            outputArray[q] = inputArray[q] + pw;

            // adjust m
            if (inputArray[q] > 0) {
                if (p > 0)
                    m += 1;
                else
                    m -= 1;
            } else if (inputArray[q] < 0) {
                if (p > 0)
                    m -= 1;
                else
                    m += 1;
            }

        }

        //
        // order 4
        //
        System.arraycopy(outputArray, 0, inputArray, 0, 8);
        m = 0;

        for (q = 8; q < numberOfElements; q++) {
            p = (4 * inputArray[q - 1]) - (6 * inputArray[q - 2]) + (4 * inputArray[q - 3]) - inputArray[q - 4];
            pw = (p * m) >> 12;

            inputArray[q] = outputArray[q] + pw;

            // adjust m
            if (outputArray[q] > 0) {
                if (p > 0)
                    m += 2;
                else
                    m -= 2;
            } else if (outputArray[q] < 0) {
                if (p > 0)
                    m -= 2;
                else
                    m += 2;
            }

        }

        antiPredictor.antiPredict(inputArray, outputArray, numberOfElements);
    }

    private final AntiPredictorNormal0000To3320 antiPredictor = new AntiPredictorNormal0000To3320();
}
