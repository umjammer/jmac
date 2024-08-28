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

import java.io.IOException;
import java.util.Arrays;

import davaguine.jmac.info.CompressionLevel;
import davaguine.jmac.info.SpecialFrame;
import davaguine.jmac.tools.JMACException;


/**
 * @author Dmitry Vaguine
 * @version 04.03.2004 14:51:31
 */
public class APEDecompressCore {

    public APEDecompressCore(IAPEDecompress apeDecompress) {
        this.apeDecompress = apeDecompress;

        // initialize the bit array
        unBitArray = UnBitArrayBase.createUnBitArray(apeDecompress, apeDecompress.getApeInfoFileVersion());

        if (apeDecompress.getApeInfoFileVersion() >= 3930)
            throw new JMACException("Wrong Version");

        antiPredictorX = AntiPredictor.createAntiPredictor(apeDecompress.getApeInfoCompressionLevel(), apeDecompress.getApeInfoFileVersion());
        antiPredictorY = AntiPredictor.createAntiPredictor(apeDecompress.getApeInfoCompressionLevel(), apeDecompress.getApeInfoFileVersion());

        dataX = new int[apeDecompress.getApeInfoBlocksPerFrame() + 16];
        dataY = new int[apeDecompress.getApeInfoBlocksPerFrame() + 16];
        tempData = new int[apeDecompress.getApeInfoBlocksPerFrame() + 16];

        blocksProcessed = 0;
    }

    public void generateDecodedArrays(int blocks, int specialCodes, int frameIndex) throws IOException {
        if (apeDecompress.getApeInfoChannels() == 2) {
            if ((specialCodes & SpecialFrame.SPECIAL_FRAME_LEFT_SILENCE) > 0 && (specialCodes & SpecialFrame.SPECIAL_FRAME_RIGHT_SILENCE) > 0) {
                Arrays.fill(dataX, 0, blocks, 0);
                Arrays.fill(dataY, 0, blocks, 0);
            } else if ((specialCodes & SpecialFrame.SPECIAL_FRAME_PSEUDO_STEREO) > 0) {
                generateDecodedArray(dataX, blocks, frameIndex, antiPredictorX);
                Arrays.fill(dataY, 0, blocks, 0);
            } else {
                generateDecodedArray(dataX, blocks, frameIndex, antiPredictorX);
                generateDecodedArray(dataY, blocks, frameIndex, antiPredictorY);
            }
        } else {
            if ((specialCodes & SpecialFrame.SPECIAL_FRAME_LEFT_SILENCE) > 0)
                Arrays.fill(dataX, 0, blocks, 0);
            else
                generateDecodedArray(dataX, blocks, frameIndex, antiPredictorX);
        }
    }

