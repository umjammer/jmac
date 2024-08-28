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

package davaguine.jmac.info;

import java.io.IOException;

import davaguine.jmac.tools.ByteBuffer;
import davaguine.jmac.tools.File;
import davaguine.jmac.tools.IntegerPointer;
import davaguine.jmac.tools.JMACException;


/**
 * @author Dmitry Vaguine
 * @version 07.05.2004 14:07:31
 */
public abstract class InputSource {

    // construction / destruction

    public InputSource(File io, WaveFormat wfeSource, IntegerPointer totalBlocks, IntegerPointer headerBytes, IntegerPointer terminatingBytes) throws IOException {
    }

    public InputSource(String sourceName, WaveFormat wfeSource, IntegerPointer totalBlocks, IntegerPointer headerBytes, IntegerPointer terminatingBytes) throws IOException {
    }

    // get data

    public abstract int getData(ByteBuffer buffer, int blocks) throws IOException;

    // get header / terminating data

    public abstract void getHeaderData(byte[] buffer) throws IOException;

    public abstract void getTerminatingData(byte[] buffer) throws IOException;

    public abstract void close() throws IOException;

    public static InputSource createInputSource(String sourceName, WaveFormat wfeSource, IntegerPointer totalBlocks, IntegerPointer headerBytes, IntegerPointer terminatingBytes) throws IOException {
        // error check the parameters
        if ((sourceName == null) || (sourceName.isEmpty()))
            throw new JMACException("Bad Parameters");

        // get the extension
        int index = sourceName.lastIndexOf('.');
        String extension = "";
        if (index >= 0)
            extension = sourceName.substring(sourceName.lastIndexOf('.'));

        // create the proper input source
        if (extension.equalsIgnoreCase(".wav")) {
            return new WAVInputSource(sourceName, wfeSource, totalBlocks, headerBytes, terminatingBytes);
        } else
            throw new JMACException("Invalid Input File");
    }
}
