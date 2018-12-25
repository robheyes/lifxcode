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

    List add(List buffer, Byte value) {
        buffer.add(Byte.toUnsignedInt(value))
        return buffer
    }

    List add(List buffer, short value) {
        def lower = value & 0xff
        add(buffer, lower as byte)
        add(buffer, ((value - lower) >>> 8) as byte)
    }

    List add(List buffer, int value) {
        def lower = value & 0xffff
        add(buffer, lower as short)
        add(buffer, Integer.divideUnsigned(value - lower, 0x10000) as short)
    }

    List add(List buffer, long value) {
        def lower = value & 0xffffffff
        add(buffer, lower as int)
        add(buffer, Long.divideUnsigned(value - lower, 0x100000000) as int)
    }

    List add(List buffer, byte[] values) {
        for (value in values) {
            add(buffer, value)
        }
        return buffer
    }

    void fill(List buffer, byte value, int count) {
        for (int i = 0; i < count; i++) {
            add(buffer, value)
        }
    }

    def add(Byte value) {
        add(theBuffer, value)
    }

    def add(Short value) {
        add(theBuffer, value)
    }

    def add(Integer value) {
        add(theBuffer, value)
    }

    def add(Long value) {
        def buffer = theBuffer
        add(buffer, value)
    }

    def add(byte[] values) {
        add(theBuffer, values)
    }

    def add(Buffer buffer) {
        add(theBuffer, buffer.contents().toArray() as byte[])
    }

    def addByteCopies(byte value, int count) {
        def buffer = theBuffer
        fill(buffer, value, count)
    }
}
