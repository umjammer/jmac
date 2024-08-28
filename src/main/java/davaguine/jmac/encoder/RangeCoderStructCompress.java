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

/**
 * @author Dmitry Vaguine
 * @version 04.05.2004 16:44:23
 */
public class RangeCoderStructCompress {

    /** low end of interval */
    public long low;
    /** length of interval */
    public long range;
    /** bytes_to_follow resp. intermediate value */
    public long help;
    /** buffer for input / output */
    public short buffer;
}
