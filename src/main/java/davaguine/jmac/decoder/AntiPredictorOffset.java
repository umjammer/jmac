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
public class AntiPredictorOffset extends AntiPredictor {

    public static void antiPredict(int[] inputArray, int[] outputArray, int numberOfElements, int offset, int deltaM) {
        System.arraycopy(inputArray, 0, outputArray, 0, offset);

        int ip = offset;
        int ipo = 0;
        int op = offset;
        int m = 0;

        for (; op < numberOfElements; ip++, ipo++, op++) {
            outputArray[op] = inputArray[ip] + ((outputArray[ipo] * m) >> 12);

            if ((outputArray[ipo] ^ inputArray[ip]) > 0)
                m += deltaM;
            else
                m -= deltaM;
        }
    }
}
