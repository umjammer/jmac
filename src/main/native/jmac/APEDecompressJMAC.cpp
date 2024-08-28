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
#include "APEDecompressJMAC.h"
#include "UnBitArrayJMAC.h"

#include <APEInfo.h>
#include <Prepare.h>
#include <UnBitArray.h>
#include <NewPredictor.h>

#define DECODE_BLOCK_SIZE        4096

CAPEDecompressJMAC::CAPEDecompressJMAC(JNIEnv* aenv, jobject athisObject, jobject aioObject, int nVersion, int nCompressionLevel,
                                       int nStartBlock, int nFinishBlock, int nTotalBlocks, int nBlockAlign, int nBlocksPerFrame,
                                       int nSampleRate, int nBitsPerSample, int nChannels) {
    SetRefs(aenv, athisObject, aioObject);

    m_nVersion = nVersion;
    m_nCompressionLevel = nCompressionLevel;
    m_nBlockAlign = nBlockAlign;
    m_nBlocksPerFrame = nBlocksPerFrame;
    m_nBitsPerSample = nBitsPerSample;

    FillWaveFormatEx(&m_wfeInput, nSampleRate, nBitsPerSample, nChannels);

    // initialize other stuff
    m_bDecompressorInitialized = FALSE;
    m_nCurrentFrame = 0;
    m_nRealFrame = 0;
    m_nCurrentBlock = 0;
    m_nCurrentFrameBufferBlock = 0;
    m_nFrameBufferFinishedBlocks = 0;
    m_bErrorDecodingCurrentFrame = FALSE;

    // set the "real" start and finish blocks
    m_nStartBlock = (nStartBlock < 0) ? 0 : min(nStartBlock, nTotalBlocks);
    m_nFinishBlock = (nFinishBlock < 0) ? nTotalBlocks : min(nFinishBlock, nTotalBlocks);
    m_bIsRanged = (m_nStartBlock != 0) || (m_nFinishBlock != nTotalBlocks);
}

CAPEDecompressJMAC::~CAPEDecompressJMAC() {
}

int CAPEDecompressJMAC::GetData(char* pBuffer, int nBlocks, int* pBlocksRetrieved) {
    int nRetVal = ERROR_SUCCESS;
    if (pBlocksRetrieved)
        *pBlocksRetrieved = 0;

    // make sure we're initialized
    RETURN_ON_ERROR(InitializeDecompressor())

    // cap
    int nBlocksUntilFinish = m_nFinishBlock - m_nCurrentBlock;
    const int nBlocksToRetrieve = min(nBlocks, nBlocksUntilFinish);

    // get the data
    unsigned char * pOutputBuffer = (unsigned char *) pBuffer;
    int nBlocksLeft = nBlocksToRetrieve; int nBlocksThisPass = 1;
    while ((nBlocksLeft > 0) && (nBlocksThisPass > 0)) {
        // fill up the frame buffer
        int nDecodeRetVal = FillFrameBuffer();
        if (nDecodeRetVal != ERROR_SUCCESS)
            nRetVal = nDecodeRetVal;

        // analyze how much to remove from the buffer
        const int nFrameBufferBlocks = m_nFrameBufferFinishedBlocks;
        nBlocksThisPass = min(nBlocksLeft, nFrameBufferBlocks);

        // remove as much as possible
        if (nBlocksThisPass > 0) {
            m_cbFrameBuffer.Get(pOutputBuffer, nBlocksThisPass * m_nBlockAlign);
            pOutputBuffer += nBlocksThisPass * m_nBlockAlign;
            nBlocksLeft -= nBlocksThisPass;
            m_nFrameBufferFinishedBlocks -= nBlocksThisPass;
        }
    }

    // calculate the blocks retrieved
    int nBlocksRetrieved = nBlocksToRetrieve - nBlocksLeft;

    // update position
    m_nCurrentBlock += nBlocksRetrieved;
    if (pBlocksRetrieved)
        *pBlocksRetrieved = nBlocksRetrieved;

    return nRetVal;
}

