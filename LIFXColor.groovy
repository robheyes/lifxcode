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

def initialize() {
    requestInfo()
}

def refresh() {

}

def poll() {

}

def on() {
    def buffer = []
    def payload = []
    parent.add(payload, 65535 as short)
    def packet = parent.makePacket(buffer, [0,0,0,0,0,0] as byte[], parent.messageTypes().DEVICE.SET_POWER, false, false, payload)
    parent.sendPacket(buffer, device.getDeviceNetworkId(), true)
}

def off() {
    def buffer = []
    def payload = []
    parent.add(payload, 0 as short)
    def packet = parent.makePacket(buffer, [0,0,0,0,0,0] as byte[], parent.messageTypes().DEVICE.SET_POWER, false, false, payload)
    parent.sendPacket(buffer, device.getDeviceNetworkId(), true)
}

def setColor(Map colorMap) {

}

def setHue(number) {

}

def setSaturation(number) {

}

def setColorTemperature(temperature) {

}

def requestInfo() {
    def labelPacket = parent.makeGetLabelPacket()
    log.debug("Sending packet ${labelPacket}")
    sendPacket(labelPacket.buffer, device.getDeviceNetworkId())
}

def parse(String description) {
    log.debug("COLOR: description = ${description}")
    Map deviceParams = new HashMap()
    description.findAll(~/(\w+):(\w+)/) {
//        log.debug("Storing ${it[2]} at ${it[1]}")
        deviceParams.putAt(it[1], it[2])
    }
    log.debug("COLOR: Params ${deviceParams}")
//    theClass = deviceParams.ip.getClass()
//    log.debug("ip ${deviceParams.ip} of ${theClass}")
    def parsed = parent.parseHeader(deviceParams)
    log.debug("COLOR: Got message of type ${parsed.type}")
    switch (parsed.type) {
        case messageTypes().DEVICE.STATE_VERSION:
            log.warn("STATE_VERSION type ignored")
            break
        case messageTypes().DEVICE.STATE_LABEL:
            log.debug('COLOR: Got STATE_LABEL')
            def descriptor = parent.makeDescriptor('label:32s')
            log.debug("COLOR: Descriptor ${descriptor}")
            log.debug("COLOR: parsed ${parsed}")
            def data = parent.parseBytes(descriptor, parsed.remainder)
            String label = data.label
            device.setLabel(label.trim())
            break
        case messageTypes().DEVICE.STATE_GROUP:
            break
        case messageTypes().DEVICE.STATE_LOCATION:
            break
        case messageTypes().DEVICE.STATE_WIFI_INFO:
            break
        case messageTypes().DEVICE.STATE_INFO:
            break
    }
}

def messageTypes() {
    parent.messageTypes()
}

def sendPacket(List buffer, String ipAddress, boolean wantLog = false) {
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