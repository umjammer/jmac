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

import java.util.Arrays;


/**
 * @author Dmitry Vaguine
 * @version 04.03.2004 14:51:31
 */
public class AntiPredictorHigh3700To3800 extends AntiPredictor {

    private final static int FIRST_ELEMENT = 16;
    private final int[] bm = new int[FIRST_ELEMENT];

    @Override
    public void antiPredict(int[] inputArray, int[] outputArray, int numberOfElements) {
        // the frame to start prediction on

        // short frame handling
        if (numberOfElements < 20) {
            System.arraycopy(inputArray, 0, outputArray, 0, numberOfElements);
            return;
        }

        // make the first five samples identical in both arrays
        System.arraycopy(inputArray, 0, outputArray, 0, FIRST_ELEMENT);

        // variable declares and initializations
        Arrays.fill(bm, 0);
        int m2 = 64, m3 = 115, m4 = 64, m5 = 740, m6 = 0;
        int p4 = inputArray[FIRST_ELEMENT - 1];
        int p3 = (inputArray[FIRST_ELEMENT - 1] - inputArray[FIRST_ELEMENT - 2]) << 1;
        int p2 = inputArray[FIRST_ELEMENT - 1] + ((inputArray[FIRST_ELEMENT - 3] - inputArray[FIRST_ELEMENT - 2]) << 3);
        int op = FIRST_ELEMENT;
        int ip = FIRST_ELEMENT;
        int ipp2 = inputArray[ip - 2];
        int ipp1 = inputArray[ip - 1];
        int p7 = 2 * inputArray[ip - 1] - inputArray[ip - 2];
        int opp = outputArray[op - 1];
        int original;

        // undo the initial prediction stuff
        for (int q = 1; q < FIRST_ELEMENT; q++) {
            outputArray[q] += outputArray[q - 1];
        }

        // pump the primary loop
        for (; op < numberOfElements; op++, ip++) {

            original = inputArray[ip] - 1;
            inputArray[ip] = original - (((inputArray[ip - 1] * bm[0]) + (inputArray[ip - 2] * bm[1]) + (inputArray[ip - 3] * bm[2]) + (inputArray[ip - 4] * bm[3]) + (inputArray[ip - 5] * bm[4]) + (inputArray[ip - 6] * bm[5]) + (inputArray[ip - 7] * bm[6]) + (inputArray[ip - 8] * bm[7]) + (inputArray[ip - 9] * bm[8]) + (inputArray[ip - 10] * bm[9]) + (inputArray[ip - 11] * bm[10]) + (inputArray[ip - 12] * bm[11]) + (inputArray[ip - 13] * bm[12]) + (inputArray[ip - 14] * bm[13]) + (inputArray[ip - 15] * bm[14]) + (inputArray[ip - 16] * bm[15])) >> 8);

            if (original > 0) {
                bm[0] -= inputArray[ip - 1] > 0 ? 1 : -1;

                bm[1] += (int) ((((long) (inputArray[ip - 2]) >> 30) & 2) - 1);

                bm[2] -= inputArray[ip - 3] > 0 ? 1 : -1;
                bm[3] += (int) ((((long) (inputArray[ip - 4]) >> 30) & 2) - 1);

                bm[4] -= inputArray[ip - 5] > 0 ? 1 : -1;
                bm[5] += (int) ((((long) (inputArray[ip - 6]) >> 30) & 2) - 1);
                bm[6] -= inputArray[ip - 7] > 0 ? 1 : -1;
                bm[7] += (int) ((((long) (inputArray[ip - 8]) >> 30) & 2) - 1);
                bm[8] -= inputArray[ip - 9] > 0 ? 1 : -1;
                bm[9] += (int) ((((long) (inputArray[ip - 10]) >> 30) & 2) - 1);
                bm[10] -= inputArray[ip - 11] > 0 ? 1 : -1;
                bm[11] += (int) ((((long) (inputArray[ip - 12]) >> 30) & 2) - 1);
                bm[12] -= inputArray[ip - 13] > 0 ? 1 : -1;
                bm[13] += (int) ((((long) (inputArray[ip - 14]) >> 30) & 2) - 1);
                bm[14] -= inputArray[ip - 15] > 0 ? 1 : -1;
                bm[15] += (int) ((((long) (inputArray[ip - 16]) >> 30) & 2) - 1);
            } else if (original < 0) {
                bm[0] -= inputArray[ip - 1] <= 0 ? 1 : -1;

                bm[1] -= (int) ((((long) (inputArray[ip - 2]) >> 30) & 2) - 1);

                bm[2] -= inputArray[ip - 3] <= 0 ? 1 : -1;
                bm[3] -= (int) ((((long) (inputArray[ip - 4]) >> 30) & 2) - 1);
                bm[4] -= inputArray[ip - 5] <= 0 ? 1 : -1;
                bm[5] -= (int) ((((long) (inputArray[ip - 6]) >> 30) & 2) - 1);
                bm[6] -= inputArray[ip - 7] <= 0 ? 1 : -1;
                bm[7] -= (int) ((((long) (inputArray[ip - 8]) >> 30) & 2) - 1);
                bm[8] -= inputArray[ip - 9] <= 0 ? 1 : -1;
                bm[9] -= (int) ((((long) (inputArray[ip - 10]) >> 30) & 2) - 1);
                bm[10] -= inputArray[ip - 11] <= 0 ? 1 : -1;
                bm[11] -= (int) ((((long) (inputArray[ip - 12]) >> 30) & 2) - 1);
                bm[12] -= inputArray[ip - 13] <= 0 ? 1 : -1;
                bm[13] -= (int) ((((long) (inputArray[ip - 14]) >> 30) & 2) - 1);
                bm[14] -= inputArray[ip - 15] <= 0 ? 1 : -1;
                bm[15] -= (int) ((((long) (inputArray[ip - 16]) >> 30) & 2) - 1);
            }

            //
            outputArray[op] = inputArray[ip] + (((p2 * m2) + (p3 * m3) + (p4 * m4)) >> 11);

            if (inputArray[ip] > 0) {
                m2 -= p2 > 0 ? -1 : 1;
                m3 -= p3 > 0 ? -4 : 4;
                m4 -= p4 > 0 ? -4 : 4;
            } else if (inputArray[ip] < 0) {
                m2 -= p2 > 0 ? 1 : -1;
                m3 -= p3 > 0 ? 4 : -4;
                m4 -= p4 > 0 ? 4 : -4;
            }

            p4 = outputArray[op];
            p2 = p4 + ((ipp2 - ipp1) << 3);
            p3 = (p4 - ipp1) << 1;

            ipp2 = ipp1;
            ipp1 = p4;

            //
            outputArray[op] += (((p7 * m5) - (opp * m6)) >> 10);

            if ((ipp1 ^ p7) >= 0)
                m5 += 2;
            else
                m5 -= 2;
            if ((ipp1 ^ opp) >= 0)
                m6--;
            else
                m6++;

            p7 = 2 * outputArray[op] - opp;
            opp = outputArray[op];

            //
            outputArray[op] += ((outputArray[op - 1] * 31) >> 5);
        }
    }
}
