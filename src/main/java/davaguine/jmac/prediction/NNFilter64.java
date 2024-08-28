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

package davaguine.jmac.prediction;


/**
 * @author Dmitry Vaguine
 * @version 04.03.2004 14:51:31
 */
public class NNFilter64 extends NNFilter {

    public NNFilter64(int shift, int version) {
        super(64, shift, version);
        orderPlusWindow = 576 /* NN_WINDOW_ELEMENTS + order */;
    }

    @Override
    protected int calculateDotProductNoMMX(short[] a, int indexA, short[] b, int indexB) {
        int dotProduct = 0;
        for (int i = 0; i < 2; i++) {
            dotProduct += (a[indexA++] * b[indexB++]) +
                    (a[indexA++] * b[indexB++]) +
                    (a[indexA++] * b[indexB++]) +
                    (a[indexA++] * b[indexB++]) +
                    (a[indexA++] * b[indexB++]) +
                    (a[indexA++] * b[indexB++]) +
                    (a[indexA++] * b[indexB++]) +
                    (a[indexA++] * b[indexB++]) +
                    (a[indexA++] * b[indexB++]) +
                    (a[indexA++] * b[indexB++]) +
                    (a[indexA++] * b[indexB++]) +
                    (a[indexA++] * b[indexB++]) +
                    (a[indexA++] * b[indexB++]) +
                    (a[indexA++] * b[indexB++]) +
                    (a[indexA++] * b[indexB++]) +
                    (a[indexA++] * b[indexB++]) +
                    (a[indexA++] * b[indexB++]) +
                    (a[indexA++] * b[indexB++]) +
                    (a[indexA++] * b[indexB++]) +
                    (a[indexA++] * b[indexB++]) +
                    (a[indexA++] * b[indexB++]) +
                    (a[indexA++] * b[indexB++]) +
                    (a[indexA++] * b[indexB++]) +
                    (a[indexA++] * b[indexB++]) +
                    (a[indexA++] * b[indexB++]) +
                    (a[indexA++] * b[indexB++]) +
                    (a[indexA++] * b[indexB++]) +
                    (a[indexA++] * b[indexB++]) +
                    (a[indexA++] * b[indexB++]) +
                    (a[indexA++] * b[indexB++]) +
                    (a[indexA++] * b[indexB++]) +
                    (a[indexA++] * b[indexB++]);
        }
        return dotProduct;
    }

    @Override
    protected void adaptNoMMX(short[] m, int indexM, short[] adapt, int indexA, int direction) {
        if (direction < 0) {
            for (int i = 0; i < 2; i++) {
                m[indexM++] += adapt[indexA++];
                m[indexM++] += adapt[indexA++];
                m[indexM++] += adapt[indexA++];
                m[indexM++] += adapt[indexA++];
                m[indexM++] += adapt[indexA++];
                m[indexM++] += adapt[indexA++];
                m[indexM++] += adapt[indexA++];
                m[indexM++] += adapt[indexA++];
                m[indexM++] += adapt[indexA++];
                m[indexM++] += adapt[indexA++];
                m[indexM++] += adapt[indexA++];
                m[indexM++] += adapt[indexA++];
                m[indexM++] += adapt[indexA++];
                m[indexM++] += adapt[indexA++];
                m[indexM++] += adapt[indexA++];
                m[indexM++] += adapt[indexA++];
                m[indexM++] += adapt[indexA++];
                m[indexM++] += adapt[indexA++];
                m[indexM++] += adapt[indexA++];
                m[indexM++] += adapt[indexA++];
                m[indexM++] += adapt[indexA++];
                m[indexM++] += adapt[indexA++];
                m[indexM++] += adapt[indexA++];
                m[indexM++] += adapt[indexA++];
                m[indexM++] += adapt[indexA++];
                m[indexM++] += adapt[indexA++];
                m[indexM++] += adapt[indexA++];
                m[indexM++] += adapt[indexA++];
                m[indexM++] += adapt[indexA++];
                m[indexM++] += adapt[indexA++];
                m[indexM++] += adapt[indexA++];
                m[indexM++] += adapt[indexA++];
            }
        } else if (direction > 0) {
            for (int i = 0; i < 2; i++) {
                m[indexM++] -= adapt[indexA++];
                m[indexM++] -= adapt[indexA++];
                m[indexM++] -= adapt[indexA++];
                m[indexM++] -= adapt[indexA++];
                m[indexM++] -= adapt[indexA++];
                m[indexM++] -= adapt[indexA++];
                m[indexM++] -= adapt[indexA++];
                m[indexM++] -= adapt[indexA++];
                m[indexM++] -= adapt[indexA++];
                m[indexM++] -= adapt[indexA++];
                m[indexM++] -= adapt[indexA++];
                m[indexM++] -= adapt[indexA++];
                m[indexM++] -= adapt[indexA++];
                m[indexM++] -= adapt[indexA++];
                m[indexM++] -= adapt[indexA++];
                m[indexM++] -= adapt[indexA++];
                m[indexM++] -= adapt[indexA++];
                m[indexM++] -= adapt[indexA++];
                m[indexM++] -= adapt[indexA++];
                m[indexM++] -= adapt[indexA++];
                m[indexM++] -= adapt[indexA++];
                m[indexM++] -= adapt[indexA++];
                m[indexM++] -= adapt[indexA++];
                m[indexM++] -= adapt[indexA++];
                m[indexM++] -= adapt[indexA++];
                m[indexM++] -= adapt[indexA++];
                m[indexM++] -= adapt[indexA++];
                m[indexM++] -= adapt[indexA++];
                m[indexM++] -= adapt[indexA++];
                m[indexM++] -= adapt[indexA++];
                m[indexM++] -= adapt[indexA++];
                m[indexM++] -= adapt[indexA++];
            }
        }
    }
}
