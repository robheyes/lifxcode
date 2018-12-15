import spock.lang.Specification

class ProtocolHeaderTest extends Specification {

    ProtocolHeader header
    Buffer buffer

    def setup() {
        buffer = new Buffer()
        header = new ProtocolHeader(buffer)
    }

    def "Sets the type"() {
        when:
        header.setType(0x0025)
        then:
        buffer.contents() == [
                0, 0, 0, 0,
                0, 0, 0, 0,
                0x25, 0,
                0, 0,
        ]
    }
}