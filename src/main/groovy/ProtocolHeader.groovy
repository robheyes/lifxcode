class ProtocolHeader {
    private Buffer theBuffer

    ProtocolHeader(Buffer buffer) {
        theBuffer = buffer
    }

    def setType(int value) {
        theBuffer.addMultiple(0 as byte, 8)
        theBuffer.addInt(value)
        theBuffer.addInt(0)
    }
}
