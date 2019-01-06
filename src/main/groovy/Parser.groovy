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

            def data = bytes.subList(offset, nextOffset)
            offset = nextOffset
            // assume big endian for now
            if ('A' == item.endian) {
                result.put(item.name, data)
                return
            }
            if ('B' != item.endian) {
                data = data.reverse()
            }

            BigInteger value = 0
            data.each { value = (value * 256) + (it & 0xff) }
            switch (item.bytes) {
                case 1:
                    result.put(item.name, (value & 0xFF) as byte)
                    break
                case 2:
                    result.put(item.name, (value & 0xFFFF) as short)
                    break
                case 3: case 4:
                    result.put(item.name, (value & 0xFFFFFFFF) as int)
                    break
                default:
                    result.put(item.name, (value & 0xFFFFFFFFFFFFFFFF) as long)
            }
        }
        if (offset < bytes.size()) {
            result.put('remainder', bytes[offset..-1])
        }
        return result
    }
}
