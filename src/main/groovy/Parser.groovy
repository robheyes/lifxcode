class Parser {
    private final List<Map> descriptor

    enum Endianness {
        BIG, LITTLE
    }

    Parser(List<Map> descriptor) {
        this.descriptor = descriptor
    }

    Parser(String descriptor) {
        this.descriptor = makeDescriptor(descriptor)
    }

    List<Map> makeDescriptor(CharSequence desc) {
        desc.findAll(~/(\w+):(\d+)([bBlL])/) {
            full ->
                [
                        endian: (full[3].toLowerCase() == 'b') ? Endianness.BIG : Endianness.LITTLE,
                        bytes : full[2],
                        name  : full[1],
                ]
        }
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
