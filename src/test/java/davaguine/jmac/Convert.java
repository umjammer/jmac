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

import davaguine.jmac.core.APESimple;
import davaguine.jmac.info.CompressionLevel;
import davaguine.jmac.tools.ProgressCallback;


/**
 * @author Dmitry Vaguine
 * @version 04.03.2004 14:51:31
 */
public class Convert extends ProgressCallback {

    private static long g_nInitialTickCount;

    @Override
    public void callback(int percent) {
        double progress = ((double) (percentageDone)) / 1000;
        double elapsedMS = (System.currentTimeMillis() - g_nInitialTickCount);

        double secondsRemaining = (((elapsedMS * 100) / progress) - elapsedMS) / 1000;
        System.out.println("Progress: " + progress + " (" + secondsRemaining + " seconds remaining)          ");
    }

    public static void main(String[] args) {
        try {
            //
            // error check the command line parameters
            //
            if (args.length != 2) {
                System.out.print("~~~Improper Usage~~~\n\n");
                System.out.print("Usage Example: Convert \"c:\\1.ape\" \"c:\\2.ape\"\n\n");
                return;
            }

            //
            // variable declares
            //
            String filename = args[0]; // the file to open
            String oFilename = args[1]; // the file to open

            //
            // attempt to verify the file
            //

            // set the start time and display the starting message
            g_nInitialTickCount = System.currentTimeMillis();
            System.out.println("Converting '" + filename + "'...");

            // do the verify (call unmac.dll)
            APESimple.convertFile(filename, oFilename, CompressionLevel.COMPRESSION_LEVEL_HIGH, new Convert());

            // process the return value
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
