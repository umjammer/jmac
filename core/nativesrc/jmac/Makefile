TARGET   = jmac
INCLUDES = -I/usr/local/java/include -I/usr/local/java/include/linux -I../mac/Source/MACLib -I../mac/Source/Shared
CPPOPT   = -s -O3 -D__GNUC_IA32__ -DBUILD_CROSS_PLATFORM
COMPILER = gcc

SOURCEFILES = 			\
APEDecompressJMAC.o		\
jmac.o				\
UnBitArrayJMAC.o		\
../mac/Shared/libmac.a

$(TARGET): $(SOURCEFILES)
	$(COMPILER) -lstdc++ -shared $(CPPOPT) $(INCLUDES) -o lib$(TARGET).so $(SOURCEFILES)

APEDecompressJMAC.o: APEDecompressJMAC.cpp
	$(COMPILER) -c $(CPPOPT) $(INCLUDES) -o APEDecompressJMAC.o       APEDecompressJMAC.cpp			

jmac.o: jmac.cpp
	$(COMPILER) -c $(CPPOPT) $(INCLUDES) -o jmac.o       jmac.cpp		

UnBitArrayJMAC.o: UnBitArrayJMAC.cpp
	$(COMPILER) -c $(CPPOPT) $(INCLUDES) -o UnBitArrayJMAC.o       UnBitArrayJMAC.cpp
