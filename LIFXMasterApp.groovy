
/**
 *
 * Copyright 2018, 2019 Robert Heyes. All Rights Reserved
 *
 *  This software if free for Private Use. You may use and modify the software without distributing it.
 *  You may not grant a sublicense to modify and distribute this software to third parties.
 *  Software is provided without warranty and your use of it is at your own risk.
 *
 */
import groovy.transform.Field

definition(name: "LIFX discovery", namespace: "robheyes", author: "Robert Alan Heyes") {
//	capability: "Switch"
    capability "Polling"
    capability "Refresh"
    command "refresh"
}
def updated() {
    log.debug "LIFX updating"
    initialize()
}


def installed() {
    log.debug "LIFX installed"
    initialize()
}

preferences {
    input "logEnable", "bool", title: "Enable debug logging", required: false
    //input "refreshBtn", "button", title: "Refresh"
}

@Field List<Map> headerDescriptor = makeDescriptor('size:2l,misc:2l,source:4l,target:8a,frame_reserved:6a,flags:1,sequence:1,protocol_reserved:8a,type:2l,protocol_reserved2')

def initialize() {
    state.sequence = 1
    state.deviceCount = 0
    def localIP = getHubIP()

    log.debug "localIP: ${localIP}"
    refresh()
}

def refresh() {
    String subnet = getSubnet()
    if (!subnet) {
        return
    }
    state.deviceCount = 0
    // use one packet, don't care about the sequence number
    def packet = makeStatePacket([0, 0, 0, 0, 0, 0] as byte[])
    1.upto(254) {
        def ipAddress = subnet + it
        //log.debug "Going to test ${ipAddress}"
        sendPacket(packet.buffer, ipAddress)
    }
}

def parse(String description) {
    state.deviceCount = state.deviceCount + 1
    Map deviceParams = new HashMap()
    description.findAll(~/(\w+):(\w+)/) {
        deviceParams.putAt(it[1], it[2])
    }
    log.debug("Payload: ${deviceParams.payload}")

    def parsed = parseBytes(headerDescriptor, hubitat.helper.HexUtils.hexStringToByteArray(deviceParams.payload) as List<Byte>)
    log.debug("Parsed: ${parsed}")
}

def poll() {
//    def packet = makeEchoPacket([0,0,0,0,0,0] as byte[])

    def packet = makeStatePacket([0, 0, 0, 0, 0, 0] as byte[])
    sendPacket(packet.buffer, "192.168.1.45")
//    sendPacket(packet.buffer, "255.255.255.255")

    log.debug "Sent packet with sequence ${packet.sequence}"
}

private def static messageTypes() {
    return [
            DEVICE: [
                    GET_SERVICE        : 2,
                    STATE_SERVICE      : 3,
                    GET_HOST_INFO      : 12,
                    STATE_HOST_INFO    : 13,
                    GET_HOST_FIRMWARE  : 14,
                    STATE_HOST_FIRMWARE: 15,
                    GET_WIFI_INFO      : 16,
                    STATE_WIFI_INFO    : 17,
                    GET_WIFI_FIRMWARE  : 18,
                    STATE_WIFI_FIRMWARE: 19,
                    GET_POWER          : 20,
                    SET_POWER          : 21,
                    STATE_POWER        : 22,
                    GET_LABEL          : 23,
                    SET_LABEL          : 24,
                    STATE_LABEL        : 25,
                    GET_VERSION        : 32,
                    STATE_VERSION      : 33,
                    GET_INFO           : 34,
                    STATE_INFO         : 35,
                    ACKNOWLEDGEMENT    : 45,
                    GET_LOCATION       : 48,
                    SET_LOCATION       : 49,
                    STATE_LOCATION     : 50,
                    GET_GROUP          : 51,
                    SET_GROUP          : 52,
                    STATE_GROUP        : 53,
                    ECHO_REQUEST       : 58,
                    ECHO_RESPONSE      : 59
            ],
            LIGHT : [
                    GET_STATE            : 101,
                    SET_COLOR            : 102,
                    SET_WAVEFORM         : 103,
                    SET_WAVEFORM_OPTIONAL: 119,
                    STATE_RESPONSE       : 107,
                    GET_POWER            : 116,
                    SET_POWER            : 117,
                    STATE_POWER          : 118,
                    GET_INFRARED         : 120,
                    STATE_INFRARED       : 121,
                    SET_INFRARED         : 122,
            ]
    ]
}

Map parseBytes(List<Map> descriptor, List<Byte> bytes) {
    Map result = new HashMap();
    int offset = 0
    descriptor.each { item ->
        int nextOffset = offset + (item.bytes as int)
        def data = bytes[offset..nextOffset - 1]
        offset = nextOffset
        // assume big endian for now
        if ('A' == item.endian) {
            result.put(item.name, data)
            return
        }
        if ('B' != item.endian) {
            data = data.reverse()
        }

        long value = 0
        data.each { value = (value << 8) | it }
        result.put(item.name, value)
    }
    if (offset < bytes.size()) {
        result.put('remainder', bytes[offset..-1])
    }
    return result
}

private List<Map> makeDescriptor(String desc) {
    desc.findAll(~/(\w+):(\d+)([aAbBlL]?)/) {
        full ->
            [
                    endian: full[3].toUpperCase(),
                    bytes : full[2],
                    name  : full[1],
            ]
    }
}