int CAPEDecompressJMAC::Seek(int nBlockOffset) {
    RETURN_ON_ERROR(InitializeDecompressor())

    // use the offset
    nBlockOffset += m_nStartBlock;

    // cap (to prevent seeking too far)
    if (nBlockOffset >= m_nFinishBlock)
        nBlockOffset = m_nFinishBlock - 1;
    if (nBlockOffset < m_nStartBlock)
        nBlockOffset = m_nStartBlock;

    // seek to the perfect location
    int nBaseFrame = nBlockOffset / m_nBlocksPerFrame;
    int nBlocksToSkip = nBlockOffset % m_nBlocksPerFrame;
    int nBytesToSkip = nBlocksToSkip* m_nBlockAlign;

    m_nCurrentBlock = nBaseFrame * m_nBlocksPerFrame;
    m_nCurrentFrameBufferBlock = nBaseFrame * m_nBlocksPerFrame;
    m_nCurrentFrame = nBaseFrame;
    m_nFrameBufferFinishedBlocks = 0;
    m_cbFrameBuffer.Empty();
    RETURN_ON_ERROR(SeekToFrame(m_nCurrentFrame));

    // skip necessary blocks
    CSmartPtr<char> spTempBuffer(new char [nBytesToSkip], TRUE);
    if (spTempBuffer == NULL)
        return ERROR_INSUFFICIENT_MEMORY;

    int nBlocksRetrieved = 0;
    GetData(spTempBuffer, nBlocksToSkip, &nBlocksRetrieved);
    if (nBlocksRetrieved != nBlocksToSkip)
        return ERROR_UNDEFINED;

    return ERROR_SUCCESS;
}

int CAPEDecompressJMAC::InitializeDecompressor() {
    // check if we have anything to do
    if (m_bDecompressorInitialized)
        return ERROR_SUCCESS;

    // update the initialized flag
    m_bDecompressorInitialized = TRUE;

    // create a frame buffer
    m_cbFrameBuffer.CreateBuffer((m_nBlocksPerFrame + DECODE_BLOCK_SIZE) * m_nBlockAlign, m_nBlockAlign * 64);

    // create decoding components
    m_spUnBitArray.Assign((CUnBitArrayBase *)  new CUnBitArrayJMAC(this, m_nVersion));

    if (m_nVersion >= 3950) {
        m_spNewPredictorX.Assign(new CPredictorDecompress3950toCurrent(m_nCompressionLevel, m_nVersion));
        m_spNewPredictorY.Assign(new CPredictorDecompress3950toCurrent(m_nCompressionLevel, m_nVersion));
    } else {
        m_spNewPredictorX.Assign(new CPredictorDecompressNormal3930to3950(m_nCompressionLevel, m_nVersion));
        m_spNewPredictorY.Assign(new CPredictorDecompressNormal3930to3950(m_nCompressionLevel, m_nVersion));
    }

    // seek to the beginning
    return Seek(-1);
}

/*****************************************************************************************
Decodes blocks of data
*****************************************************************************************/
int CAPEDecompressJMAC::FillFrameBuffer() {
    int nRetVal = ERROR_SUCCESS;

    // determine the maximum blocks we can decode
    // note that we won't do end capping because we can't use data
    // until EndFrame(...) successfully handles the frame
    // that means we may decode a little extra in end capping cases
    // but this allows robust error handling of bad frames
    int nMaxBlocks = m_cbFrameBuffer.MaxAdd() / m_nBlockAlign;

    // loop and decode data
    int nBlocksLeft = nMaxBlocks;
    while (nBlocksLeft > 0) {
        int nFrameBlocks = getApeInfoFrameBlocks(m_nCurrentFrame);
        if (nFrameBlocks < 0)
            break;

        int nFrameOffsetBlocks = m_nCurrentFrameBufferBlock % m_nBlocksPerFrame;
        int nFrameBlocksLeft = nFrameBlocks - nFrameOffsetBlocks;
        int nBlocksThisPass = min(nFrameBlocksLeft, nBlocksLeft);

        // start the frame if we need to
        if (nFrameOffsetBlocks == 0)
            StartFrame();

        // store the frame buffer bytes before we start
        int nFrameBufferBytes = m_cbFrameBuffer.MaxGet();

        // decode data
        DecodeBlocksToFrameBuffer(nBlocksThisPass);

        // end the frame if we need to
        if ((nFrameOffsetBlocks + nBlocksThisPass) >= nFrameBlocks) {
            EndFrame();
            if (m_bErrorDecodingCurrentFrame) {
                // remove any decoded data from the buffer
                m_cbFrameBuffer.RemoveTail(m_cbFrameBuffer.MaxGet() - nFrameBufferBytes);

                // add silence
                unsigned char cSilence = (m_nBitsPerSample == 8) ? 127 : 0;
                for (int z = 0; z < nFrameBlocks*m_nBlockAlign; z++) {
                    *m_cbFrameBuffer.GetDirectWritePointer() = cSilence;
                    m_cbFrameBuffer.UpdateAfterDirectWrite(1);
                }

                // seek to try to synchronize after an error
                SeekToFrame(m_nCurrentFrame);

                // save the return value
                nRetVal = ERROR_INVALID_CHECKSUM;
            }
        }

        nBlocksLeft -= nBlocksThisPass;
    }

    return nRetVal;
}

