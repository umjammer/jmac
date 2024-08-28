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

#ifndef JMAC_APEDECOMPRESS_H
#define JMAC_APEDECOMPRESS_H

class CUnBitArray;
class CPrepare;
class CAPEInfo;
class IPredictorDecompress;
#include <UnBitArrayBase.h>
#include <MACLib.h>
#include <Prepare.h>
#include <CircleBuffer.h>
#include "UnBitArrayJMAC.h"

class CAPEDecompressJMAC : public IAPEDecompress {
  public:
    CAPEDecompressJMAC(JNIEnv* aenv, jobject athisObject, jobject aioObject, int nVersion, int nCompressionLevel, int nStartBlock,
                       int nFinishBlock, int nTotalBlocks, int nBlockAlign, int nBlocksPerFrame, int nSampleRate, int nBitsPerSample,
                       int nChannels);
    ~CAPEDecompressJMAC();

    inline void SetRefs(JNIEnv* aenv, jobject athisObject, jobject aioObject) {
        env = aenv;
        thisObject = athisObject;
        ioObject = aioObject;
    }

    int getApeInfoFrameBlocks(int nFrameIndex);
    int getApeInfoSeekByte(int nFrameIndex);

    int GetData(char* pBuffer, int nBlocks, int* pBlocksRetrieved);
    int Seek(int nBlockOffset);

    int GetInfo(APE_DECOMPRESS_FIELDS Field, int nParam1 = 0, int nParam2 = 0);

  protected:

    // file info
    int m_nBlockAlign;
    int m_nCurrentFrame;
    int m_nRealFrame;

    // start / finish information
    int m_nStartBlock;
    int m_nFinishBlock;
    int m_nCurrentBlock;
    BOOL m_bIsRanged;
    BOOL m_bDecompressorInitialized;

    // decoding tools    
    CPrepare m_Prepare;
    WAVEFORMATEX m_wfeInput;
    unsigned int m_nCRC;
    unsigned int m_nStoredCRC;
    int m_nSpecialCodes;

    int SeekToFrame(int nFrameIndex);
    void DecodeBlocksToFrameBuffer(int nBlocks);
    int FillFrameBuffer();
    void StartFrame();
    void EndFrame();
    int InitializeDecompressor();

    // more decoding components
    CSmartPtr<CAPEInfo> m_spAPEInfo;
    CSmartPtr<CUnBitArrayBase> m_spUnBitArray;
    UNBIT_ARRAY_STATE m_BitArrayStateX;
    UNBIT_ARRAY_STATE m_BitArrayStateY;

    CSmartPtr<IPredictorDecompress> m_spNewPredictorX;
    CSmartPtr<IPredictorDecompress> m_spNewPredictorY;

    int m_nLastX;

    // decoding buffer
    BOOL m_bErrorDecodingCurrentFrame;
    int m_nCurrentFrameBufferBlock;
    int m_nFrameBufferFinishedBlocks;
    CCircleBuffer m_cbFrameBuffer;

    int m_nVersion;
    int m_nCompressionLevel;
    int m_nBlocksPerFrame;
    int m_nBitsPerSample;

    friend class CUnBitArrayJMAC;

    JNIEnv* env;
    jobject ioObject;
    jobject thisObject;
};

#endif // #ifndef JMAC_APEDECOMPRESS_H
