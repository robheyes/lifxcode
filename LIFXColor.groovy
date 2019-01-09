import groovy.transform.Field

/**
 *
 * Copyright 2018, 2019 Robert Heyes. All Rights Reserved
 *
 *  This software if free for Private Use. You may use and modify the software without distributing it.
 *  You may not grant a sublicense to modify and distribute this software to third parties.
 *  Software is provided without warranty and your use of it is at your own risk.
 *
 */
definition(name: "LIFX Color", namespace: "robheyes", author: "Robert Alan Heyes") {
	capability "Light"
//	capability "LightEffect"
    capability "ColorControl"
    capability "ColorTemperature"
    capability "HealthCheck"
    capability "Polling"
    capability "Initialize"
    attribute "Label", "string"
    attribute "Group", "string"
    attribute "Location", "string"
}


preferences {
    input "logEnable", "bool", title: "Enable debug logging", required: false
}

@Field String hsbkDescriptor = 'hue:2l,saturation:2l,brightness:2l,kelvin:2l'

def installed() {
    requestInfo()
}

def initialize() {
    requestInfo()
}

def refresh() {

}

def poll() {
    sendCommand(messageTypes().LIGHT.GET_STATE)
}

def on() {
    def payload = []
    parent.add(payload, 65535 as short)
    sendCommand(parent.messageTypes().DEVICE.SET_POWER, payload)
}

def off() {
    def payload = []
    parent.add(payload, 0 as short)
    sendCommand(parent.messageTypes().DEVICE.SET_POWER, payload)
}

def setColor(Map colorMap) {

}

def setHue(number) {

}

def setSaturation(number) {

}

def setColorTemperature(temperature) {

}

private void sendCommand(int messageType, List payload = [], boolean responseRequired = false) {
    def buffer = []
    def packet = parent.makePacket(buffer, [0, 0, 0, 0, 0, 0] as byte[], messageType, false, responseRequired, payload)
    sendPacket(buffer, true)
}

def requestInfo() {
    sendCommand(messageTypes().DEVICE.GET_LABEL)
    sendCommand(messageTypes().DEVICE.GET_GROUP)
    sendCommand(messageTypes().DEVICE.GET_LOCATION)
    sendCommand(messageTypes().LIGHT.GET_STATE)
}

def parse(String description) {
    def header = parent.parseHeader(parseDeviceParameters(description))
    def type = lookupMessageType(header.type)
    log.debug("COLOR: Got message type = ${type}")
    switch (header.type) {
        case messageTypes().DEVICE.STATE_VERSION:
            log.warn("STATE_VERSION type ignored")
            break
        case messageTypes().DEVICE.STATE_LABEL:
            def data = parseBytes(makeDescriptor('name:32s'), header.remainder)
            String label = data.name
            device.setLabel(label.trim())
            break
        case messageTypes().DEVICE.STATE_GROUP:
            def data = parseBytes(makeDescriptor('group:16a,name:32s,updated_at:8l'), header.remainder)
            String group = data.name
            log.debug("Group: ${group}")
            return createEvent(name: 'Group', value: group)
        case messageTypes().DEVICE.STATE_LOCATION:
            def data = parseBytes(makeDescriptor('location:16a,name:32s,updated_at:8l'), header.remainder)
            String location = data.name
            log.debug("Location: ${location}")
            break
        case messageTypes().DEVICE.STATE_WIFI_INFO:
            break
        case messageTypes().DEVICE.STATE_INFO:
            break
        case messageTypes().LIGHT.STATE:
            def data = parseBytes(makeDescriptor("${hsbkDescriptor},reserved1:2l,power:2l,label:32s,reserved2:8a"), header.remainder)
            log.debug("LIGHT state: ${data}")
            break
    }
}

private static Map parseDeviceParameters(String description) {
    Map deviceParams = new HashMap()
    description.findAll(~/(\w+):(\w+)/) {
        deviceParams.putAt(it[1], it[2])
    }
    return deviceParams
}

String lookupMessageType(messageType) {
    parent.lookupMessageType(messageType)
}
private List<Map> makeDescriptor(String pattern) {
    parent.makeDescriptor(pattern)
}

private Map parseHeader(Map deviceParams) {
    parent.parseHeader(deviceParams)
}

private Map parseBytes(List<Map> descriptor, List<Long> parseable) {
    parent.parseBytes(descriptor, parseable)
}

private def messageTypes() {
    parent.messageTypes()
}

private def myIp() {
    device.getDeviceNetworkId()
}

private def sendPacket(List buffer, boolean wantLog = false) {
    String ipAddress = myIp()
    def rawBytes = parent.asByteArray(buffer)
//    log.debug "raw bytes: ${rawBytes}"
    String stringBytes = hubitat.helper.HexUtils.byteArrayToHexString(rawBytes)
    if (wantLog){
        log.debug "sending bytes: ${stringBytes} to ${ipAddress}"
    }
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