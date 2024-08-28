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
public class AntiPredictorExtraHighHelper {

    static int conventionalDotProduct(short[] bip, int indexB, short[] bbm, int indexBbm, short[] adaptFactors, int indexA, int op, int numberOfIterations) {
        // dot product
        int dotProduct = 0;
        int maxBBM = numberOfIterations;

        if (op == 0) {
            int i = indexBbm;
            int j = indexB;
            while (i < (maxBBM + indexBbm)) {
                dotProduct += bip[j++] * bbm[i++];
                dotProduct += bip[j++] * bbm[i++];
                dotProduct += bip[j++] * bbm[i++];
                dotProduct += bip[j++] * bbm[i++];
                dotProduct += bip[j++] * bbm[i++];
                dotProduct += bip[j++] * bbm[i++];
                dotProduct += bip[j++] * bbm[i++];
                dotProduct += bip[j++] * bbm[i++];
                dotProduct += bip[j++] * bbm[i++];
                dotProduct += bip[j++] * bbm[i++];
                dotProduct += bip[j++] * bbm[i++];
                dotProduct += bip[j++] * bbm[i++];
                dotProduct += bip[j++] * bbm[i++];
                dotProduct += bip[j++] * bbm[i++];
                dotProduct += bip[j++] * bbm[i++];
                dotProduct += bip[j++] * bbm[i++];
                dotProduct += bip[j++] * bbm[i++];
                dotProduct += bip[j++] * bbm[i++];
                dotProduct += bip[j++] * bbm[i++];
                dotProduct += bip[j++] * bbm[i++];
                dotProduct += bip[j++] * bbm[i++];
                dotProduct += bip[j++] * bbm[i++];
                dotProduct += bip[j++] * bbm[i++];
                dotProduct += bip[j++] * bbm[i++];
                dotProduct += bip[j++] * bbm[i++];
                dotProduct += bip[j++] * bbm[i++];
                dotProduct += bip[j++] * bbm[i++];
                dotProduct += bip[j++] * bbm[i++];
                dotProduct += bip[j++] * bbm[i++];
                dotProduct += bip[j++] * bbm[i++];
                dotProduct += bip[j++] * bbm[i++];
                dotProduct += bip[j++] * bbm[i++];
            }
        } else if (op > 0) {
            int i = indexBbm;
            int j = indexB;
            int k = indexA;
            while (i < (maxBBM + indexBbm)) {
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] += adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] += adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] += adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] += adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] += adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] += adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] += adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] += adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] += adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] += adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] += adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] += adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] += adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] += adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] += adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] += adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] += adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] += adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] += adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] += adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] += adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] += adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] += adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] += adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] += adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] += adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] += adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] += adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] += adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] += adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] += adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] += adaptFactors[k++];
            }
        } else {
            int i = indexBbm;
            int j = indexB;
            int k = indexA;
            while (i < (maxBBM + indexBbm)) {
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] -= adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] -= adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] -= adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] -= adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] -= adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] -= adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] -= adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] -= adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] -= adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] -= adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] -= adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] -= adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] -= adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] -= adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] -= adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] -= adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] -= adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] -= adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] -= adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] -= adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] -= adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] -= adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] -= adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] -= adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] -= adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] -= adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] -= adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] -= adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] -= adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] -= adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] -= adaptFactors[k++];
                dotProduct += bip[j++] * bbm[i];
                bbm[i++] -= adaptFactors[k++];
            }
        }

        // use the dot product
        return dotProduct;
    }
}
