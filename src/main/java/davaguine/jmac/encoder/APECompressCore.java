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

package davaguine.jmac.encoder;

import java.io.IOException;

import davaguine.jmac.info.SpecialFrame;
import davaguine.jmac.info.WaveFormat;
import davaguine.jmac.prediction.IPredictorCompress;
import davaguine.jmac.prediction.PredictorCompressNormal;
import davaguine.jmac.tools.ByteArrayReader;
import davaguine.jmac.tools.Crc32;
import davaguine.jmac.tools.File;
import davaguine.jmac.tools.IntegerPointer;
import davaguine.jmac.tools.Prepare;


/**
 * @author Dmitry Vaguine
 * @version 08.05.2004 11:18:47
 */
public class APECompressCore {

    public APECompressCore(File io, WaveFormat wfeInput, int maxFrameBlocks, int compressionLevel) {
        bitArray = new BitArray(io);
        dataX = new int[maxFrameBlocks];
        dataY = new int[maxFrameBlocks];
        predictorX = new PredictorCompressNormal(compressionLevel);
        predictorY = new PredictorCompressNormal(compressionLevel);

        this.wfeInput = wfeInput;
        peakLevel.value = 0;
    }

    private final IntegerPointer specialCodes = new IntegerPointer();

    public void encodeFrame(ByteArrayReader inputData, int inputBytes) throws IOException {
        // variables
        int inputBlocks = inputBytes / wfeInput.blockAlign;
        specialCodes.value = 0;

        // always start a new frame on a byte boundary
        bitArray.advanceToByteBoundary();

        // do the preparation stage
        prepare(inputData, inputBytes, specialCodes);

        predictorX.flush();
        predictorY.flush();

        bitArray.flushState(bitArrayStateX);
        bitArray.flushState(bitArrayStateY);

        bitArray.flushBitArray();

        if (wfeInput.channels == 2) {
            boolean encodeX = true;
            boolean encodeY = true;

            if ((specialCodes.value & SpecialFrame.SPECIAL_FRAME_LEFT_SILENCE) > 0 &&
                    (specialCodes.value & SpecialFrame.SPECIAL_FRAME_RIGHT_SILENCE) > 0) {
                encodeX = false;
                encodeY = false;
            }

            if ((specialCodes.value & SpecialFrame.SPECIAL_FRAME_PSEUDO_STEREO) > 0) {
                encodeY = false;
            }

            if (encodeX && encodeY) {
                int lastX = 0;
                for (int z = 0; z < inputBlocks; z++) {
                    bitArray.encodeValue(predictorY.compressValue(dataY[z], lastX), bitArrayStateY);
                    bitArray.encodeValue(predictorX.compressValue(dataX[z], dataY[z]), bitArrayStateX);

                    lastX = dataX[z];
                }
            } else if (encodeX) {
                for (int z = 0; z < inputBlocks; z++) {
                    bitArray.encodeValue(predictorX.compressValue(dataX[z]), bitArrayStateX);
                }
            } else if (encodeY) {
                for (int z = 0; z < inputBlocks; z++) {
                    bitArray.encodeValue(predictorY.compressValue(dataY[z]), bitArrayStateY);
                }
            }
        } else if (wfeInput.channels == 1) {
            if ((specialCodes.value & SpecialFrame.SPECIAL_FRAME_MONO_SILENCE) <= 0) {
                for (int z = 0; z < inputBlocks; z++) {
                    bitArray.encodeValue(predictorX.compressValue(dataX[z]), bitArrayStateX);
                }
            }
        }

        bitArray.finalize_();
    }

    public BitArray getBitArray() {
        return bitArray;
    }

    public int getPeakLevel() {
        return peakLevel.value;
    }

    private final BitArray bitArray;
    private final IPredictorCompress predictorX;
    private final IPredictorCompress predictorY;

    private final BitArrayState bitArrayStateX = new BitArrayState();
    private final BitArrayState bitArrayStateY = new BitArrayState();

    private final int[] dataX;
    private final int[] dataY;

    private final WaveFormat wfeInput;

    private final IntegerPointer peakLevel = new IntegerPointer();

    private final Crc32 crc = new Crc32();

    private void prepare(ByteArrayReader inputData, int inputBytes, IntegerPointer specialCodes) throws IOException {
        // variable declares
        specialCodes.value = 0;

        // do the preparation
        Prepare.prepare(inputData, inputBytes, wfeInput, dataX, dataY,
                crc, specialCodes, peakLevel);

        // store the CRC
        bitArray.encodeUnsignedLong(crc.getCrc());

        // store any special codes
        if (specialCodes.value != 0)
            bitArray.encodeUnsignedLong(specialCodes.value);
    }
}
