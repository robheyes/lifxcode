import spock.lang.Specification

class ShortFrameTest extends Specification {

    def "Fills a buffer with target true"() {
        given:
        def shortFrame = new ShortFrame(true, 0xefbeadde as int)
        def buffer = new Buffer()
        when:
        shortFrame.fillBuffer(buffer)
        def expected = [0x00, 0x0034, 0x00de, 0x00ad, 0x00be, 0x00ef]
        then:
        buffer.contents() == expected
    }
}