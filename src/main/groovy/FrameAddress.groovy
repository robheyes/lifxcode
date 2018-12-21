class FrameAddress {
    private final long target
    private final boolean ackRequired
    private final boolean responseRequired
    private final byte sequenceNumber

    FrameAddress(long target, boolean ackRequired, boolean responseRequired, byte sequenceNumber) {
        this.sequenceNumber = sequenceNumber
        this.responseRequired = responseRequired
        this.ackRequired = ackRequired
        this.target = target
    }

    def addToBuffer(Buffer buffer) {
        buffer.addLong(target)
        buffer.addByteCopies(0 as byte, 6)
//        buffer.addByte(0x03 as byte)
        buffer.addByte(((ackRequired ? 0x02 : 0) | (responseRequired ? 0x01 : 0)) as byte)
        buffer.addByte(sequenceNumber)
    }
}
