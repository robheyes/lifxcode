class Parser {
    private final List<Map> descriptor

    Parser(String descriptor) {
        this.descriptor = descriptor.findAll(~/(\w+):(\d+)([aAbBlL]?)/) {
            full ->
                [
                        endian: full[3].toUpperCase(),
                        bytes : full[2],
                        name  : full[1],
                ]
        }
    }


    Map parse(List<Byte> bytes) {
        Map result = new HashMap();
        int offset = 0
        descriptor.each { item ->
            int nextOffset = offset + (item.bytes as int)
            def data = bytes[offset..nextOffset - 1]
            offset = nextOffset
            // assume big endian for now
            if ('A' == item.endian) {
                result.put(item.name, data)
                return
            }
            if ('B' != item.endian) {
                data = data.reverse()
            }

            long value = 0
            data.each { value = (value << 8) | it }
            result.put(item.name, value)
        }
        if (offset < bytes.size()) {
            result.put('remainder', bytes[offset..-1])
        }
        return result
    }
}
