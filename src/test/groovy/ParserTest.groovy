import spock.lang.Specification


class ParserTest extends Specification {

    def "It parses a single byte"() {
        given:
        def parser = new Parser([
                [
                        endian: 'B',
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
                        endian: 'B',
                        bytes : 1,
                        name  : 'thing1'
                ],
                [
                        endian: 'B',
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
                        endian: 'B',
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
                        endian: 'L',
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
    def "It creates a parser from a string and parses little-endian bytes"() {
        given:
        def parser = new Parser('thingy:2l')
        when:
        def result = parser.parse([0x72, 0x73] as List<Byte>)
        then:
        result == [
                thingy: 0x7273,
        ]
    }

    def "It creates a parser from a string and parses two little-endian shorts"() {
        given:
        def parser = new Parser('thingy:2l,thing2:2b')
        when:
        def result = parser.parse([0x72, 0x73, 0x74, 0x75] as List<Byte>)
        then:
        result == [
                thingy: 0x7273,
                thing2: 0x7574
        ]
    }

    def "It creates a parser from a string and parses a byte buffer little-endian shorts"() {
        given:
        def parser = new Parser('thingy:2l,thing2:12a')
        when:
        def result = parser.parse([0x72, 0x73, 0x74, 0x75, 0x76, 0x77, 0x74, 0x75, 0x76, 0x77,0x74, 0x75, 0x76, 0x77] as List<Byte>)
        then:
        result == [
                thingy: 0x7273,
                thing2: [0x74, 0x75, 0x76, 0x77, 0x74, 0x75, 0x76, 0x77,0x74, 0x75, 0x76, 0x77]
        ]
    }


}