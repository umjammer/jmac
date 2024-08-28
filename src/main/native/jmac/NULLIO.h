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

#ifndef JMAC_NULLIO_H
#define JMAC_NULLIO_H

class CNULLIO : public CIO {
  public:

    // construction / destruction
    CNULLIO() {
    };
    ~CNULLIO() {
    };

    // open / close
    int Open(const wchar_t* pName) {
        return 0;
    };
    int Close() {
        return 0;
    };

    // read / write
    int Read(void* pBuffer, unsigned int nBytesToRead, unsigned int* pBytesRead) {
        return 0;
    };
    int Write(const void* pBuffer, unsigned int nBytesToWrite, unsigned int* pBytesWritten) {
        return 0;
    };

    // seek
    int Seek(int nDistance, unsigned int nMoveMode) {
        return 0;
    };

    // other functions
    int SetEOF() {
        return 0;
    };

    // creation / destruction
    int Create(const wchar_t* pName) {
        return 0;
    };
    int Delete() {
        return 0;
    };

    // attributes
    int GetPosition() {
        return 0;
    };
    int GetSize() {
        return 0;
    };
    int GetName(wchar_t* pBuffer) {
        return 0;
    };
};

#endif //JMAC_NULLIO_H