    public void generateDecodedArray(int[] inputArray, int numberOfElements, int frameIndex, AntiPredictor antiPredictor) throws IOException {
        int frameBytes = apeDecompress.getApeInfoFrameBytes(frameIndex);

        // run the prediction sequence
        switch (apeDecompress.getApeInfoCompressionLevel()) {

            case CompressionLevel.COMPRESSION_LEVEL_FAST:
                if (apeDecompress.getApeInfoFileVersion() < 3320) {
                    unBitArray.generateArray(tempData, numberOfElements, frameBytes);
                    antiPredictor.antiPredict(tempData, inputArray, numberOfElements);
                } else {
                    unBitArray.generateArray(inputArray, numberOfElements, frameBytes);
                    antiPredictor.antiPredict(inputArray, null, numberOfElements);
                }

                break;

            case CompressionLevel.COMPRESSION_LEVEL_NORMAL: {
                // get the array from the bitstream
                unBitArray.generateArray(tempData, numberOfElements, frameBytes);
                antiPredictor.antiPredict(tempData, inputArray, numberOfElements);
                break;
            }

            case CompressionLevel.COMPRESSION_LEVEL_HIGH:
                // get the array from the bitstream
                unBitArray.generateArray(tempData, numberOfElements, frameBytes);
                antiPredictor.antiPredict(tempData, inputArray, numberOfElements);
                break;

            case CompressionLevel.COMPRESSION_LEVEL_EXTRA_HIGH:
                long numberOfCoefficients;

                if (apeDecompress.getApeInfoFileVersion() < 3320) {
                    numberOfCoefficients = unBitArray.decodeValue(DecodeValueMethod.DECODE_VALUE_METHOD_X_BITS, 4);
                    for (int z = 0; z <= numberOfCoefficients; z++) {
                        coefficientsA[z] = unBitArray.decodeValue(DecodeValueMethod.DECODE_VALUE_METHOD_X_BITS, 6);
                        coefficientsB[z] = unBitArray.decodeValue(DecodeValueMethod.DECODE_VALUE_METHOD_X_BITS, 6);
                    }
                    unBitArray.generateArray(tempData, numberOfElements, frameBytes);
                    ((AntiPredictorExtraHigh0000To3320) antiPredictor).antiPredict(tempData, inputArray, numberOfElements, (int) numberOfCoefficients, coefficientsA, coefficientsB);
                } else if (apeDecompress.getApeInfoFileVersion() < 3600) {
                    numberOfCoefficients = unBitArray.decodeValue(DecodeValueMethod.DECODE_VALUE_METHOD_X_BITS, 3);
                    for (int z = 0; z <= numberOfCoefficients; z++) {
                        coefficientsA[z] = unBitArray.decodeValue(DecodeValueMethod.DECODE_VALUE_METHOD_X_BITS, 5);
                        coefficientsB[z] = unBitArray.decodeValue(DecodeValueMethod.DECODE_VALUE_METHOD_X_BITS, 5);
                    }
                    unBitArray.generateArray(tempData, numberOfElements, frameBytes);
                    ((AntiPredictorExtraHigh3320To3600) antiPredictor).antiPredict(tempData, inputArray, numberOfElements, (int) numberOfCoefficients, coefficientsA, coefficientsB);
                } else if (apeDecompress.getApeInfoFileVersion() < 3700) {
                    numberOfCoefficients = unBitArray.decodeValue(DecodeValueMethod.DECODE_VALUE_METHOD_X_BITS, 3);
                    for (int z = 0; z <= numberOfCoefficients; z++) {
                        coefficientsA[z] = unBitArray.decodeValue(DecodeValueMethod.DECODE_VALUE_METHOD_X_BITS, 6);
                        coefficientsB[z] = unBitArray.decodeValue(DecodeValueMethod.DECODE_VALUE_METHOD_X_BITS, 6);
                    }
                    unBitArray.generateArray(tempData, numberOfElements, frameBytes);
                    ((AntiPredictorExtraHigh3600To3700) antiPredictor).antiPredict(tempData, inputArray, numberOfElements, (int) numberOfCoefficients, coefficientsA, coefficientsB);
                } else if (apeDecompress.getApeInfoFileVersion() < 3800) {
                    numberOfCoefficients = unBitArray.decodeValue(DecodeValueMethod.DECODE_VALUE_METHOD_X_BITS, 3);
                    for (int z = 0; z <= numberOfCoefficients; z++) {
                        coefficientsA[z] = unBitArray.decodeValue(DecodeValueMethod.DECODE_VALUE_METHOD_X_BITS, 6);
                        coefficientsB[z] = unBitArray.decodeValue(DecodeValueMethod.DECODE_VALUE_METHOD_X_BITS, 6);
                    }
                    unBitArray.generateArray(tempData, numberOfElements, frameBytes);
                    ((AntiPredictorExtraHigh3700To3800) antiPredictor).antiPredict(tempData, inputArray, numberOfElements, (int) numberOfCoefficients, coefficientsA, coefficientsB);
                } else {
                    unBitArray.generateArray(tempData, numberOfElements, frameBytes);
                    ((AntiPredictorExtraHigh3800ToCurrent) antiPredictor).antiPredict(tempData, inputArray, numberOfElements, apeDecompress.getApeInfoFileVersion());
                }

                break;
        }
    }

    public UnBitArrayBase getUnBitArray() {
        return unBitArray;
    }

    private final long[] coefficientsA = new long[64];
    private final long[] coefficientsB = new long[64];

    public int[] tempData;
    public int[] dataX;
    public int[] dataY;

    public AntiPredictor antiPredictorX;
    public AntiPredictor antiPredictorY;

    public final UnBitArrayBase unBitArray;
    public UnBitArrayState bitArrayStateX = new UnBitArrayState();
    public UnBitArrayState bitArrayStateY = new UnBitArrayState();

    public final IAPEDecompress apeDecompress;

    public int blocksProcessed;
}
