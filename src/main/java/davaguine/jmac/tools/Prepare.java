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

import davaguine.jmac.info.SpecialFrame;
import davaguine.jmac.info.WaveFormat;


/**
 * @author Dmitry Vaguine
 * @version 04.03.2004 14:51:31
 */
public class Prepare {

    public static void prepare(ByteArrayReader rawData, int bytes, WaveFormat waveFormatEx, int[] outputX, int[] outputY, Crc32 crc, IntegerPointer specialCodes, IntegerPointer peakLevel) {
        // initialize the pointers that got passed in
        crc.init();
        specialCodes.value = 0;

        // variables
        int totalBlocks = bytes / waveFormatEx.blockAlign;
        int r, l;

        // the prepare code

        if (waveFormatEx.bitsPerSample == 8) {
            if (waveFormatEx.channels == 2) {
                for (int blockIndex = 0; blockIndex < totalBlocks; blockIndex++) {
                    short b1 = rawData.readUnsignedByte();
                    short b2 = rawData.readUnsignedByte();
                    r = b1 - 128;
                    l = b2 - 128;

                    crc.append((byte) b1);
                    crc.append((byte) b2);

                    // check the peak
                    if (Math.abs(l) > peakLevel.value)
                        peakLevel.value = Math.abs(l);
                    if (Math.abs(r) > peakLevel.value)
                        peakLevel.value = Math.abs(r);

                    // convert to x,y
                    outputY[blockIndex] = l - r;
                    outputX[blockIndex] = r + (outputY[blockIndex] / 2);
                }
            } else if (waveFormatEx.channels == 1) {
                for (int blockIndex = 0; blockIndex < totalBlocks; blockIndex++) {
                    short b1 = rawData.readUnsignedByte();
                    r = b1 - 128;

                    crc.append((byte) b1);

                    // check the peak
                    if (Math.abs(r) > peakLevel.value)
                        peakLevel.value = Math.abs(r);

                    // convert to x,y
                    outputX[blockIndex] = r;
                }
            }
        } else if (waveFormatEx.bitsPerSample == 24) {
            if (waveFormatEx.channels == 2) {
                for (int blockIndex = 0; blockIndex < totalBlocks; blockIndex++) {
                    long temp = 0;

                    short b = rawData.readUnsignedByte();
                    temp |= (b << 0);
                    crc.append((byte) b);

                    b = rawData.readUnsignedByte();
                    temp |= (b << 8);
                    crc.append((byte) b);

                    b = rawData.readUnsignedByte();
                    temp |= (b << 16);
                    crc.append((byte) b);

                    if ((temp & 0x80_0000) > 0)
                        r = (int) (temp & 0x7f_ffff) - 0x80_0000;
                    else
                        r = (int) (temp & 0x7f_ffff);

                    temp = 0;

                    b = rawData.readUnsignedByte();
                    temp |= (b << 0);
                    crc.append((byte) b);

                    b = rawData.readUnsignedByte();
                    temp |= (b << 8);
                    crc.append((byte) b);

                    b = rawData.readUnsignedByte();
                    temp |= (b << 16);
                    crc.append((byte) b);

                    if ((temp & 0x80_0000) > 0)
                        l = (int) (temp & 0x7f_ffff) - 0x80_0000;
                    else
                        l = (int) (temp & 0x7f_ffff);

                    // check the peak
                    if (Math.abs(l) > peakLevel.value)
                        peakLevel.value = Math.abs(l);
                    if (Math.abs(r) > peakLevel.value)
                        peakLevel.value = Math.abs(r);

                    // convert to x,y
                    outputY[blockIndex] = l - r;
                    outputX[blockIndex] = r + (outputY[blockIndex] / 2);
                }
            } else if (waveFormatEx.channels == 1) {
                for (int blockIndex = 0; blockIndex < totalBlocks; blockIndex++) {
                    long temp = 0;

                    short b = rawData.readUnsignedByte();
                    temp |= (b << 0);
                    crc.append((byte) b);

                    b = rawData.readUnsignedByte();
                    temp |= (b << 8);
                    crc.append((byte) b);

                    b = rawData.readUnsignedByte();
                    temp |= (b << 16);
                    crc.append((byte) b);

                    if ((temp & 0x80_0000) > 0)
                        r = (int) (temp & 0x7f_ffff) - 0x80_0000;
                    else
                        r = (int) (temp & 0x7f_ffff);

                    // check the peak
                    if (Math.abs(r) > peakLevel.value)
                        peakLevel.value = Math.abs(r);

                    // convert to x,y
                    outputX[blockIndex] = r;
                }
            }
        } else {
            if (waveFormatEx.channels == 2) {
                int lPeak = 0;
                int rPeak = 0;
                int blockIndex = 0;
                for (blockIndex = 0; blockIndex < totalBlocks; blockIndex++) {
                    r = rawData.readShort();
                    crc.append((short) r);

                    l = rawData.readShort();
                    crc.append((short) l);

                    // check the peak
                    if (Math.abs(l) > lPeak)
                        lPeak = Math.abs(l);
                    if (Math.abs(r) > rPeak)
                        rPeak = Math.abs(r);

                    // convert to x,y
                    outputY[blockIndex] = l - r;
                    outputX[blockIndex] = r + (outputY[blockIndex] / 2);
                }

                if (lPeak == 0)
                    specialCodes.value |= SpecialFrame.SPECIAL_FRAME_LEFT_SILENCE;
                if (rPeak == 0)
                    specialCodes.value |= SpecialFrame.SPECIAL_FRAME_RIGHT_SILENCE;
                if (Math.max(lPeak, rPeak) > peakLevel.value)
                    peakLevel.value = Math.max(lPeak, rPeak);

                // check for pseudo-stereo files
                blockIndex = 0;
                while (outputY[blockIndex++] == 0) {
                    if (blockIndex == (bytes / 4)) {
                        specialCodes.value |= SpecialFrame.SPECIAL_FRAME_PSEUDO_STEREO;
                        break;
                    }
                }
            } else if (waveFormatEx.channels == 1) {
                int peak = 0;
                for (int blockIndex = 0; blockIndex < totalBlocks; blockIndex++) {
                    r = rawData.readUnsignedShort();
                    crc.append((short) r);

                    // check the peak
                    if (Math.abs(r) > peak)
                        peak = Math.abs(r);

                    // convert to x,y
                    outputX[blockIndex] = r;
                }

                if (peak > peakLevel.value)
                    peakLevel.value = peak;
                if (peak == 0)
                    specialCodes.value |= SpecialFrame.SPECIAL_FRAME_MONO_SILENCE;
            }
        }

        crc.prefinalizeCrc();

        // add the special code
        crc.finalizeCrc();

        if (specialCodes.value != 0)
            crc.doSpecial();
    }

