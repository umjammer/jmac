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

// jmac.cpp
//

#include <jni.h>
#include <All.h>
#include "jmac.h"
#include "APEDecompressJMAC.h"

#define MAX_DECODERS 1024
CAPEDecompressJMAC* decoders[MAX_DECODERS];

typedef struct MAC_ERROR {
    int nErrorCode;
    char* spErrorExplanation;
} MAC_ERROR;

#define NUM_MAC_ERRORS 23
MAC_ERROR MAC_ERRORS[NUM_MAC_ERRORS] = {
    { ERROR_IO_READ                               , "I/O read error" },                        
    { ERROR_IO_WRITE                              , "I/O write error" },                       
    { ERROR_INVALID_INPUT_FILE                    , "invalid input file" },                    
    { ERROR_INVALID_OUTPUT_FILE                   , "invalid output file" },                   
    { ERROR_INPUT_FILE_TOO_LARGE                  , "input file file too large" },             
    { ERROR_INPUT_FILE_UNSUPPORTED_BIT_DEPTH      , "input file unsupported bit depth" },      
    { ERROR_INPUT_FILE_UNSUPPORTED_SAMPLE_RATE    , "input file unsupported sample rate" },    
    { ERROR_INPUT_FILE_UNSUPPORTED_CHANNEL_COUNT  , "input file unsupported channel count" },  
    { ERROR_INPUT_FILE_TOO_SMALL                  , "input file too small" },                  
    { ERROR_INVALID_CHECKSUM                      , "invalid checksum" },                      
    { ERROR_DECOMPRESSING_FRAME                   , "decompressing frame" },                   
    { ERROR_INITIALIZING_UNMAC                    , "initializing unmac" },                    
    { ERROR_INVALID_FUNCTION_PARAMETER            , "invalid function parameter" },            
    { ERROR_UNSUPPORTED_FILE_TYPE                 , "unsupported file type" },                 
    { ERROR_INSUFFICIENT_MEMORY                   , "insufficient memory" },                   
    { ERROR_LOADING_UNMAC_DLL                     , "loading UnMAC.dll" },
    { ERROR_USER_STOPPED_PROCESSING               , "user stopped processing" },               
    { ERROR_SKIPPED                               , "skipped" },                               
    { ERROR_BAD_PARAMETER                         , "bad parameter" },                         
    { ERROR_APE_COMPRESS_TOO_MUCH_DATA            , "APE compress too much data" },            
    { ERROR_UNDEFINED                             , "undefined" },                             
};

void ThrowError(JNIEnv* env, int nErrorCode) {
    char* errorMessage = "Error Undefined";
    for (int i = 0; i < NUM_MAC_ERRORS; i++) {
        if (MAC_ERRORS[i].nErrorCode == nErrorCode) {
            errorMessage = MAC_ERRORS[i].spErrorExplanation;
            break;
        }
    }
    jclass clazz = env->FindClass("davaguine/jmac/tools/JMACException");
    if (clazz == NULL) {
        if (env->ExceptionOccurred()) {
            env->ExceptionDescribe();
            env->ExceptionClear();
        }
    } else
        env->ThrowNew(clazz, errorMessage);
    env->DeleteLocalRef(clazz);
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    memset(decoders, 0, sizeof(decoders));
    return JNI_VERSION_1_4;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved) {
    for (jint i = 0; i < MAX_DECODERS; i++) {
        if (decoders[i]) {
            delete decoders[i];
            decoders[i] = NULL;
        }
    }
}

JNIEXPORT jint JNICALL Java_davaguine_jmac_decoder_APEDecompressNative_registerDecoder(JNIEnv* env, jobject athisObject, jobject aioObject,
                                                                                       jint nVersion, jint nCompressionLevel,
                                                                                       jint nStartBlock, jint nFinishBlock,
                                                                                       jint nTotalBlocks, jint nBlockAlign,
                                                                                       jint nBlocksPerFrame, jint nSampleRate,
                                                                                       jint nBitsPerSample, jint nChannels) {
    for (jint i = 0; i < MAX_DECODERS; i++) {
        if (!decoders[i]) {
            decoders[i] = new CAPEDecompressJMAC(env, athisObject, aioObject, nVersion, nCompressionLevel, nStartBlock, nFinishBlock,
                                                 nTotalBlocks, nBlockAlign, nBlocksPerFrame, nSampleRate, nBitsPerSample, nChannels);
            return i;
        }
    }
    return -1;
}

JNIEXPORT jint JNICALL Java_davaguine_jmac_decoder_APEDecompressNative_GetData(JNIEnv* env, jobject athisObject, jint ID, jobject aioObject,
                                                                               jbyteArray pBuffer, jint nBlocks) {
    if (decoders[ID] != NULL) {
        decoders[ID]->SetRefs(env, athisObject, aioObject);
        jbyte* elements = env->GetByteArrayElements(pBuffer, NULL);
        if (elements != NULL) {
            APE::int64 nBlocksRetrieved;
            int retValue = decoders[ID]->GetData((unsigned char *) elements, nBlocks, &nBlocksRetrieved);
            env->ReleaseByteArrayElements(pBuffer, elements, 0);
            if (retValue != ERROR_SUCCESS)
                ThrowError(env, retValue);
            return nBlocksRetrieved;
        }
    }
    ThrowError(env, ERROR_UNDEFINED);
    return -1;
};

JNIEXPORT void JNICALL Java_davaguine_jmac_decoder_APEDecompressNative_Seek(JNIEnv* env, jobject athisObject, jint ID, jobject aioObject,
                                                                            jint nBlockOffset) {
    if (decoders[ID] != NULL) {
        decoders[ID]->SetRefs(env, athisObject, aioObject);
        int retValue = decoders[ID]->Seek(nBlockOffset);
        if (retValue != ERROR_SUCCESS)
            ThrowError(env, retValue);
        return;
    }
    ThrowError(env, ERROR_UNDEFINED);
};

JNIEXPORT void JNICALL Java_davaguine_jmac_decoder_APEDecompressNative_finalize(JNIEnv* env, jobject athisObject, jint ID, jobject aioObject) {
    if (decoders[ID] != NULL) {
        decoders[ID]->SetRefs(env, athisObject, aioObject);
        delete decoders[ID];
        decoders[ID] = NULL;
        return;
    }
    ThrowError(env, ERROR_UNDEFINED);
};
