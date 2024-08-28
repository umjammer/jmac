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
public class AntiPredictorExtraHigh3700To3800 extends AntiPredictor {

    public void antiPredict(int[] inputArray, int[] outputArray, int numberOfElements, int iterations, long[] offsetValueArrayA, long[] offsetValueArrayB) {
        for (int z = iterations; z >= 0; ) {

            antiPredictorOffset(inputArray, outputArray, numberOfElements, (int) offsetValueArrayA[z], (int) offsetValueArrayB[z], 64);
            z--;

            if (z >= 0) {
                antiPredictorOffset(outputArray, inputArray, numberOfElements, (int) offsetValueArrayA[z], (int) offsetValueArrayB[z], 64);
                z--;
            } else {
                System.arraycopy(outputArray, 0, inputArray, 0, numberOfElements);
                break;
            }
        }

        antiPredictor.antiPredict(inputArray, outputArray, numberOfElements);
    }

    private final AntiPredictorHigh3700To3800 antiPredictor = new AntiPredictorHigh3700To3800();

    protected static void antiPredictorOffset(int[] inputArray, int[] outputArray, int numberOfElements, int g1, int g2, int maxOrder) {
        int q;

        if ((g1 == 0) || (g2 == 0) || (numberOfElements <= maxOrder)) {
            System.arraycopy(inputArray, 0, outputArray, 0, numberOfElements);
            return;
        }

        System.arraycopy(inputArray, 0, outputArray, 0, maxOrder);

        int m = 64;
        int m2 = 64;

        for (q = maxOrder; q < numberOfElements; q++) {
            outputArray[q] = inputArray[q] + ((outputArray[q - g1] * m) >> 9) - ((outputArray[q - g2] * m2) >> 9);
            if ((inputArray[q] ^ outputArray[q - g1]) > 0)
                m++;
            else
                m--;
            if ((inputArray[q] ^ outputArray[q - g2]) > 0)
                m2--;
            else
                m2++;
        }
    }
}
