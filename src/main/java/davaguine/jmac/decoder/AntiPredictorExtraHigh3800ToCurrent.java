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
public class AntiPredictorExtraHigh3800ToCurrent extends AntiPredictor {

    private final short[] bm = new short[256];
    private final int[] fm = new int[9];
    private final int[] fp = new int[9];
    private short[] adaptFactors = null;
    private short[] shorts = null;

    public void antiPredict(int[] inputArray, int[] outputArray, int numberOfElements, int version) {
        int filterStageElements = (version < 3830) ? 128 : 256;
        int filterStageShift = (version < 3830) ? 11 : 12;
        int maxElements = (version < 3830) ? 134 : 262;
        int firstElement = (version < 3830) ? 128 : 256;
        int stageCShift = (version < 3830) ? 10 : 11;

        // short frame handling
        if (numberOfElements < maxElements) {
            System.arraycopy(inputArray, 0, outputArray, 0, numberOfElements);
            return;
        }

        // make the first five samples identical in both arrays
        System.arraycopy(inputArray, 0, outputArray, 0, firstElement);

        // variable declares and initializations
        Arrays.fill(bm, (short) 0);
        int m2 = 64, m3 = 115, m4 = 64, m5 = 740, m6 = 0;
        int p4 = inputArray[firstElement - 1];
        int p3 = (inputArray[firstElement - 1] - inputArray[firstElement - 2]) << 1;
        int p2 = inputArray[firstElement - 1] + ((inputArray[firstElement - 3] - inputArray[firstElement - 2]) << 3);
        int op = firstElement;
        int ip = firstElement;
        int ipp2 = inputArray[ip - 2];
        int p7 = 2 * inputArray[ip - 1] - inputArray[ip - 2];
        int opp = outputArray[op - 1];
        int original;

        // undo the initial prediction stuff
        int q; // loop variable
        for (q = 1; q < firstElement; q++) {
            outputArray[q] += outputArray[q - 1];
        }

        // pump the primary loop
        if (adaptFactors == null || adaptFactors.length < numberOfElements)
            adaptFactors = new short[numberOfElements];
        if (shorts == null || shorts.length < numberOfElements)
            shorts = new short[numberOfElements];
        for (q = 0; q < firstElement; q++) {
            adaptFactors[q] = (short) (((inputArray[q] >> 30) & 2) - 1);
            shorts[q] = (short) inputArray[q];
        }

        Arrays.fill(fm, 0);
        Arrays.fill(fp, 0);

        for (q = firstElement; op < numberOfElements; op++, ip++, q++) {
            // CPU load-balancing
            if (version >= 3830) {
                int fpP = 8;
                int fmP = 8;
                int dotProduct = 0;
                fp[0] = inputArray[ip];

                if (fp[0] == 0) {
                    dotProduct += fp[fpP] * fm[fmP--];
                    fp[fpP--] = fp[fpP - 1];
                    dotProduct += fp[fpP] * fm[fmP--];
                    fp[fpP--] = fp[fpP - 1];
                    dotProduct += fp[fpP] * fm[fmP--];
                    fp[fpP--] = fp[fpP - 1];
                    dotProduct += fp[fpP] * fm[fmP--];
                    fp[fpP--] = fp[fpP - 1];
                    dotProduct += fp[fpP] * fm[fmP--];
                    fp[fpP--] = fp[fpP - 1];
                    dotProduct += fp[fpP] * fm[fmP--];
                    fp[fpP--] = fp[fpP - 1];
                    dotProduct += fp[fpP] * fm[fmP--];
                    fp[fpP--] = fp[fpP - 1];
                    dotProduct += fp[fpP] * fm[fmP--];
                    fp[fpP--] = fp[fpP - 1];
                } else if (fp[0] > 0) {
                    dotProduct += fp[fpP] * fm[fmP];
                    fm[fmP--] += ((fp[fpP] >> 30) & 2) - 1;
                    fp[fpP--] = fp[fpP - 1];
                    dotProduct += fp[fpP] * fm[fmP];
                    fm[fmP--] += ((fp[fpP] >> 30) & 2) - 1;
                    fp[fpP--] = fp[fpP - 1];
                    dotProduct += fp[fpP] * fm[fmP];
                    fm[fmP--] += ((fp[fpP] >> 30) & 2) - 1;
                    fp[fpP--] = fp[fpP - 1];
                    dotProduct += fp[fpP] * fm[fmP];
                    fm[fmP--] += ((fp[fpP] >> 30) & 2) - 1;
                    fp[fpP--] = fp[fpP - 1];
                    dotProduct += fp[fpP] * fm[fmP];
                    fm[fmP--] += ((fp[fpP] >> 30) & 2) - 1;
                    fp[fpP--] = fp[fpP - 1];
                    dotProduct += fp[fpP] * fm[fmP];
                    fm[fmP--] += ((fp[fpP] >> 30) & 2) - 1;
                    fp[fpP--] = fp[fpP - 1];
                    dotProduct += fp[fpP] * fm[fmP];
                    fm[fmP--] += ((fp[fpP] >> 30) & 2) - 1;
                    fp[fpP--] = fp[fpP - 1];
                    dotProduct += fp[fpP] * fm[fmP];
                    fm[fmP--] += ((fp[fpP] >> 30) & 2) - 1;
                    fp[fpP--] = fp[fpP - 1];
                } else {
                    dotProduct += fp[fpP] * fm[fmP];
                    fm[fmP--] -= ((fp[fpP] >> 30) & 2) - 1;
                    fp[fpP--] = fp[fpP - 1];
                    dotProduct += fp[fpP] * fm[fmP];
                    fm[fmP--] -= ((fp[fpP] >> 30) & 2) - 1;
                    fp[fpP--] = fp[fpP - 1];
                    dotProduct += fp[fpP] * fm[fmP];
                    fm[fmP--] -= ((fp[fpP] >> 30) & 2) - 1;
                    fp[fpP--] = fp[fpP - 1];
                    dotProduct += fp[fpP] * fm[fmP];
                    fm[fmP--] -= ((fp[fpP] >> 30) & 2) - 1;
                    fp[fpP--] = fp[fpP - 1];
                    dotProduct += fp[fpP] * fm[fmP];
                    fm[fmP--] -= ((fp[fpP] >> 30) & 2) - 1;
                    fp[fpP--] = fp[fpP - 1];
                    dotProduct += fp[fpP] * fm[fmP];
                    fm[fmP--] -= ((fp[fpP] >> 30) & 2) - 1;
                    fp[fpP--] = fp[fpP - 1];
                    dotProduct += fp[fpP] * fm[fmP];
                    fm[fmP--] -= ((fp[fpP] >> 30) & 2) - 1;
                    fp[fpP--] = fp[fpP - 1];
                    dotProduct += fp[fpP] * fm[fmP];
                    fm[fmP--] -= ((fp[fpP] >> 30) & 2) - 1;
                    fp[fpP--] = fp[fpP - 1];
                }

                inputArray[ip] -= dotProduct >> 9;
            }

            original = inputArray[ip];

            shorts[q] = (short) inputArray[ip];
            adaptFactors[q] = (short) (((inputArray[ip] >> 30) & 2) - 1);

            inputArray[ip] -= (AntiPredictorExtraHighHelper.conventionalDotProduct(shorts, q - firstElement, bm, 0, adaptFactors, q - firstElement, original, filterStageElements) >> filterStageShift);

            shorts[q] = (short) inputArray[ip];
            adaptFactors[q] = (short) (((inputArray[ip] >> 30) & 2) - 1);

            //
            outputArray[op] = inputArray[ip] + (((p2 * m2) + (p3 * m3) + (p4 * m4)) >> 11);

            if (inputArray[ip] > 0) {
                m2 -= ((p2 >> 30) & 2) - 1;
                m3 -= ((p3 >> 28) & 8) - 4;
                m4 -= ((p4 >> 28) & 8) - 4;
            } else if (inputArray[ip] < 0) {
                m2 += ((p2 >> 30) & 2) - 1;
                m3 += ((p3 >> 28) & 8) - 4;
                m4 += ((p4 >> 28) & 8) - 4;
            }


            p2 = outputArray[op] + ((ipp2 - p4) << 3);
            p3 = (outputArray[op] - p4) << 1;
            ipp2 = p4;
            p4 = outputArray[op];

            //
            outputArray[op] += (((p7 * m5) - (opp * m6)) >> stageCShift);

            if (p4 > 0) {
                m5 -= ((p7 >> 29) & 4) - 2;
                m6 += ((opp >> 30) & 2) - 1;
            } else if (p4 < 0) {
                m5 += ((p7 >> 29) & 4) - 2;
                m6 -= ((opp >> 30) & 2) - 1;
            }

            p7 = 2 * outputArray[op] - opp;
            opp = outputArray[op];

            //
            outputArray[op] += ((outputArray[op - 1] * 31) >> 5);
        }
    }
}
