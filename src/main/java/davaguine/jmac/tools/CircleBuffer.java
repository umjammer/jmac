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
 * @version 06.05.2004 21:05:48
 */
public class CircleBuffer {

    // construction / destruction

    public CircleBuffer() {
        buffer = null;
        total = 0;
        head = 0;
        tail = 0;
        endCap = 0;
        maxDirectWriteBytes = 0;
    }

    // create the buffer

    public void createBuffer(int bytes, int maxDirectWriteBytes) {
        this.maxDirectWriteBytes = maxDirectWriteBytes;
        total = bytes + 1 + maxDirectWriteBytes;
        buffer = new byte[total];
        byteBuffer = new ByteBuffer();
        head = 0;
        tail = 0;
        endCap = total;
    }

    // query

    public int maxAdd() {
        int maxAdd = (tail >= head) ? (total - 1 - maxDirectWriteBytes) - (tail - head) : head - tail - 1;
        return maxAdd;
    }

    public int maxGet() {
        return (tail >= head) ? tail - head : (endCap - head) + tail;
    }

    // direct writing

    public ByteBuffer getDirectWritePointer() {
        // return a pointer to the tail -- note that it will always be safe to write
        // at least maxDirectWriteBytes since we use an end cap region
        byteBuffer.reset(buffer, tail);
        return byteBuffer;
    }

    public void updateAfterDirectWrite(int bytes) {
        // update the tail
        tail += bytes;

        // if the tail enters the "end cap" area, set the end cap and loop around
        if (tail >= (total - maxDirectWriteBytes)) {
            endCap = tail;
            tail = 0;
        }
    }

    // get data

    public int get(byte[] buffer, int index, int bytes) {
        int totalGetBytes = 0;

        if (buffer != null && bytes > 0) {
            int headBytes = Math.min(endCap - head, bytes);
            int frontBytes = bytes - headBytes;

            System.arraycopy(this.buffer, head, buffer, index, headBytes);
            totalGetBytes = headBytes;

            if (frontBytes > 0) {
                System.arraycopy(this.buffer, 0, buffer, index + headBytes, frontBytes);
                totalGetBytes += frontBytes;
            }

            removeHead(bytes);
        }

        return totalGetBytes;
    }

    // remove / empty

    public void empty() {
        head = 0;
        tail = 0;
        endCap = total;
    }

    public int removeHead(int bytes) {
        bytes = Math.min(maxGet(), bytes);
        head += bytes;
        if (head >= endCap)
            head -= endCap;
        return bytes;
    }

    public int removeTail(int bytes) {
        bytes = Math.min(maxGet(), bytes);
        tail -= bytes;
        if (tail < 0)
            tail += endCap;
        return bytes;
    }

    private int total;
    private int maxDirectWriteBytes;
    private int endCap;
    private int head;
    private int tail;
    private byte[] buffer;
    private ByteBuffer byteBuffer;
}
