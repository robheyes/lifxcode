import spock.lang.Specification


class BufferTest extends Specification {
    def "New buffer is empty"() {
        given:
        def buffer = new Buffer()
        when:
        def size = buffer.size()
        then:
        size == 0
    }

    def "New buffer has empty contents"() {
        given:
        def buffer = new Buffer();
        when:
        def content = buffer.contents()
        then:
        content == []
    }

    def "Buffer has size 1 after adding a byte"() {
        given:
        def buffer = new Buffer()
        when:
        byte value = 0x3a
        buffer.addByte(value)
        then:
        buffer.size() == 1
    }

    def "Buffer contents returns a string with data"() {
        given:
        def buffer = new Buffer()
        when:
        byte value = 0x39
        buffer.addByte(value)
        then:
        buffer.contents() == [0x39]
    }

    def "Buffer has size 2 after adding an int"() {
        given:
        def buffer = new Buffer()
        when:
        buffer.addInt(0x393a)
        then:
        buffer.size() == 2
    }

    def "Buffer has size 4 after adding a long"() {
        given:
        def buffer = new Buffer()
        when:
        buffer.addLong(0x393a3b3c)
        then:
        buffer.size() == 4
    }

    def "Buffer contents are in little endian format after adding an int"() {
        given:
        def buffer = new Buffer()
        when:
        buffer.addInt(0x393a)
        byte[] expected = [0x3a, 0x39]
        then:
        buffer.contents() == expected
    }

    def "Buffer contents are in little endian format after adding a long"() {
        given:
        def buffer = new Buffer()
        when:
        buffer.addLong(0x393a3b3c)
        byte[] expected = [0x3c, 0x3b, 0x3a, 0x39]
        then:
        buffer.contents() == expected
    }

    def "Buffer has size 3 after adding a 3 byte array"() {
        given:
        def buffer = new Buffer()
        when:
        buffer.addBytes([0x01, 0x02, 0x03] as byte[])
        then:
        buffer.size() == 3
    }

    def "Buffer contents are in correct order after adding a 3 byte array"() {
        given:
        def buffer = new Buffer()
        when:
        buffer.addBytes([0x01, 0x02, 0x03] as byte[])
        then:
        buffer.contents() == [0x01, 0x02, 0x03] as byte[]
    }
}