private String getSubnet() {
    def ip = getHubIP()
    def m = ip =~ /^(\d{1,3}\.\d{1,3}\.\d{1,3}\.)\d{1,3}/
    def partialIp = null
    if (!m) {
        log.debug('ip does not match pattern')
        return null
    }
    return m.group(1)
}

private Long makeTarget(List macAddress) {
    return macAddress.inject(0L) { Long current, Long val -> current * 256 + val }
}

private sendPacket(List buffer, String ipAddress) {
    def rawBytes = asByteArray(buffer)
//    log.debug "raw bytes: ${rawBytes}"
    String stringBytes = hubitat.helper.HexUtils.byteArrayToHexString(rawBytes)
//    log.debug "sending bytes: ${stringBytes} to ${ipAddress}"
    sendHubCommand(
            new hubitat.device.HubAction(
                    stringBytes,
                    hubitat.device.Protocol.LAN,
                    [
                            type              : hubitat.device.HubAction.Type.LAN_TYPE_UDPCLIENT,
                            destinationAddress: ipAddress + ":56700",
                            encoding          : hubitat.device.HubAction.Encoding.HEX_STRING
                    ]
            )
    )
}

private Map makeGetDevicePacket() {
    def buffer = []
    def getServiceSequence = makePacket(buffer, [0, 0, 0, 0, 0, 0] as byte[], messageTypes().DEVICE.GET_SERVICE, false, true, [])
    return [sequence: getServiceSequence, buffer: buffer]
}

private Map makeEchoPacket(byte[] target) {
    def payload = []
    fill(payload, 0xAA as byte, 64)
    def buffer = []
    def echoSequence = makePacket(buffer, target, messageTypes().DEVICE.ECHO_REQUEST, false, false, payload)
    return [sequence: echoSequence, buffer: buffer]
}

private Map makeStatePacket(byte[] target) {
    def payload = []
    def buffer = []
    def echoSequence = makePacket(buffer, target, messageTypes().DEVICE.GET_VERSION, false, false, payload)
    return [sequence: echoSequence, buffer: buffer]
}

private String getHubIP() {
    def hub = location.hubs[0]

    hub.localIP
}

// fills the buffer with the LIFX packet
private byte makePacket(List buffer, byte[] targetAddress, int messageType, Boolean ackRequired, Boolean responseRequired, List payload) {
    def lastSequence = sequenceNumber()
    createFrame(buffer, targetAddress.every { it == 0 })
    createFrameAddress(buffer, targetAddress, ackRequired, responseRequired, lastSequence)
    createProtocolHeader(buffer, messageType as short)
    createPayload(buffer, payload as byte[])

    put(buffer, 0, buffer.size() as short)
    return lastSequence
}

private byte sequenceNumber() {
    state.sequence = (state.sequence + 1) % 256
}

private def createFrame(List buffer, boolean tagged) {
    int LIFX_HABITAT_SOURCE = 0xFFFEFDFC
    add(buffer, 0 as short)
    add(buffer, 0x00 as byte)
    add(buffer, (tagged ? 0x34 : 0x14) as byte)
    add(buffer, LIFX_HABITAT_SOURCE)
}

private def createFrameAddress(List buffer, byte[] target, boolean ackRequired, boolean responseRequired, byte sequenceNumber) {
    add(buffer, target)
    add(buffer, 0 as short)
    fill(buffer, 0 as byte, 6)
    add(buffer, ((ackRequired ? 0x02 : 0) | (responseRequired ? 0x01 : 0)) as byte)
    add(buffer, sequenceNumber)
}

private def createProtocolHeader(List buffer, short messageType) {
    fill(buffer, 0 as byte, 8)
    add(buffer, messageType)
    add(buffer, 0 as short)
}

private def createPayload(List buffer, byte[] payload) {
    add(buffer, payload)
}

private static byte[] asByteArray(List buffer) {
    (buffer.each { it as byte }) as byte[]
}

private static void add(List buffer, byte value) {
    buffer.add(Byte.toUnsignedInt(value))
}

private static void put(List buffer, int index, byte value) {
    buffer.set(index, Byte.toUnsignedInt(value))
}

private static void add(List buffer, short value) {
    def lower = value & 0xff
    add(buffer, lower as byte)
    add(buffer, ((value - lower) >>> 8) as byte)
}

private static void put(List buffer, int index, short value) {
    def lower = value & 0xff
    put(buffer, index, lower as byte)
    put(buffer, index + 1, ((value - lower) >>> 8) as byte)
}

private static void add(List buffer, int value) {
    def lower = value & 0xffff
    add(buffer, lower as short)
    add(buffer, Integer.divideUnsigned(value - lower, 0x10000) as short)
}

private static void add(List buffer, long value) {
    def lower = value & 0xffffffff
    add(buffer, lower as int)
    add(buffer, Long.divideUnsigned(value - lower, 0x100000000) as int)
}

private static void add(List buffer, byte[] values) {
    for (value in values) {
        add(buffer, value)
    }
}

private static void fill(List buffer, byte value, int count) {
    for (int i = 0; i < count; i++) {
        add(buffer, value)
    }
}
