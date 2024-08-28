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
public class RollBufferFastInt {

    public RollBufferFastInt(int windowElements, int historyElements) {
        data = new int[windowElements + historyElements];
        this.windowElements = windowElements;
        this.historyElements = historyElements;
        windowPlusHistory = windowElements + historyElements;
        flush();
    }

    public void flush() {
        Arrays.fill(data, 0, historyElements, 0);
        index = historyElements;
    }

    public void roll() {
        int[] ai;
        int i;
        System.arraycopy(ai = data, index - (i = historyElements), ai, 0, i);
        index = i;
    }

    public void incrementSafe() {
        if ((++index) == windowPlusHistory)
            roll();
    }

    public final int[] data;
    public int index;
    protected final int historyElements;
    protected final int windowElements;
    protected final int windowPlusHistory;
}
