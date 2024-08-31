TARGET   = jmac
MAC_SDK=/usr/local/src/MAC_1076_SDK
INCLUDES = -I${JAVA_HOME}/include -I${JAVA_HOME}/include/darwin -I${MAC_SDK}/Source/MACLib -I${MAC_SDK}/Source/Shared
CPPOPT   = -O3 -D__GNUC_IA32__ -DBUILD_CROSS_PLATFORM -DPLATFORM_APPLE -DAPE_ENABLE_CIRCLE_BUFFER_WRITE
COMPILER = gcc

SOURCEFILES = 			\
APEDecompressJMAC.o		\
jmac.o				\
UnBitArrayJMAC.o

$(TARGET): $(SOURCEFILES)
	$(COMPILER) -lstdc++ -dynamiclib -L/usr/local/lib -lMAC $(CPPOPT) $(INCLUDES) -o lib$(TARGET).dylib $(SOURCEFILES)

APEDecompressJMAC.o: APEDecompressJMAC.cpp
	$(COMPILER) -c $(CPPOPT) $(INCLUDES) -o APEDecompressJMAC.o       APEDecompressJMAC.cpp			

jmac.o: jmac.cpp
	$(COMPILER) -c $(CPPOPT) $(INCLUDES) -o jmac.o       jmac.cpp		

UnBitArrayJMAC.o: UnBitArrayJMAC.cpp
	$(COMPILER) -c $(CPPOPT) $(INCLUDES) -o UnBitArrayJMAC.o       UnBitArrayJMAC.cpp
