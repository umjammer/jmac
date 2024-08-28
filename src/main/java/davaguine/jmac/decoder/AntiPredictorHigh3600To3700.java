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
public class AntiPredictorHigh3600To3700 extends AntiPredictor {

    @Override
    public void antiPredict(int[] inputArray, int[] outputArray, int numberOfElements) {
        // variable declares
        int q;

        // short frame handling
        if (numberOfElements < 16) {
            System.arraycopy(inputArray, 0, outputArray, 0, numberOfElements);
            return;
        }

        // make the first five samples identical in both arrays
        System.arraycopy(inputArray, 0, outputArray, 0, 13);

        // initialize values
        int bm1 = 0;
        int bm2 = 0;
        int bm3 = 0;
        int bm4 = 0;
        int bm5 = 0;
        int bm6 = 0;
        int bm7 = 0;
        int bm8 = 0;
        int bm9 = 0;
        int bm10 = 0;
        int bm11 = 0;
        int bm12 = 0;
        int bm13 = 0;


        int m2 = 64;

        int m3 = 28;
        int m4 = 16;
        int OP0;
        int p4 = inputArray[12];
        int p3 = (inputArray[12] - inputArray[11]) << 1;
        int p2 = inputArray[12] + ((inputArray[10] - inputArray[11]) << 3);
        int bp1 = outputArray[12];
        int bp2 = outputArray[11];
        int bp3 = outputArray[10];
        int bp4 = outputArray[9];
        int bp5 = outputArray[8];
        int bp6 = outputArray[7];
        int bp7 = outputArray[6];
        int bp8 = outputArray[5];
        int bp9 = outputArray[4];
        int bp10 = outputArray[3];
        int bp11 = outputArray[2];
        int bp12 = outputArray[1];
        int bp13 = outputArray[0];

        for (q = 13; q < numberOfElements; q++) {
            inputArray[q] = inputArray[q] - 1;
            OP0 = (inputArray[q] - ((bp1 * bm1) >> 8) + ((bp2 * bm2) >> 8) - ((bp3 * bm3) >> 8) - ((bp4 * bm4) >> 8) - ((bp5 * bm5) >> 8) - ((bp6 * bm6) >> 8) - ((bp7 * bm7) >> 8) - ((bp8 * bm8) >> 8) - ((bp9 * bm9) >> 8) + ((bp10 * bm10) >> 8) + ((bp11 * bm11) >> 8) + ((bp12 * bm12) >> 8) + ((bp13 * bm13) >> 8));

            if (inputArray[q] > 0) {
                bm1 -= bp1 > 0 ? 1 : -1;
                bm2 += bp2 >= 0 ? 1 : -1;
                bm3 -= bp3 > 0 ? 1 : -1;
                bm4 -= bp4 >= 0 ? 1 : -1;
                bm5 -= bp5 > 0 ? 1 : -1;
                bm6 -= bp6 >= 0 ? 1 : -1;
                bm7 -= bp7 > 0 ? 1 : -1;
                bm8 -= bp8 >= 0 ? 1 : -1;
                bm9 -= bp9 > 0 ? 1 : -1;
                bm10 += bp10 >= 0 ? 1 : -1;
                bm11 += bp11 > 0 ? 1 : -1;
                bm12 += bp12 >= 0 ? 1 : -1;
                bm13 += bp13 > 0 ? 1 : -1;

            } else if (inputArray[q] < 0) {
                bm1 -= bp1 <= 0 ? 1 : -1;
                bm2 += bp2 < 0 ? 1 : -1;
                bm3 -= bp3 <= 0 ? 1 : -1;
                bm4 -= bp4 < 0 ? 1 : -1;
                bm5 -= bp5 <= 0 ? 1 : -1;
                bm6 -= bp6 < 0 ? 1 : -1;
                bm7 -= bp7 <= 0 ? 1 : -1;
                bm8 -= bp8 < 0 ? 1 : -1;
                bm9 -= bp9 <= 0 ? 1 : -1;
                bm10 += bp10 < 0 ? 1 : -1;
                bm11 += bp11 <= 0 ? 1 : -1;
                bm12 += bp12 < 0 ? 1 : -1;
                bm13 += bp13 <= 0 ? 1 : -1;

            }

            bp13 = bp12;
            bp12 = bp11;
            bp11 = bp10;
            bp10 = bp9;
            bp9 = bp8;
            bp8 = bp7;
            bp7 = bp6;
            bp6 = bp5;
            bp5 = bp4;
            bp4 = bp3;
            bp3 = bp2;
            bp2 = bp1;
            bp1 = OP0;

            inputArray[q] = OP0 + ((p2 * m2) >> 11) + ((p3 * m3) >> 9) + ((p4 * m4) >> 9);

            if (OP0 > 0) {
                m2 -= p2 > 0 ? -1 : 1;
                m3 -= p3 > 0 ? -1 : 1;
                m4 -= p4 > 0 ? -1 : 1;
            } else if (OP0 < 0) {
                m2 -= p2 > 0 ? 1 : -1;
                m3 -= p3 > 0 ? 1 : -1;
                m4 -= p4 > 0 ? 1 : -1;
            }

            p2 = inputArray[q] + ((inputArray[q - 2] - inputArray[q - 1]) << 3);
            p3 = (inputArray[q] - inputArray[q - 1]) << 1;
            p4 = inputArray[q];

            outputArray[q] = inputArray[q];
        }

        m4 = 370;

        outputArray[1] = inputArray[1] + outputArray[0];
        outputArray[2] = inputArray[2] + outputArray[1];
        outputArray[3] = inputArray[3] + outputArray[2];
        outputArray[4] = inputArray[4] + outputArray[3];
        outputArray[5] = inputArray[5] + outputArray[4];
        outputArray[6] = inputArray[6] + outputArray[5];
        outputArray[7] = inputArray[7] + outputArray[6];
        outputArray[8] = inputArray[8] + outputArray[7];
        outputArray[9] = inputArray[9] + outputArray[8];
        outputArray[10] = inputArray[10] + outputArray[9];
        outputArray[11] = inputArray[11] + outputArray[10];
        outputArray[12] = inputArray[12] + outputArray[11];

        p4 = (2 * inputArray[12]) - inputArray[11];
        int p6 = 0;
        int p5 = outputArray[12];
        int ip0, ip1;
        int m6 = 0;

        ip1 = inputArray[12];
        for (q = 13; q < numberOfElements; q++) {
            ip0 = outputArray[q] + ((p4 * m4) >> 9) - ((p6 * m6) >> 10);
            if ((outputArray[q] ^ p4) >= 0)
                m4++;
            else
                m4--;
            if ((outputArray[q] ^ p6) >= 0)
                m6--;
            else
                m6++;
            p4 = (2 * ip0) - ip1;
            p6 = ip0;

            outputArray[q] = ip0 + ((p5 * 31) >> 5);
            p5 = outputArray[q];

            ip1 = ip0;
        }
    }
}