void CAPEDecompressJMAC::DecodeBlocksToFrameBuffer(int nBlocks) {
    // decode the samples
    int nBlocksProcessed = 0;

    try {
        if (m_wfeInput.nChannels == 2) {
            if ((m_nSpecialCodes & SPECIAL_FRAME_LEFT_SILENCE) && (m_nSpecialCodes & SPECIAL_FRAME_RIGHT_SILENCE))
                for (nBlocksProcessed = 0; nBlocksProcessed < nBlocks; nBlocksProcessed++) {
                    m_Prepare.Unprepare(0, 0, &m_wfeInput, m_cbFrameBuffer.GetDirectWritePointer(), &m_nCRC);
                    m_cbFrameBuffer.UpdateAfterDirectWrite(m_nBlockAlign);
                }
            else if (m_nSpecialCodes & SPECIAL_FRAME_PSEUDO_STEREO)
                for (nBlocksProcessed = 0; nBlocksProcessed < nBlocks; nBlocksProcessed++) {
                    int X = m_spNewPredictorX->DecompressValue(m_spUnBitArray->DecodeValueRange(m_BitArrayStateX));
                    m_Prepare.Unprepare(X, 0, &m_wfeInput, m_cbFrameBuffer.GetDirectWritePointer(), &m_nCRC);
                    m_cbFrameBuffer.UpdateAfterDirectWrite(m_nBlockAlign);
                }
            else {
                if (m_nVersion >= 3950)
                    for (nBlocksProcessed = 0; nBlocksProcessed < nBlocks; nBlocksProcessed++) {
                        int nY = m_spUnBitArray->DecodeValueRange(m_BitArrayStateY);
                        int nX = m_spUnBitArray->DecodeValueRange(m_BitArrayStateX);
                        int Y = m_spNewPredictorY->DecompressValue(nY, m_nLastX);
                        int X = m_spNewPredictorX->DecompressValue(nX, Y);
                        m_nLastX = X;

                        m_Prepare.Unprepare(X, Y, &m_wfeInput, m_cbFrameBuffer.GetDirectWritePointer(), &m_nCRC);
                        m_cbFrameBuffer.UpdateAfterDirectWrite(m_nBlockAlign);
                    }
                else
                    for (nBlocksProcessed = 0; nBlocksProcessed < nBlocks; nBlocksProcessed++) {
                        int X = m_spNewPredictorX->DecompressValue(m_spUnBitArray->DecodeValueRange(m_BitArrayStateX));
                        int Y = m_spNewPredictorY->DecompressValue(m_spUnBitArray->DecodeValueRange(m_BitArrayStateY));

                        m_Prepare.Unprepare(X, Y, &m_wfeInput, m_cbFrameBuffer.GetDirectWritePointer(), &m_nCRC);
                        m_cbFrameBuffer.UpdateAfterDirectWrite(m_nBlockAlign);
                    }
            }
        } else {
            if (m_nSpecialCodes & SPECIAL_FRAME_MONO_SILENCE)
                for (nBlocksProcessed = 0; nBlocksProcessed < nBlocks; nBlocksProcessed++) {
                    m_Prepare.Unprepare(0, 0, &m_wfeInput, m_cbFrameBuffer.GetDirectWritePointer(), &m_nCRC);
                    m_cbFrameBuffer.UpdateAfterDirectWrite(m_nBlockAlign);
                }
            else
                for (nBlocksProcessed = 0; nBlocksProcessed < nBlocks; nBlocksProcessed++) {
                    int X = m_spNewPredictorX->DecompressValue(m_spUnBitArray->DecodeValueRange(m_BitArrayStateX));
                    m_Prepare.Unprepare(X, 0, &m_wfeInput, m_cbFrameBuffer.GetDirectWritePointer(), &m_nCRC);
                    m_cbFrameBuffer.UpdateAfterDirectWrite(m_nBlockAlign);
                }
        }
    } catch (...) {
        m_bErrorDecodingCurrentFrame = TRUE;
    }

    m_nCurrentFrameBufferBlock += nBlocks;
}

