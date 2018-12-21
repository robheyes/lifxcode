import spock.lang.Specification


class FrameAddressTest extends Specification {

    Buffer buffer

    def setup() {
        buffer = new Buffer()
    }

    def "It fills the buffer with an all devices frame address"() {
        when:
        def frameAddress = new FrameAddress(0L, true, true, 0x97 as byte)
        frameAddress.addToBuffer(buffer)
        def expected = ([
                0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
                0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
                0x0003,
                0x0097
        ])
        then:
        buffer.contents() == expected
    }

    def "It fills the buffer with an all devices frame address but no ack"() {
        when:
        def frameAddress = new FrameAddress(0L, false, true, 0x97 as byte)
        frameAddress.addToBuffer(buffer)
        def expected = ([
                0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
                0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
                0x0001,
                0x0097
        ])
        then:
        buffer.contents() == expected
    }

    def "It fills the buffer with an all devices frame address but no response"() {
        when:
        def frameAddress = new FrameAddress(0L, true, false, 0x97 as byte)
        frameAddress.addToBuffer(buffer)
        def expected = ([
                0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
                0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
                0x0002,
                0x0097
        ])
        then:
        buffer.contents() == expected
    }

    def "It fills the buffer with an all devices frame address but no ack or response"() {
        when:
        def frameAddress = new FrameAddress(0L, false, false, 0x97 as byte)
        frameAddress.addToBuffer(buffer)
        def expected = ([
                0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
                0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
                0x0000,
                0x0097
        ])
        then:
        buffer.contents() == expected
    }

    def "It fills the buffer with an all targeted frame address"() {
        when:
        def frameAddress = new FrameAddress(0x12345678ABCD, true, true, 0x97 as byte)
        frameAddress.addToBuffer(buffer)
        def expected = ([
                0x00cd, 0x00ab, 0x0078, 0x0056, 0x0034, 0x0012, 0x0000, 0x0000,
                0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
                0x0003,
                0x0097
        ])
        then:
        buffer.contents() == expected
    }

}