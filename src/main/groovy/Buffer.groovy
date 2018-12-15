class Buffer {
    private LinkedList<Byte> theBuffer = []

    def size() {
        theBuffer.size()
    }

    def contents() {
        theBuffer
    }

    def addByte(byte value) {
        theBuffer << value
    }

    def addInt(int value) {
        addByte((value % 256) as byte)
        addByte((value / 256) as byte)
    }

    def addLong(long value) {
        addInt((value % 65536) as int)
        addInt((value / 65536) as int)
    }

    def addBytes(byte[] values) {
        for (value in values) {
            addByte(value)
        }
    }
}
