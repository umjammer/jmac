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
public class AntiPredictorExtraHigh0000To3320 extends AntiPredictor {

    public void antiPredict(int[] inputArray, int[] outputArray, int numberOfElements, int iterations, long[] offsetValueArrayA, long[] offsetValueArrayB) {
        for (int z = iterations; z >= 0; z--) {
            antiPredictorOffset(inputArray, outputArray, numberOfElements, (int) offsetValueArrayB[z], -1, 64);
            antiPredictorOffset(outputArray, inputArray, numberOfElements, (int) offsetValueArrayA[z], 1, 64);
        }

        antiPredictor.antiPredict(inputArray, outputArray, numberOfElements);
    }

    private final AntiPredictorHigh0000To3320 antiPredictor = new AntiPredictorHigh0000To3320();

    protected static void antiPredictorOffset(int[] inputArray, int[] outputArray, int numberOfElements, int g, int dm, int maxOrder) {
        int q;

        if ((g == 0) || (numberOfElements <= maxOrder)) {
            System.arraycopy(inputArray, 0, outputArray, 0, numberOfElements);
            return;
        }

        System.arraycopy(inputArray, 0, outputArray, 0, maxOrder);

        if (dm > 0)
            for (q = maxOrder; q < numberOfElements; q++) {
                outputArray[q] = inputArray[q] + (outputArray[q - g] >> 3);
            }
        else
            for (q = maxOrder; q < numberOfElements; q++) {
                outputArray[q] = inputArray[q] - (outputArray[q - g] >> 3);
            }
    }
}
