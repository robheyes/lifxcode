import spock.lang.Specification


class ParserTest extends Specification {

    def "It creates a parser from a string and parses little-endian bytes"() {
        given:
        def parser = new Parser('thingy:2l')
        when:
        def result = parser.parse([0x30, 0x00] as List<Byte>)
        then:
        result == [
                thingy: 48,
        ]
    }

    def "It creates a parser from a string and parses big-endian bytes"() {
        given:
        def parser = new Parser('thingy:2b')
        when:
        def result = parser.parse([0x72, 0x73] as List<Byte>)
        then:
        result == [
                thingy: 0x7273,
        ]
    }

    def "It copes with unsigned longs"() {
        given:
        def parser = new Parser('test:1,thingy:4l')
        when:
        def result = parser.parse([0x10, 0xFC, 0xFD, 0xFE, 0xFF, 0xD0] as List<Byte>)
        then:
        result == [
                test: 0x10,
                thingy: 0xFFFEFDFC,
                remainder: [0xD0]
        ]
    }

    def "It creates a parser from a string and parses two shorts"() {
        given:
        def parser = new Parser('thingy:2l,thing2:2b')
        when:
        def result = parser.parse([0x72, 0x73, 0x74, 0x75] as List<Byte>)
        then:
        result == [
                thingy: 0x7372,
                thing2: 0x7475
        ]
    }


    def "It copes with an omitted type specifier"() {
        given:
        def parser = new Parser('thingy:2,thing2:2b')
        when:
        def result = parser.parse([0x72, 0x73, 0x74, 0x75] as List<Byte>)
        then:
        result == [
                thingy: 0x7372,
                thing2: 0x7475
        ]
    }

    def "It copes with an underscore in the name"() {
        given:
        def parser = new Parser('thing_1:2,thing_2:2b')
        when:
        def result = parser.parse([0x72, 0x73, 0x74, 0x75] as List<Byte>)
        then:
        result == [
                thing_1: 0x7372,
                thing_2: 0x7475
        ]
    }

    def "It creates a parser from a string and parses a byte buffer little-endian shorts"() {
        given:
        def parser = new Parser('thingy:2b,thing2:12a')
        when:
        def result = parser.parse([0x72, 0x73, 0x74, 0x75, 0x76, 0x77, 0x74, 0x75, 0x76, 0x77, 0x74, 0x75, 0x76, 0x77] as List<Byte>)
        then:
        result == [
                thingy: 0x7273,
                thing2: [0x74, 0x75, 0x76, 0x77, 0x74, 0x75, 0x76, 0x77, 0x74, 0x75, 0x76, 0x77]
        ]
    }

    def "It appends the remainder"() {
        given:
        def parser = new Parser('thingy:2b')
        when:
        def result = parser.parse([0x72, 0x73, 0x74, 0x75, 0x76, 0x77, 0x74, 0x75, 0x76, 0x77, 0x74, 0x75, 0x76, 0x77] as List<Byte>)
        then:
        result == [
                thingy   : 0x7273,
                remainder: [0x74, 0x75, 0x76, 0x77, 0x74, 0x75, 0x76, 0x77, 0x74, 0x75, 0x76, 0x77]
        ]
    }


}