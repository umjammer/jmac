/*
 *  21.04.2004 Original verion. davagin@udm.ru.
 *-----------------------------------------------------------------------
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
 *----------------------------------------------------------------------
 */

#ifndef _JMAC_H
#define _JMAC_H

#ifdef __cplusplus
extern "C" {
#endif

    JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved);

    JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved);

    JNIEXPORT jint JNICALL Java_davaguine_jmac_decoder_APEDecompressNative_registerDecoder(JNIEnv* env, jobject athisObject,
                                                                                           jobject aioObject, jint nVersion,
                                                                                           jint nCompressionLevel, jint nStartBlock,
                                                                                           jint nFinishBlock, jint nTotalBlocks,
                                                                                           jint nBlockAlign, jint nBlocksPerFrame,
                                                                                           jint nSampleRate, jint nBitsPerSample,
                                                                                           jint nChannels);

    JNIEXPORT jint JNICALL Java_davaguine_jmac_decoder_APEDecompressNative_GetData(JNIEnv* env, jobject athisObject, jint ID,
                                                                                   jobject aioObject, jbyteArray pBuffer, jint nBlocks);

    JNIEXPORT void JNICALL Java_davaguine_jmac_decoder_APEDecompressNative_Seek(JNIEnv* env, jobject athisObject, jint ID,
                                                                                jobject aioObject, jint nBlockOffset);

    JNIEXPORT void JNICALL Java_davaguine_jmac_decoder_APEDecompressNative_finalize(JNIEnv* env, jobject athisObject, jint ID,
                                                                                    jobject aioObject);

#ifdef __cplusplus
}
#endif

#endif //_JMAC_H
