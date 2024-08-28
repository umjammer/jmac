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

import java.util.Arrays;


/**
 * @author Dmitry Vaguine
 * @version 04.03.2004 14:51:31
 */
public class RollBufferShort {

    public RollBufferShort() {
        data = null;
    }

    public int create(int windowElements, int historyElements) {
        this.windowElements = windowElements;
        this.historyElements = historyElements;
        windowPlusHistory = windowElements += historyElements;

        data = new short[windowElements];

        flush();
        return 0;
    }

    public void flush() {
        Arrays.fill(data, 0, historyElements, (short) 0);
        index = historyElements;
    }

    public void incrementSafe() {
        if ((++index) == windowPlusHistory) {
            short[] aword0;
            int i;
            System.arraycopy(aword0 = data, index - (i = historyElements), aword0, 0, i);
            index = i;
        }
    }

    public short[] data;
    public int index;

    protected int historyElements;
    protected int windowElements;
    protected int windowPlusHistory;
}