    public static void unprepare(int x, int y, WaveFormat waveFormat, ByteBuffer output, Crc32 crc) {
        // decompress and convert from (x,y) -> (l,r)
        // sort of long and ugly.... sorry
        int channels = waveFormat.channels;
        int bitsPerSample = waveFormat.bitsPerSample;
        if (channels == 2) {
            if (bitsPerSample == 16) {
                // get the right and left values
                short r = (short) (x - (y / 2));
                short l = (short) (r + y);

                output.append(r, l);
                crc.append(r, l);
            } else if (bitsPerSample == 8) {
                byte r = (byte) (x - (y / 2) + 128);
                byte l = (byte) (r + y);

                output.append(r, l);
                crc.append(r, l);
            } else if (bitsPerSample == 24) {
                int rv = x - (y / 2);
                int lv = rv + y;

                if (rv < 0)
                    rv = (rv + 0x80_0000) | 0x80_0000;
                if (lv < 0)
                    lv = (lv + 0x80_0000) | 0x80_0000;

                output.append24(rv, lv);
                crc.append24(rv, lv);
            }
        } else if (channels == 1) {
            if (bitsPerSample == 16) {
                output.append((short) x);
                crc.append((short) x);
            } else if (bitsPerSample == 8) {
                byte r = (byte) (x + 128);

                output.append(r);
                crc.append(r);
            } else if (bitsPerSample == 24) {
                if (x < 0)
                    x = (x + 0x80_0000) | 0x80_0000;

                output.append24(x);
                crc.append24(x);
            }
        }
    }

