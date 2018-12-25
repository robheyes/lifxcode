class ShortFrame {
    private final boolean tagged
    private final int source

    ShortFrame(boolean tagged, int source) {
        this.source = source
        this.tagged = tagged
    }

    def fillBuffer(Buffer buffer) {
        buffer.add(0x00 as byte)
        buffer.add((tagged ? 0x34 : 0x14) as byte)
        buffer.add(source)
    }
}
