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

package davaguine.jmac;

import davaguine.jmac.decoder.IAPEDecompress;
import davaguine.jmac.info.APEInfo;
import davaguine.jmac.info.APETag;
import davaguine.jmac.info.ID3Tag;
import davaguine.jmac.tools.File;


/**
 * @author Dmitry Vaguine
 * @version 23.04.2004 18:34:42
 */
public class ChangeTagInfo {

    public static void main(String[] args) {
        try {
            //
            // error check the command line parameters
            //
            if (args.length != 3) {
                System.out.print("~~~Improper Usage~~~\n\n");
                System.out.print("Usage Example: ChangeTagInfo \"c:\\1.ape\" TagName TagValue\n\n");
                return;
            }

            File in = File.createFile(args[0], "rw");
            if (!in.isLocal()) {
                System.out.print("~~~Improper Usage~~~\nThe input ape file should be local file.\n");
                return;
            }

            APEInfo apeInfo = IAPEDecompress.createAPEInfo(in);
            APETag tag = apeInfo.getApeInfoTag();
            tag.remove(false);
            tag.setFieldString(args[1], args[2]);
            ID3Tag id3 = new ID3Tag();
            tag.createID3Tag(id3);
            tag.save();

            apeInfo.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
