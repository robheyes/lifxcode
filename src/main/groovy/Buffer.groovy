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

    def addShort(short value) {
        addByte((value % 256) as byte)
        addByte((value / 256) as byte)
    }

    def addInt(long value) {
        addShort((value % 65536) as short)
        addShort((value / 65536) as short)
    }

    def addLong(long value) {
        addInt((value % 4294967296) as int)
        addInt((value / 4294967296) as int)
    }

    def addBytes(byte[] values) {
        for (value in values) {
            addByte(value)
        }
    }

    def addBuffer(Buffer buffer) {
        addBytes(buffer.contents().toArray() as byte[])
    }

    def addByteCopies(byte value, int count) {
        for (int i = 0; i < count ; i++) {
            addByte(value)
        }
    }
}
