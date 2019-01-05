import spock.lang.Specification


class ParserTest extends Specification {

    def "It parses a single byte"() {
        given:
        def parser = new Parser([
                [
                        endian: Parser.Endianness.BIG,
                        bytes : 1,
                        name  : 'thing'
                ]
        ])
        when:
        def result = parser.parse([0x72, 0x73] as List<Byte>)
        then:
        result == [
                thing: 0x72 as byte
        ]
    }

    def "It parses two separate successive bytes"() {
        given:
        def parser = new Parser([
                [
                        endian: Parser.Endianness.BIG,
                        bytes : 1,
                        name  : 'thing1'
                ],
                [
                        endian: Parser.Endianness.BIG,
                        bytes : 1,
                        name  : 'thing2'
                ],
        ])
        when:
        def result = parser.parse([0x72, 0x73] as List<Byte>)
        then:
        result == [
                thing1: 0x72 as byte,
                thing2: 0x73 as byte,
        ]
    }

    def "It parses two successive bytes as a short"() {
        given:
        def parser = new Parser([
                [
                        endian: Parser.Endianness.BIG,
                        bytes : 2,
                        name  : 'thing'
                ],
        ])
        when:
        def result = parser.parse([0x72, 0x73] as List<Byte>)
        then:
        result == [
                thing: 0x7372 as short,
        ]
    }

    def "It parses two successive bytes as a short in little-endian"() {
        given:
        def parser = new Parser([
                [
                        endian: Parser.Endianness.LITTLE,
                        bytes : 2,
                        name  : 'thing'
                ],
        ])
        when:
        def result = parser.parse([0x72, 0x73] as List<Byte>)
        then:
        result == [
                thing: 0x7273 as short,
        ]
    }

    def "It creates a parser from a string and parses big-endian bytes"() {
        given:
        def parser = new Parser('thingy:2b')
        when:
        def result = parser.parse([0x72, 0x73] as List<Byte>)
        then:
        result == [
                thingy: 0x7372,
        ]
    }
}