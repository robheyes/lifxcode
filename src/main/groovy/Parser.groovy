class Parser {
    private final List<Map> descriptor

    enum Endianness {BIG, LITTLE}

    Parser(List<Map> descriptor) {
        this.descriptor = descriptor
    }

    Map parse(List<Byte> bytes) {
        Map result = new HashMap();
        int offset = 0
        descriptor.each { item ->
            int nextOffset = offset + (item['bytes'] as int)
            def data = bytes[offset..nextOffset - 1]
            offset = nextOffset
            // assume big endian for now
            long value = 0
            if (Endianness.BIG == item['endian']) {
                data = data.reverse()
            }
            data.each { value = value * 256 + it }
            result.put(item['name'], value)
        }
        return result
    }
}
