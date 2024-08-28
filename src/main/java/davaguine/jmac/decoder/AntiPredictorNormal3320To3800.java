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
public class AntiPredictorNormal3320To3800 extends AntiPredictor {

    @Override
    public void antiPredict(int[] inputArray, int[] outputArray, int numberOfElements) {
        // variable declares
        int q;

        // short frame handling
        if (numberOfElements < 8) {
            System.arraycopy(inputArray, 0, outputArray, 0, numberOfElements);
            return;
        }

        // make the first five samples identical in both arrays
        System.arraycopy(inputArray, 0, outputArray, 0, 5);

        // initialize values
        int m1 = 0;
        int m2 = 64;
        int m3 = 28;
        int op0;

        int p3 = (3 * (outputArray[4] - outputArray[3])) + outputArray[2];
        int p2 = inputArray[4] + ((inputArray[2] - inputArray[3]) << 3) - inputArray[1] + inputArray[0];
        int p1 = outputArray[4];

        for (q = 5; q < numberOfElements; q++) {
            op0 = inputArray[q] + ((p1 * m1) >> 8);
            if ((inputArray[q] ^ p1) > 0)
                m1++;
            else
                m1--;
            p1 = op0;

            inputArray[q] = op0 + ((p2 * m2) >> 11);
            if ((op0 ^ p2) > 0)
                m2++;
            else
                m2--;
            p2 = inputArray[q] + ((inputArray[q - 2] - inputArray[q - 1]) << 3) - inputArray[q - 3] + inputArray[q - 4];

            outputArray[q] = inputArray[q] + ((p3 * m3) >> 9);
            if ((inputArray[q] ^ p3) > 0)
                m3++;
            else
                m3--;
            p3 = (3 * (outputArray[q] - outputArray[q - 1])) + outputArray[q - 2];
        }

        int m4 = 370;
        int m5 = 3900;

//        outputArray[0] = inputArray[0];
        outputArray[1] = inputArray[1] + outputArray[0];
        outputArray[2] = inputArray[2] + outputArray[1];
        outputArray[3] = inputArray[3] + outputArray[2];
        outputArray[4] = inputArray[4] + outputArray[3];

        int p4 = (2 * inputArray[4]) - inputArray[3];
        int p5 = outputArray[4];
        int ip0, ip1;

        ip1 = inputArray[4];
        for (q = 5; q < numberOfElements; q++) {
            ip0 = outputArray[q] + ((p4 * m4) >> 9);
            if ((outputArray[q] ^ p4) > 0)
                m4++;
            else
                m4--;
            p4 = (2 * ip0) - ip1;

            outputArray[q] = ip0 + ((p5 * m5) >> 12);
            if ((ip0 ^ p5) > 0)
                m5++;
            else
                m5--;
            p5 = outputArray[q];

            ip1 = ip0;
        }
    }
}
