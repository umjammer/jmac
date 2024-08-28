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
public class AntiPredictorNormal3800ToCurrent extends AntiPredictor {

    private final static int FIRST_ELEMENT = 4;

    @Override
    public void antiPredict(int[] inputArray, int[] outputArray, int numberOfElements) {
        // short frame handling
        if (numberOfElements < 8) {
            System.arraycopy(inputArray, 0, outputArray, 0, numberOfElements);
            return;
        }

        // make the first five samples identical in both arrays
        System.arraycopy(inputArray, 0, outputArray, 0, FIRST_ELEMENT);

        // variable declares and initializations
        int m2 = 64, m3 = 115, m4 = 64, m5 = 740, m6 = 0;
        int p4 = inputArray[FIRST_ELEMENT - 1];
        int p3 = (inputArray[FIRST_ELEMENT - 1] - inputArray[FIRST_ELEMENT - 2]) << 1;
        int p2 = inputArray[FIRST_ELEMENT - 1] + ((inputArray[FIRST_ELEMENT - 3] - inputArray[FIRST_ELEMENT - 2]) << 3);
        int op = FIRST_ELEMENT;
        int ip = FIRST_ELEMENT;
        int ipp2 = inputArray[ip - 2];
        int p7 = 2 * inputArray[ip - 1] - inputArray[ip - 2];
        int opp = outputArray[op - 1];

        // undo the initial prediction stuff
        for (int q = 1; q < FIRST_ELEMENT; q++) {
            outputArray[q] += outputArray[q - 1];
        }

        // pump the primary loop
        for (; op < numberOfElements; op++, ip++) {

            int o = outputArray[op], i = inputArray[ip];

            //
            o = i + (((p2 * m2) + (p3 * m3) + (p4 * m4)) >> 11);

            if (i > 0) {
                m2 -= ((p2 >> 30) & 2) - 1;
                m3 -= ((p3 >> 28) & 8) - 4;
                m4 -= ((p4 >> 28) & 8) - 4;

            } else if (i < 0) {
                m2 += ((p2 >> 30) & 2) - 1;
                m3 += ((p3 >> 28) & 8) - 4;
                m4 += ((p4 >> 28) & 8) - 4;
            }


            p2 = o + ((ipp2 - p4) << 3);
            p3 = (o - p4) << 1;
            ipp2 = p4;
            p4 = o;

            //
            o += (((p7 * m5) - (opp * m6)) >> 10);

            if (p4 > 0) {
                m5 -= ((p7 >> 29) & 4) - 2;
                m6 += ((opp >> 30) & 2) - 1;
            } else if (p4 < 0) {
                m5 += ((p7 >> 29) & 4) - 2;
                m6 -= ((opp >> 30) & 2) - 1;
            }

            p7 = 2 * o - opp;
            opp = o;

            //
            outputArray[op] = o + ((outputArray[op - 1] * 31) >> 5);
        }
    }
}
