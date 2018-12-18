import spock.lang.Specification

class ProtocolHeaderTest extends Specification {

    ProtocolHeader header
    Buffer buffer

    def setup() {
        buffer = new Buffer()
        header = new ProtocolHeader(12 as short)
    }

    def "Sets the type"() {
        when:
        header.fillBuffer(buffer)
        then:
        buffer.contents() == [
                0, 0, 0, 0,
                0, 0, 0, 0,
                12, 0,
                0, 0,
        ]
    }
}