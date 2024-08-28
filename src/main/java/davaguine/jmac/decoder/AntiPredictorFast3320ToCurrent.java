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
public class AntiPredictorFast3320ToCurrent extends AntiPredictor {

    @Override
    public void antiPredict(int[] inputArray, int[] outputArray, int numberOfElements) {

        // short frame handling
        if (numberOfElements < 3) {
            return;
        }

        // variable declares
        int p;
        int m = 375;
        int ip;
        int ip2 = inputArray[1];
        int ip3 = inputArray[0];
        int op1 = inputArray[1];

        // the decompression loop (order 2 followed by order 1)
        for (ip = 2; ip < numberOfElements; ip++) {

            // make a prediction for order 2
            p = ip2 + ip2 - ip3;

            // rollback the values
            ip3 = ip2;
            ip2 = inputArray[ip] + ((p * m) >> 9);

            // adjust m for the order 2
            if ((inputArray[ip] ^ p) > 0)
                m++;
            else
                m--;

            // set the output value
            inputArray[ip] = ip2 + op1;
            op1 = inputArray[ip];
        }
    }
}
