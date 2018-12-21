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

    def "Fills a buffer with target false"() {
        given:
        def shortFrame = new ShortFrame(false, 0xdeadbeef as int)
        def buffer = new Buffer()
        when:
        shortFrame.fillBuffer(buffer)
        def expected = [0x00, 0x0014, 0x00ef, 0x00be, 0x00ad, 0x00de]
        then:
        buffer.contents() == expected
    }
}