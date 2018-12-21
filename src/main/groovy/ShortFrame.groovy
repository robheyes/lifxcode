class ShortFrame {
    private final boolean target
    private final int source

    ShortFrame(boolean target, int source) {
        this.source = source
        this.target = target
    }

    def fillBuffer(Buffer buffer) {
        buffer.addByte(0x00 as byte)
        buffer.addByte(0x34 as byte)
        buffer.addInt(source)
    }
}
