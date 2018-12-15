import spock.lang.Specification


class BufferTest extends Specification {
    private Buffer buffer

    def setup() {
        buffer = new Buffer()
    }

    def "New buffer is empty"() {
        when:
        def size = buffer.size()
        then:
        size == 0
    }

    def "New buffer has empty contents"() {
        when:
        def content = buffer.contents()
        then:
        content == []
    }

    def "Buffer has size 1 after adding a byte"() {
        when:
        byte value = 0x3a
        buffer.addByte(value)
        then:
        buffer.size() == 1
    }

    def "Buffer contents returns a string with data"() {
        when:
        byte value = 0x39
        buffer.addByte(value)
        then:
        buffer.contents() == [0x39] as byte[]
    }

    def "Buffer has size 2 after adding an int"() {
        when:
        buffer.addInt(0x393a)
        then:
        buffer.size() == 2
    }

    def "Buffer has size 4 after adding a long"() {
        when:
        buffer.addLong(0x393a3b3c)
        then:
        buffer.size() == 4
    }

    def "Buffer contents are in little endian format after adding an int"() {
        when:
        buffer.addInt(0x393a)
        then:
        buffer.contents() == ([0x3a, 0x39] as byte[])
    }

    def "Buffer contents are in little endian format after adding a long"() {
        when:
        buffer.addLong(0x393a3b3c)
        then:
        buffer.contents() == ([0x3c, 0x3b, 0x3a, 0x39] as byte[])
    }

    def "Buffer has size 3 after adding a 3 byte array"() {
        when:
        buffer.addBytes([0x01, 0x02, 0x03] as byte[])
        then:
        buffer.size() == 3
    }

    def "Buffer contents are in correct order after adding a 3 byte array"() {
        when:
        buffer.addBytes([0x01, 0x02, 0x03] as byte[])
        then:
        buffer.contents() == [0x01, 0x02, 0x03] as byte[]
    }

    def "Buffer can add another buffer"() {
        given:
        def other = new Buffer()
        and:
        other.addBytes([0x39, 0x3a] as byte[])
        and:
        buffer.addByte(0x38 as byte)
        when:
        buffer.addBuffer(other)
        then:
        buffer.contents() == [0x38, 0x39, 0x3a]
    }

    def "It fills with multiple copies of a byte"() {
        when:
        buffer.addMultiple(0xaa as byte, 4)
        then:
        buffer.contents() == [0xaa,0xaa,0xaa,0xaa] as byte[]
    }
}