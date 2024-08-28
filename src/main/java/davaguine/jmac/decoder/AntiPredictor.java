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

import davaguine.jmac.info.CompressionLevel;


/**
 * @author Dmitry Vaguine
 * @version 04.03.2004 14:51:31
 */
public class AntiPredictor {

    // construction/destruction

    public AntiPredictor() {
    }

    // functions

    public void antiPredict(int[] inputArray, int[] outputArray, int numberOfElements) {
    }

    public static AntiPredictor createAntiPredictor(int compressionLevel, int version) {
        AntiPredictor antiPredictor = null;

        switch (compressionLevel) {
            case CompressionLevel.COMPRESSION_LEVEL_FAST:
                if (version < 3320)
                    antiPredictor = new AntiPredictorFast0000To3320();
                else
                    antiPredictor = new AntiPredictorFast3320ToCurrent();
                break;

            case CompressionLevel.COMPRESSION_LEVEL_NORMAL:
                if (version < 3320)
                    antiPredictor = new AntiPredictorNormal0000To3320();
                else if (version < 3800)
                    antiPredictor = new AntiPredictorNormal3320To3800();
                else
                    antiPredictor = new AntiPredictorNormal3800ToCurrent();
                break;

            case CompressionLevel.COMPRESSION_LEVEL_HIGH:
                if (version < 3320)
                    antiPredictor = new AntiPredictorHigh0000To3320();
                else if (version < 3600)
                    antiPredictor = new AntiPredictorHigh3320To3600();
                else if (version < 3700)
                    antiPredictor = new AntiPredictorHigh3600To3700();
                else if (version < 3800)
                    antiPredictor = new AntiPredictorHigh3700To3800();
                else
                    antiPredictor = new AntiPredictorHigh3800ToCurrent();
                break;

            case CompressionLevel.COMPRESSION_LEVEL_EXTRA_HIGH:
                if (version < 3320)
                    antiPredictor = new AntiPredictorExtraHigh0000To3320();
                else if (version < 3600)
                    antiPredictor = new AntiPredictorExtraHigh3320To3600();
                else if (version < 3700)
                    antiPredictor = new AntiPredictorExtraHigh3600To3700();
                else if (version < 3800)
                    antiPredictor = new AntiPredictorExtraHigh3700To3800();
                else
                    antiPredictor = new AntiPredictorExtraHigh3800ToCurrent();
                break;
        }

        return antiPredictor;
    }
}
