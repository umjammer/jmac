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
public class AntiPredictorHigh3320To3600 extends AntiPredictor {

    @Override
    public void antiPredict(int[] inputArray, int[] outputArray, int numberOfElements) {
        // short frame handling
        if (numberOfElements < 8) {
            System.arraycopy(inputArray, 0, outputArray, 0, numberOfElements);
            return;
        }

        // do the offset anti-prediction
        davaguine.jmac.decoder.AntiPredictorOffset.antiPredict(inputArray, outputArray, numberOfElements, 2, 12);
        davaguine.jmac.decoder.AntiPredictorOffset.antiPredict(outputArray, inputArray, numberOfElements, 3, 12);

        davaguine.jmac.decoder.AntiPredictorOffset.antiPredict(inputArray, outputArray, numberOfElements, 4, 12);
        davaguine.jmac.decoder.AntiPredictorOffset.antiPredict(outputArray, inputArray, numberOfElements, 5, 12);

        davaguine.jmac.decoder.AntiPredictorOffset.antiPredict(inputArray, outputArray, numberOfElements, 6, 12);
        davaguine.jmac.decoder.AntiPredictorOffset.antiPredict(outputArray, inputArray, numberOfElements, 7, 12);


        // use the normal mode
        antiPredictor.antiPredict(inputArray, outputArray, numberOfElements);
    }

    private final AntiPredictorOffset antiPredictorOffset = new AntiPredictorOffset();
    private final AntiPredictorNormal3320To3800 antiPredictor = new AntiPredictorNormal3320To3800();
}