void CAPEDecompressJMAC::StartFrame() {
    m_nCRC = 0xFFFFFFFF;

    // get the frame header
    m_nStoredCRC = m_spUnBitArray->DecodeValue(DECODE_VALUE_METHOD_UNSIGNED_INT);
    m_bErrorDecodingCurrentFrame = FALSE;

    // get any 'special' codes if the file uses them (for silence, FALSE stereo, etc.)
    m_nSpecialCodes = 0;
    if (m_nVersion > 3820) {
        if (m_nStoredCRC & 0x80000000)
            m_nSpecialCodes = m_spUnBitArray->DecodeValue(DECODE_VALUE_METHOD_UNSIGNED_INT);
        m_nStoredCRC &= 0x7FFFFFFF;
    }

    m_spNewPredictorX->Flush();
    m_spNewPredictorY->Flush();

    m_spUnBitArray->FlushState(m_BitArrayStateX);
    m_spUnBitArray->FlushState(m_BitArrayStateY);

    m_spUnBitArray->FlushBitArray();

    m_nLastX = 0;
}

void CAPEDecompressJMAC::EndFrame() {
    m_nFrameBufferFinishedBlocks += getApeInfoFrameBlocks(m_nCurrentFrame);
    m_nCurrentFrame++;

    // finalize
    m_spUnBitArray->Finalize();

    // check the CRC
    m_nCRC = m_nCRC ^ 0xFFFFFFFF;
    m_nCRC >>= 1;
    if (m_nCRC != m_nStoredCRC)
        m_bErrorDecodingCurrentFrame = TRUE;
}

int CAPEDecompressJMAC::getApeInfoFrameBlocks(int nFrameIndex) {
    jclass clazz = env->GetObjectClass(thisObject);
    jmethodID method = env->GetMethodID(clazz, "getApeInfoFrameBlocks", "(I)I");
    if (method == NULL) {
        if (env->ExceptionOccurred()) {
            env->ExceptionDescribe();
            env->ExceptionClear();
        }
        env->DeleteLocalRef(clazz);
        return -1;
    }
    env->DeleteLocalRef(clazz);
    return env->CallIntMethod(thisObject, method, (jint) nFrameIndex);
}

int CAPEDecompressJMAC::getApeInfoSeekByte(int nFrameIndex) {
    jclass clazz = env->GetObjectClass(thisObject);
    jmethodID method = env->GetMethodID(clazz, "getApeInfoSeekByte", "(I)I");
    if (method == NULL) {
        if (env->ExceptionOccurred()) {
            env->ExceptionDescribe();
            env->ExceptionClear();
        }
        env->DeleteLocalRef(clazz);
        return -1;
    }
    env->DeleteLocalRef(clazz);
    return env->CallIntMethod(thisObject, method, (jint) nFrameIndex);
}

int CAPEDecompressJMAC::SeekToFrame(int nFrameIndex) {
    int nSeekRemainder = (getApeInfoSeekByte(nFrameIndex) - getApeInfoSeekByte(0)) % 4;
    int retVal = m_spUnBitArray->FillAndResetBitArray(m_nRealFrame == nFrameIndex ? -1 : getApeInfoSeekByte(nFrameIndex) - nSeekRemainder,
                                                      nSeekRemainder * 8);
    m_nRealFrame = nFrameIndex;
    return retVal;
}

int CAPEDecompressJMAC::GetInfo(APE_DECOMPRESS_FIELDS Field, int nParam1, int nParam2) {
    return -1;
}
