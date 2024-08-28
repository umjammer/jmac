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

#include <jni.h>
#include <All.h>
#include <IO.h>
#include "UnBitArrayJMAC.h"
#include "APEDecompressJMAC.h"
#include "NULLIO.h"

CNULLIO m_IO;

/***********************************************************************************
Construction
***********************************************************************************/
CUnBitArrayJMAC::CUnBitArrayJMAC(CAPEDecompressJMAC* decoder, int nVersion) : CUnBitArray(&m_IO, nVersion) {
    m_APEDecompress = decoder;
    JNIEnv* env = m_APEDecompress->env;
    jbyteArray ba = env->NewByteArray(m_nBytes);
    if (ba != NULL) {
        byteArray = (jbyteArray) env->NewGlobalRef(ba);
        env->DeleteLocalRef(ba);
    } else
        byteArray = NULL;
}

CUnBitArrayJMAC::~CUnBitArrayJMAC() {
    if (byteArray != NULL)
        m_APEDecompress->env->DeleteGlobalRef(byteArray);
}

int CUnBitArrayJMAC::Seek(int nDistance) {
    JNIEnv* env = m_APEDecompress->env;
    jclass clazz = env->GetObjectClass(m_APEDecompress->ioObject);
    jmethodID method = env->GetMethodID(clazz, "seek", "(J)V");
    if (method == NULL) {
        if (env->ExceptionOccurred()) {
            env->ExceptionDescribe();
            env->ExceptionClear();
        }
        env->DeleteLocalRef(clazz);
        return ERROR_IO_READ;
    }
    env->DeleteLocalRef(clazz);
    env->CallVoidMethod(m_APEDecompress->ioObject, method, (jlong) nDistance);
    if (env->ExceptionOccurred()) {
        env->ExceptionClear();
        return ERROR_IO_READ;
    }
    return ERROR_SUCCESS;
}

int CUnBitArrayJMAC::Read(void* pBuffer, unsigned int nBytesToRead, unsigned int* pBytesRead) {
    JNIEnv* env = m_APEDecompress->env;
    jclass clazz = env->GetObjectClass(m_APEDecompress->ioObject);
    jmethodID method = env->GetMethodID(clazz, "read", "([BII)I");
    if (method == NULL) {
        if (env->ExceptionOccurred()) {
            env->ExceptionDescribe();
            env->ExceptionClear();
        }
        env->DeleteLocalRef(clazz);
        return ERROR_IO_READ;
    }
    env->DeleteLocalRef(clazz);
    *pBytesRead = env->CallIntMethod(m_APEDecompress->ioObject, method, byteArray, 0, nBytesToRead);
    if (env->ExceptionOccurred()) {
        env->ExceptionClear();
        return ERROR_IO_READ;
    }
    void* elements = env->GetPrimitiveArrayCritical(byteArray, NULL);
    if (elements == NULL) {
        if (env->ExceptionOccurred())
            env->ExceptionClear();
        return ERROR_IO_READ;
    }
    memcpy(pBuffer, elements, nBytesToRead);
    env->ReleasePrimitiveArrayCritical(byteArray, elements, 0);
    return ERROR_SUCCESS;
}

int CUnBitArrayJMAC::FillAndResetBitArray(int nFileLocation, int nNewBitIndex) {
    // reset the bit index
    m_nCurrentBitIndex = nNewBitIndex;

    // seek if necessary
    if (nFileLocation != -1) {
        if (Seek(nFileLocation) != 0)
            return ERROR_IO_READ;
    }

    // read the new data into the bit array
    unsigned int nBytesRead = 0;
    if (Read(((unsigned char *) m_pBitArray), m_nBytes, &nBytesRead) != ERROR_SUCCESS)
        return ERROR_IO_READ;

    return ERROR_SUCCESS;
}

int CUnBitArrayJMAC::FillBitArray() {
    // get the bit array index
    uint32 nBitArrayIndex = m_nCurrentBitIndex >> 5;

    // move the remaining data to the front
    memmove((void *) (m_pBitArray), (const void *) (m_pBitArray + nBitArrayIndex), m_nBytes - (nBitArrayIndex * 4));

    // read the new data
    int nBytesToRead = nBitArrayIndex * 4;
    unsigned int nBytesRead = 0;
    int nRetVal = Read((unsigned char*) (m_pBitArray + m_nElements - nBitArrayIndex), nBytesToRead, &nBytesRead);

    // adjust the m_Bit pointer
    m_nCurrentBitIndex = m_nCurrentBitIndex & 31;

    // return
    return (nRetVal == ERROR_SUCCESS) ? ERROR_SUCCESS : ERROR_IO_READ;
}
