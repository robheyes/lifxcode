class ProtocolHeader {

    private final short messageType

    ProtocolHeader(short messageType) {
        this.messageType = messageType
    }

    def fillBuffer(Buffer theBuffer) {
        theBuffer.addByteCopies(0 as byte, 8)
        theBuffer.add(messageType)
        theBuffer.add(0 as short)
    }
}