    public static void unprepareOld(int[] inputX, int[] inputY, int blocks, WaveFormat waveFormatEx, ByteBuffer output, Crc32 crc, int fileVersion) {
        // the CRC that will be figured during decompression
        crc.init();

        // decompress and convert from (x,y) -> (l,r)
        // sort of int and ugly.... sorry
        int channels = waveFormatEx.channels;
        int bitsPerSample = waveFormatEx.bitsPerSample;
        if (channels == 2) {
            // convert the x,y data to raw data
            if (bitsPerSample == 16) {
                short r;
                int x = 0;
                int y = 0;

                for (; x < blocks; x++, y++) {
                    r = (short) (inputX[x] - (inputY[y] / 2));

                    output.append(r);
                    crc.append(r);
                    r += (short) inputY[y];
                    output.append(r);
                    crc.append(r);
                }
            } else if (bitsPerSample == 8) {
                byte r;
                if (fileVersion > 3830) {
                    for (int sampleIndex = 0; sampleIndex < blocks; sampleIndex++) {
                        r = (byte) (inputX[sampleIndex] - (inputY[sampleIndex] / 2) + 128);
                        output.append(r);
                        crc.append(r);
                        r += (byte) inputY[sampleIndex];
                        output.append(r);
                        crc.append(r);
                    }
                } else {
                    for (int sampleIndex = 0; sampleIndex < blocks; sampleIndex++) {
                        r = (byte) (inputX[sampleIndex] - (inputY[sampleIndex] / 2));
                        output.append(r);
                        crc.append(r);
                        r += (byte) inputY[sampleIndex];
                        output.append(r);
                        crc.append(r);
                    }
                }
            } else if (bitsPerSample == 24) {
                int rv, lv;

                for (int sampleIndex = 0; sampleIndex < blocks; sampleIndex++) {
                    rv = inputX[sampleIndex] - (inputY[sampleIndex] / 2);
                    lv = rv + inputY[sampleIndex];

                    int temp = 0;
                    if (rv < 0)
                        temp = (rv + 0x80_0000) | 0x80_0000;
                    else
                        temp = rv;

                    output.append24(temp);
                    crc.append24(temp);

                    temp = 0;
                    if (lv < 0)
                        temp = (lv + 0x80_0000) | 0x80_0000;
                    else
                        temp = lv;

                    output.append24(temp);
                    crc.append24(temp);
                }
            }
        } else if (channels == 1) {
            // convert to raw data
            if (bitsPerSample == 8) {
                byte r;
                if (fileVersion > 3830) {
                    for (int sampleIndex = 0; sampleIndex < blocks; sampleIndex++) {
                        r = (byte) (inputX[sampleIndex] + 128);
                        output.append(r);
                        crc.append(r);
                    }
                } else {
                    for (int sampleIndex = 0; sampleIndex < blocks; sampleIndex++) {
                        r = (byte) (inputX[sampleIndex]);
                        output.append(r);
                        crc.append(r);
                    }
                }

            } else if (bitsPerSample == 24) {

                int rv;
                for (int sampleIndex = 0; sampleIndex < blocks; sampleIndex++) {
                    rv = inputX[sampleIndex];

                    int temp = 0;
                    if (rv < 0)
                        temp = (rv + 0x80_0000) | 0x80_0000;
                    else
                        temp = rv;

                    output.append24(temp);
                    crc.append24(temp);
                }
            } else {
                short r;
                for (int sampleIndex = 0; sampleIndex < blocks; sampleIndex++) {
                    r = (short) (inputX[sampleIndex]);
                    output.append(r);
                    crc.append(r);
                }
            }
        }
        crc.prefinalizeCrc();
    }
}
