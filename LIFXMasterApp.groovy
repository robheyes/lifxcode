/**
 *
 * Copyright 2018, 2019 Robert Heyes. All Rights Reserved
 *
 *  This software if free for Private Use. You may use and modify the software without distributing it.
 *  You may not grant a sublicense to modify and distribute this software to third parties.
 *  Software is provided without warranty and your use of it is at your own risk.
 *
 */

definition(
        name: "LIFX discovery app",
        namespace: "robheyes",
        author: "Robert Alan Heyes",
        description: "Discover LIFX devices",
        category: "Automation",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",

)

preferences {
    page(name: "main")
}

def main() {
    dynamicPage(name: "main", title: "LIFX discovery", uninstall: true, install: true) {
        section("Settings...") {
            input "logEnable", "bool", title: "Enable debug logging", required: false
            input "refreshBtn", "button", title: "Refresh"
        }
    }
}

def appButtonHandler(btn){
    log.debug "appButtonHandler called with ${btn}"
    state.btnCall = btn
    if(state.btnCall == "refreshBtn"){
        refresh()
    }

}

def refresh() {
    def buffer = []
    def getServiceSequence = makePacket(buffer, 0, messageTypes().DEVICE.GET_SERVICE, false, false, [])
    def rawBytes = asByteArray(buffer)
    log.debug "raw bytes: ${rawBytes}"
    String stringBytes = hubitat.helper.HexUtils.byteArrayToHexString(rawBytes)
    log.debug "sending bytes: ${stringBytes}"
    def myHubAction = new hubitat.device.HubAction(
            stringBytes,
            hubitat.device.Protocol.LAN,
            [
                    type              : hubitat.device.HubAction.Type.LAN_TYPE_UDPCLIENT,
                    destinationAddress: "255.255.255.255:56700",
                    encoding          : hubitat.device.HubAction.Encoding.HEX_STRING
            ]
    )
    def response = sendHubCommand(myHubAction)
    log.debug "response from sendHubCommand ${response}"
}

def static messageTypes() {
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

def installed() {
    log.debug "LIFX installed"
    initialize()
}

def updated() {
    initialize()
}

def initialize() {
    state.sequence = 1
    refresh()
}

def parse(String description) {
    log.debug "parse description: ${description}"
}

// fills the buffer with the LIFX packet
private byte makePacket(List buffer, long targetAddress, int messageType, Boolean ackRequired, Boolean responseRequired, List payload) {
    def lastSequence = sequenceNumber()
    createFrame(buffer, targetAddress == 0)
    createFrameAddress(buffer, targetAddress, ackRequired, responseRequired, lastSequence)
    createProtocolHeader(buffer, messageType as short)
    createPayload(buffer, payload as byte[])

    put(buffer, 0, buffer.size() as short)
    return lastSequence
}

private byte sequenceNumber() {
    state.sequence = (state.sequence + 1) % 256
}

def createFrame(List buffer, boolean tagged) {
    int LIFX_HABITAT_SOURCE = 0xFFFEFDFC
    add(buffer, 0 as short)
    add(buffer, 0x00 as byte)
    add(buffer, (tagged ? 0x34 : 0x14) as byte)
    add(buffer, LIFX_HABITAT_SOURCE)
}

def createFrameAddress(List buffer, long target, boolean ackRequired, boolean responseRequired, byte sequenceNumber) {
    add(buffer, target)
    fill(buffer, 0 as byte, 6)
    add(buffer, ((ackRequired ? 0x02 : 0) | (responseRequired ? 0x01 : 0)) as byte)
    add(buffer, sequenceNumber)
}

def createProtocolHeader(List buffer, short messageType) {
    fill(buffer, 0 as byte, 8)
    add(buffer, messageType)
    add(buffer, 0 as short)
}

def createPayload(List buffer, byte[] payload) {
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
