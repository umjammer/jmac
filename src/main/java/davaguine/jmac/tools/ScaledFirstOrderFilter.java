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

package davaguine.jmac.tools;

/**
 * @author Dmitry Vaguine
 * @version 04.03.2004 14:51:31
 */
public class ScaledFirstOrderFilter {

    public ScaledFirstOrderFilter(int multiply, int shift) {
        this.multiply = multiply;
        this.shift = shift;
    }

    public void flush() {
        lastValue = 0;
    }

    public int compress(int input) {
        int retVal = input - ((lastValue * multiply) >> shift);
        lastValue = input;
        return retVal;
    }

    public int Decompress(int input) {
        lastValue = input + ((lastValue * multiply) >> shift);
        return lastValue;
    }

    protected int lastValue;
    protected final int multiply;
    protected final int shift;
}
