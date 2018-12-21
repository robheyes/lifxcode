class Buffer {
    private theBuffer = []

    def size() {
        theBuffer.size()
    }

    def contents() {
        theBuffer.each { it as byte }
    }

    def addByte(value) {
        theBuffer.add(Byte.toUnsignedInt(value))
    }

    def addShort(Short value) {
        def lower = value & 0xff
        addByte(lower as byte)
        addByte(((value - lower) >>> 8) as byte)
    }

    def addInt(Integer value) {
        def lower = value & 0xffff
        addShort(lower as short)
        addShort(Integer.divideUnsigned(value - lower, 0x10000) as short)
    }

    def addLong(Long value) {
        def lower = value & 0xffffffff
        addInt(lower as int)
        addInt(Long.divideUnsigned(value - lower, 0x100000000) as int)
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
        for (int i = 0; i < count; i++) {
            addByte(value)
        }
    }
}
