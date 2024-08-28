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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;


/**
 * @author Dmitry Vaguine
 * @version 12.03.2004 13:35:13
 */
public class RandomAccessFile extends File {

    private final java.io.RandomAccessFile file;
    private final java.io.File f;
    private long markPosition = -1;

    public RandomAccessFile(URL url, String mode) throws URISyntaxException, IOException {
        this(new java.io.File(url.toURI()), mode);
    }

    public RandomAccessFile(java.io.File file, String mode) throws FileNotFoundException {
        this.f = file;
        this.file = new java.io.RandomAccessFile(file, mode);
    }

    @Override
    public void mark(int readlimit) throws IOException {
        markPosition = file.getFilePointer();
    }

    @Override
    public void reset() throws IOException {
        if (markPosition >= 0)
            file.seek(markPosition);
    }

    @Override
    public int read() throws IOException {
        return file.read();
    }

    @Override
    public short readShortBack() throws IOException {
        return (short) (read() | (read() << 8));
    }

    @Override
    public int readIntBack() throws IOException {
        return read() | (read() << 8) | (read() << 16) | (read() << 24);
    }

    @Override
    public long readLongBack() throws IOException {
        return read() |
                ((long) read() << 8) |
                ((long) read() << 16) |
                ((long) read() << 24) |
                ((long) read() << 32) |
                ((long) read() << 40) |
                ((long) read() << 48) |
                ((long) read() << 56);
    }

    @Override
    public int read(byte[] b) throws IOException {
        return file.read(b);
    }

    @Override
    public int read(byte[] b, int offs, int len) throws IOException {
        return file.read(b, offs, len);
    }

    @Override
    public void readFully(byte[] b) throws IOException {
        file.readFully(b);
    }

    @Override
    public void readFully(byte[] b, int offs, int len) throws IOException {
        file.readFully(b, offs, len);
    }

    @Override
    public void close() throws IOException {
        file.close();
    }

    @Override
    public boolean readBoolean() throws IOException {
        return file.readBoolean();
    }

    @Override
    public byte readByte() throws IOException {
        return file.readByte();
    }

    @Override
    public char readChar() throws IOException {
        return file.readChar();
    }

    @Override
    public double readDouble() throws IOException {
        return file.readDouble();
    }

    @Override
    public float readFloat() throws IOException {
        return file.readFloat();
    }

    @Override
    public int readInt() throws IOException {
        return file.readInt();
    }

    @Override
    public String readLine() throws IOException {
        return file.readLine();
    }

    @Override
    public long readLong() throws IOException {
        return file.readLong();
    }

    @Override
    public short readShort() throws IOException {
        return file.readShort();
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return file.readUnsignedByte();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return file.readUnsignedShort();
    }

    @Override
    public String readUTF() throws IOException {
        return file.readUTF();
    }

    @Override
    public int skipBytes(int n) throws IOException {
        return file.skipBytes(n);
    }

    @Override
    public long length() throws IOException {
        return file.length();
    }

    @Override
    public void seek(long pos) throws IOException {
        file.seek(pos);
    }

    @Override
    public long getFilePointer() throws IOException {
        return file.getFilePointer();
    }

    @Override
    public void setLength(long newLength) throws IOException {
        file.setLength(newLength);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        file.write(b, off, len);
    }

    @Override
    public boolean isLocal() {
        return true;
    }

    @Override
    public String getFilename() {
        return f.getName();
    }
}
