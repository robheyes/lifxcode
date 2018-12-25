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
        buffer.add(value)
        then:
        buffer.size() == 1
    }

    def "Buffer contents returns the data"() {
        when:
        byte value = 0x39
        buffer.add(value)
        then:
        buffer.contents() == [0x0039]
    }

    def "Buffer adds a byte of 0xff"() {
        when:
        byte value = 0xff as byte
        buffer.add(value)
        then:
        buffer.contents() == [0x00ff]
    }

    def "Buffer has size 2 after adding a short"() {
        when:
        buffer.add(0x393a as short)
        then:
        buffer.size() == 2
    }

    def "Buffer has size 4 after adding an int"() {
        when:
        buffer.add(0x393a3b3c)
        then:
        buffer.size() == 4
    }

    def "Buffer has size 8 after adding a long"() {
        when:
        buffer.add(0x393a3b3c as long)
        then:
        buffer.size() == 8
    }

    def "Buffer contents are in little endian format after adding a short"() {
        when:
        buffer.add(0x393a as short)
        then:
        buffer.contents() == ([0x003a, 0x0039] )
    }

    def "Buffer contents are in little endian format after adding an int"() {
        when:
        buffer.add(0x393a3b3c)
        then:
        buffer.contents() == [0x003c, 0x003b, 0x003a, 0x0039]
    }

    def "Buffer contents are in little endian format after adding a long"() {
        when:
        buffer.add(0x393a3b3c3d3e3f40)
        then:
        buffer.contents() == [0x0040, 0x003f, 0x003e, 0x003d, 0x003c, 0x003b, 0x003a, 0x0039]
    }

    def "Buffer has size 3 after adding a 3 byte array"() {
        when:
        buffer.add([0x01, 0x02, 0x03] as byte[])
        then:
        buffer.size() == 3
    }

    def "Buffer contents are in correct order after adding a 3 byte array"() {
        when:
        buffer.add([0x01, 0x02, 0x03] as byte[])
        then:
        buffer.contents() == [0x0001, 0x0002, 0x0003]
    }

    def "Buffer can add another buffer"() {
        given:
        def other = new Buffer()
        and:
        other.add([0x39, 0x3a] as byte[])
        and:
        buffer.add(0x38 as byte)
        when:
        buffer.add(other)
        then:
        buffer.contents() == [0x0038, 0x0039, 0x003a]
    }

    def "Adding an empty buffer has no impact"() {
        given:
        def other = new Buffer()
        and:
        buffer.add(0x38 as byte)
        when:
        buffer.add(other)
        then:
        buffer.contents() == [0x0038]
    }

    def "It fills with multiple copies of a byte"() {
        when:
        buffer.addByteCopies(0xaa as byte, 4)
        then:
        buffer.contents() == [0x00aa,0x00aa,0x00aa,0x00aa]
    }
}