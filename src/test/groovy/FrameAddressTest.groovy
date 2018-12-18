import spock.lang.Specification


class FrameAddressTest extends Specification {
    def "It fills the buffer with an all devices frame address"() {
        when:
        def frameAddress = new FrameAddress(0L, true, true, 0x97 as byte)
        def buffer = new Buffer()
        frameAddress.addToBuffer(buffer)
        byte[] expected = ([
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x03,
                0x97
        ] as byte[])
        then:
        buffer.contents() == expected
    }
}