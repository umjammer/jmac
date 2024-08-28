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
import java.nio.charset.StandardCharsets;

import davaguine.jmac.tools.File;
import davaguine.jmac.tools.RandomAccessFile;


/**
 * @author Dmitry Vaguine
 * @version 04.03.2004 14:51:31
 */
public class APELink {

    private final static String APE_LINK_HEADER = "[Monkey's Audio Image Link File]";
    private final static String APE_LINK_IMAGE_FILE_TAG = "Image File=";
    private final static String APE_LINK_START_BLOCK_TAG = "Start Block=";
    private final static String APE_LINK_FINISH_BLOCK_TAG = "Finish Block=";

    public APELink(String filename) throws IOException {
        // empty
        isLinkFile = false;
        startBlock = 0;
        finishBlock = 0;
        imageFilename = "";

        // open the file
        File linkFile = new RandomAccessFile(new java.io.File(filename), "r");
        // create a buffer
        byte[] buffer_ = new byte[1024];

        // fill the buffer from the file and null terminate it
        int numRead = linkFile.read(buffer_);

        byte[] buffer = new byte[numRead];
        System.arraycopy(buffer_, 0, buffer, 0, numRead);

        // call the other constructor (uses a buffer instead of opening the file)
        parseData(buffer, filename);
    }

    public APELink(byte[] data, String filename) {
        parseData(data, filename);
    }

    public boolean isLinkFile() {
        return isLinkFile;
    }

    public int getStartBlock() {
        return startBlock;
    }

    public int getFinishBlock() {
        return finishBlock;
    }

    public String getImageFilename() {
        return imageFilename;
    }

    protected boolean isLinkFile;
    protected int startBlock;
    protected int finishBlock;
    protected String imageFilename;

    protected void parseData(byte[] data, String filename) {
        // empty
        isLinkFile = false;
        startBlock = 0;
        finishBlock = 0;
        imageFilename = "";

        if (data != null) {
            String data_;
            // parse out the information
            data_ = new String(data, StandardCharsets.US_ASCII);

            int header = data_.indexOf(APE_LINK_HEADER);
            int imageFile = data_.indexOf(APE_LINK_IMAGE_FILE_TAG);
            int startBlock = data_.indexOf(APE_LINK_START_BLOCK_TAG);
            int finishBlock = data_.indexOf(APE_LINK_FINISH_BLOCK_TAG);

            if (header >= 0 && imageFile >= 0 && startBlock >= 0 && finishBlock >= 0) {
                // get the start and finish blocks
                int i1 = data_.indexOf('\r', startBlock);
                int i2 = data_.indexOf('\n', startBlock);
                int ii = i1 > 0 && i2 > 0 ? Math.min(i1, i2) : Math.max(i1, i2);

                try {
                    startBlock = Integer.parseInt(data_.substring(startBlock + APE_LINK_START_BLOCK_TAG.length(), ii >= 0 ? ii : data_.length()));
                } catch (Exception e) {
                    startBlock = -1;
                }

                i1 = data_.indexOf('\r', finishBlock);
                i2 = data_.indexOf('\n', finishBlock);
                ii = i1 > 0 && i2 > 0 ? Math.min(i1, i2) : Math.max(i1, i2);
                try {
                    this.finishBlock = Integer.parseInt(data_.substring(finishBlock + APE_LINK_FINISH_BLOCK_TAG.length(), ii >= 0 ? ii : data_.length()));
                } catch (Exception e) {
                    this.finishBlock = -1;
                }

                // get the path
                i1 = data_.indexOf('\r', imageFile);
                i2 = data_.indexOf('\n', imageFile);
                ii = i1 > 0 && i2 > 0 ? Math.min(i1, i2) : Math.max(i1, i2);
                String imageFile_ = data_.substring(imageFile + APE_LINK_IMAGE_FILE_TAG.length(), ii >= 0 ? ii : data_.length());

                // process the path
                if (imageFile_.lastIndexOf('\\') < 0) {
                    int ij = filename.lastIndexOf('\\');
                    imageFilename = ij >= 0 ? filename.substring(0, ij) + imageFile_ : imageFile_;
                } else {
                    imageFilename = imageFile_;
                }

                // this is a valid link file
                isLinkFile = true;
            }
        }
    }
}
