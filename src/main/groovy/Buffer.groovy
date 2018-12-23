class Buffer {
    private theBuffer = []

    def size() {
        theBuffer.size()
    }

    def contents() {
        asByteList(theBuffer)
    }

    private List<Byte> asByteList(List buffer) {
        buffer.each { it as byte }
    }

    private List byteAdd(List buffer, value) {
        buffer.add(Byte.toUnsignedInt(value))
        return buffer
    }

    private List shortAdd(List buffer, short value) {
        def lower = value & 0xff
        byteAdd(buffer, lower as byte)
        byteAdd(buffer, ((value - lower) >>> 8) as byte)
    }

    private List intAdd(List buffer, int value) {
        def lower = value & 0xffff
        shortAdd(buffer, lower as short)
        shortAdd(buffer, Integer.divideUnsigned(value - lower, 0x10000) as short)
    }

    private List longAdd(List buffer, long value) {
        def lower = value & 0xffffffff
        intAdd(buffer, lower as int)
        intAdd(buffer, Long.divideUnsigned(value - lower, 0x100000000) as int)
    }

    private List bytesAdd(List buffer, byte[] values) {
        for (value in values) {
            byteAdd(buffer, value)
        }
        return buffer
    }

    private void byteFill(List buffer, byte value, int count) {
        for (int i = 0; i < count; i++) {
            byteAdd(buffer, value)
        }
    }

    def addByte(value) {
        byteAdd(theBuffer, value)
    }

    def addShort(Short value) {
        shortAdd(theBuffer, value)
    }

    def addInt(Integer value) {
        intAdd(theBuffer, value)
    }

    def addLong(Long value) {
        def buffer = theBuffer
        longAdd(buffer, value)
    }

    def addBytes(byte[] values) {
        bytesAdd(theBuffer, values)
    }

    def addBuffer(Buffer buffer) {
        bytesAdd(theBuffer, buffer.contents().toArray() as byte[])
    }

    def addByteCopies(byte value, int count) {
        def buffer = theBuffer
        byteFill(buffer, value, count)
    }
}
