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
    capability "Bulb"
//	capability "LightEffect"
    capability "Color Control"
    capability "Color Temperature"
    capability "HealthCheck"
    capability "Polling"
    capability "Switch"
    capability "Switch Level"
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
    sendCommand(messageTypes().LIGHT.GET_STATE.type)
}

def on() {
    def payload = []
    parent.add(payload, 65535 as short)
    sendCommand(parent.messageTypes().DEVICE.SET_POWER.type, payload)
}

def off() {
    def payload = []
    parent.add(payload, 0 as short)
    sendCommand(parent.messageTypes().DEVICE.SET_POWER.type, payload)
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
    sendCommand(messageTypes().LIGHT.GET_STATE.type)
    sendCommand(messageTypes().DEVICE.GET_GROUP.type)
    sendCommand(messageTypes().DEVICE.GET_LOCATION.type)
}

def parse(String description) {
    def header = parent.parseHeader(parseDeviceParameters(description))
//    log.debug("COLOR: Header = ${header}")
    //    log.debug("COLOR: Got message type = ${type} (${header.type})")
//    log.debug("COLOR: Got message type = ${type.name} (${header.type}) descriptor: ${type.descriptor}")
//    def data = parseBytes(getDescriptorString(header), getRemainder(header))
    switch (header.type) {
        case messageTypes().DEVICE.STATE_VERSION.type:
            log.warn("STATE_VERSION type ignored")
            break
        case messageTypes().DEVICE.STATE_LABEL.type:
            def data = parseBytes(lookupPayload('DEVICE', 'STATE_LABEL'), getRemainder(header))
            String label = data.label
            device.setLabel(label.trim())
            break
        case messageTypes().DEVICE.STATE_GROUP.type:
            def data = parseBytes(lookupPayload('DEVICE', 'STATE_GROUP'), getRemainder(header))
            //def data = parseBytes(makeDescriptor('group:16a,name:32s,updated_at:8l'), getRemainder(header))
            String group = data.label
//            log.debug("Group: ${group}")
            return createEvent(name: 'Group', value: group)
        case messageTypes().DEVICE.STATE_LOCATION.type:
            def data = parseBytes(lookupPayload('DEVICE', 'STATE_LOCATION'), getRemainder(header))
            String location = data.label
//            log.debug("Location: ${location}")
            return createEvent(name: 'Location', value: location)
        case messageTypes().DEVICE.STATE_WIFI_INFO.type:
            def data = parseBytes(lookupPayload('DEVICE', 'STATE_WIFI_INFO'), getRemainder(header))
            break
        case messageTypes().DEVICE.STATE_INFO.type:
            def data = parseBytes(lookupPayload('DEVICE', 'STATE_INFO'), getRemainder(header))
            break
        case messageTypes().LIGHT.STATE.type:
            def data = parseBytes(lookupPayload('LIGHT', 'STATE'), getRemainder(header))
            log.debug("LIGHT state: ${data}")
            device.setLabel(data.label.trim())
            return [
                    createEvent(name: "hue", value: scaleDown(data.hue, 100), displayed: getUseActivityLogDebug()),
                    createEvent(name: "saturation", value: scaleDown(data.saturation, 100), displayed: getUseActivityLogDebug()),
                    createEvent(name: "level", value: scaleDown(data.brightness, 100), displayed: getUseActivityLogDebug()),
            ]
            break
    }
}

private String lookupPayload(String device, String type) {
    parent.lookupDescriptorForDeviceAndType(device, type)
}
//
//private String getDescriptorString(header) {
//    responseDescriptor().get(header.type as Integer, 'none')
//}

private static Integer scaleDown(value, maxValue) {
    (value * maxValue) / 65535
}

private static Integer scaleUp(value, maxValue) {
    (value * 65535) / maxValue
}
//
//private Map<Integer, String> responseDescriptor() {
//    parent.responseDescriptor()
//}

private static List<Long> getRemainder(header) {
    header.remainder as List<Long>
}

private static Map parseDeviceParameters(String description) {
    Map deviceParams = new HashMap()
    description.findAll(~/(\w+):(\w+)/) {
        deviceParams.putAt(it[1], it[2])
    }
    return deviceParams
}
//
//Map lookupMessageType(messageType) {
//    parent.lookupMessageType(messageType)
//}

//private List<Map> makeDescriptor(String pattern) {
//    parent.getDescriptor(pattern)
//}

private Map parseHeader(Map deviceParams) {
    parent.parseHeader(deviceParams)
}

private Map parseBytes(List<Map> descriptor, List<Long> parseable) {
    parent.parseBytes(descriptor, parseable)
}

private Map parseBytes(String descriptor, List<Long> parseable) {
    parent.parseBytes(descriptor, parseable)
}

private Map<String, Map<String, Map>> messageTypes() {
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
    if (wantLog) {
//        log.debug "sending bytes: ${stringBytes} to ${ipAddress}"
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

def getUseActivityLog() {
    if(state.useActivityLog == null) {
        state.useActivityLog = true
    }
    return state.useActivityLog
}

def setUseActivityLog(value) {
    state.useActivityLog = value
}

def getUseActivityLogDebug() {
    if(state.useActivityLogDebug == null) {
        state.useActivityLogDebug = false
    }
    return state.useActivityLogDebug
}

def setUseActivityLogDebug(value) {
    state.useActivityLogDebug = value
}
