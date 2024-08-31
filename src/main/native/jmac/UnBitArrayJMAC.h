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

#ifndef JMAC_UNBITARRAY_H
#define JMAC_UNBITARRAY_H

#include <UnBitArray.h>
#include "UnBitArrayJMAC.h"

class CAPEDecompressJMAC;

class CUnBitArrayJMAC : public APE::CUnBitArray {
  public:

    // construction/destruction
    CUnBitArrayJMAC(CAPEDecompressJMAC* decoder, int nVersion, APE::int64 nFurthestReadByte);
    ~CUnBitArrayJMAC();

    virtual int FillBitArray();
    virtual int FillAndResetBitArray(int nFileLocation = -1, int nNewBitIndex = 0);

  private:

    int Seek(int nDistance);
    int Read(void* pBuffer, unsigned int nBytesToRead, unsigned int* pBytesRead);

    CAPEDecompressJMAC* m_APEDecompress;
    jbyteArray byteArray;

    friend class CAPEDecompressJMAC;
};

#endif // #ifndef JMAC_UNBITARRAY_H
