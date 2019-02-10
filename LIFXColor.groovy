/**
 *
 *  Copyright 2019 Robert Heyes. All Rights Reserved
 *
 *  This software is free for Private Use. You may use and modify the software without distributing it.
 *  If you make a fork, and add new code, then you should create a pull request to add value, there is no
 *  guarantee that your pull request will be merged.
 *
 *  You may not grant a sublicense to modify and distribute this software to third parties without permission
 *  from the copyright holder
 *  Software is provided without warranty and your use of it is at your own risk.
 *
 */

metadata {
    definition(name: "LIFX Color", namespace: "robheyes", author: "Robert Alan Heyes") {
        capability "Bulb"
        capability "Color Temperature"
        capability "HealthCheck"
        capability "Polling"
        capability "Switch"
        capability "Switch Level"
        capability "Initialize"
        capability "Color Control"
        // capability "LightEffect"
        attribute "Label", "string"
        attribute "Group", "string"
        attribute "Location", "string"
    }

    preferences {
        input "logEnable", "bool", title: "Enable debug logging", required: false
        input "defaultTransition", "decimal", title: "Color map level transition time", description: "Set color time (seconds)", required: true, defaultValue: 0.0
    }
}

def installed() {
    initialize()
}

def updated() {
    initialize()
}

def initialize() {
    state.colorTransitionTime = defaultTransition
    requestInfo()
    runEvery1Minute poll
}

def refresh() {

}

def poll() {
    lifxQuery 'LIGHT.GET_STATE'
}

def on() {
    lifxCommand 'DEVICE.SET_POWER', [level: 65535]
    sendEvent name: "switch", value: "on", displayed: getUseActivityLog(), data: [syncing: "false"]
}

def off() {
    lifxCommand 'DEVICE.SET_POWER', [level: 0]
    sendEvent name: "switch", value: "off", displayed: getUseActivityLog(), data: [syncing: "false"]
}

def setColor(Map colorMap) {
    Map hsbkMap = parent.getCurrentHSBK device
    hsbkMap << getScaledColorMap(colorMap)
    hsbkMap.duration = 1000 * (state.colorTransitionTime ?: 0)
    lifxCommand 'LIGHT.SET_COLOR', hsbkMap
    sendColorMapEvent hsbkMap
}

def setHue(hue) {
    Map hsbkMap = parent.getCurrentHSBK device
    hsbkMap.hue = scaleUp100 hue
    hsbkMap.duration = 1000 * (state.colorTransitionTime ?: 0)
    lifxCommand 'LIGHT.SET_COLOR', hsbkMap
    sendEvent name: 'hue', value: hue, displayed: getUseActivityLog()
}

def setSaturation(saturation) {
    Map hsbkMap = parent.getCurrentHSBK(device)
    hsbkMap.saturation = scaleUp100 saturation
    hsbkMap.duration = 1000 * (state.colorTransitionTime ?: 0)
    lifxCommand 'LIGHT.SET_COLOR', hsbkMap
    sendEvent name: 'saturation', value: saturation, displayed: getUseActivityLog()
}

def setLevel(level, duration = 0) {
//    log.debug "Begin setting light's level to ${level} over ${duration} seconds."
    if (level > 100) {
        level = 100
    } else if ((level <= 0 || null == level) && 0 == duration) {
        return off()
    }
    Map hsbkMap = parent.getCurrentHSBK(device)
//    logDebug("BK Map: $hsbkMap")
    if (hsbkMap.saturation == 0) {
        hsbkMap.level = scaleUp100 level
        hsbkMap.hue = 0
        hsbkMap.saturation = 0
        hsbkMap.duration = duration * 1000
    } else {
        hsbkMap = [
                level     : scaleUp100(level),
                duration  : duration * 1000,
                hue       : scaleUp100(device.currentHue),
                saturation: scaleUp100(device.currentSaturation),
                kelvin    : device.currentColorTemperature
        ]
    }
//    logDebug "Map to be sent: $hsbkMap"
    lifxCommand 'LIGHT.SET_COLOR', hsbkMap
    sendEvent name: 'level', value: level, displayed: getUseActivityLog()
}

private Long scaleUp100(Number value) {
    parent.scaleUp value, 100
}

private Float scaleDown100(Number value) {
    parent.scaleDown value, 100
}

def setColorTemperature(temperature) {
    Map hsbkMap = parent.getCurrentHSBK device
    hsbkMap.kelvin = temperature
    hsbkMap.saturation = 0
    lifxCommand 'LIGHT.SET_COLOR', hsbkMap
    sendColorMapEvent hsbkMap
}

private void sendColorMapEvent(Map hsbkMap) {
    sendEvent name: "hue", value: scaleDown100(hsbkMap.hue), displayed: getUseActivityLogDebug()
    sendEvent name: "saturation", value: scaleDown100(hsbkMap.saturation), displayed: getUseActivityLogDebug()
    sendEvent name: "level", value: scaleDown100(hsbkMap.level), displayed: getUseActivityLogDebug()
    sendEvent name: "colorTemperature", value: hsbkMap.kelvin as Integer, displayed: getUseActivityLogDebug()
    sendEvent name: "switch", value: (hsbkMap.level as Integer == 0 ? "off" : "on"), displayed: getUseActivityLog(), data: [syncing: "false"]
}

private Map<String, Integer> getScaledColorMap(Map colorMap) {
    [
            hue       : scaleUp100(colorMap.hue) as Integer,
            saturation: scaleUp100(colorMap.saturation) as Integer,
            level     : scaleUp100(colorMap.level) as Integer,
    ]
}

private void lifxQuery(String deviceAndType) {
    sendCommand deviceAndType, [:], true
}

private void lifxCommand(String deviceAndType, Map payload) {
    sendCommand deviceAndType, payload, false, true
}

private void sendCommand(String deviceAndType, Map payload = [:], boolean responseRequired = true, boolean ackRequired = false) {
    resendUnacknowledgedCommand()
    def parts = deviceAndType.split(/\./)
    def buffer = []
    byte sequence = parent.makePacket buffer, parts[0], parts[1], payload, responseRequired, ackRequired
    if (ackRequired) {
        parent.expectAckFor device, sequence, buffer
    }
    sendPacket buffer
}

private void resendUnacknowledgedCommand() {
    def expectedSequence = parent.ackWasExpected device
    if (expectedSequence) {
        List resendBuffer = parent.getBufferToResend device, expectedSequence
        logWarn "resend buffer is $resendBuffer"
        parent.clearExpectedAckFor device, expectedSequence
        sendPacket resendBuffer
    }
}

def requestInfo() {
    lifxQuery('LIGHT.GET_STATE')
}

def parse(String description) {
    Map header = parent.parseHeaderFromDescription description
    switch (header.type) {
        case messageTypes().DEVICE.STATE_VERSION.type:
            log.warn("STATE_VERSION type ignored")
            break
        case messageTypes().DEVICE.STATE_LABEL.type:
            def data = parent.parsePayload 'DEVICE.STATE_LABEL', header
            String label = data.label
            device.setLabel(label.trim())
            break
        case messageTypes().DEVICE.STATE_GROUP.type:
            def data = parent.parsePayload 'DEVICE.STATE_GROUP', header
            String group = data.label
            return createEvent(name: 'Group', value: group)
        case messageTypes().DEVICE.STATE_LOCATION.type:
            def data = parent.parsePayload 'DEVICE.STATE_LOCATION', header
            String location = data.label
            return createEvent(name: 'Location', value: location)
        case messageTypes().DEVICE.STATE_WIFI_INFO.type:
            def data = parent.parsePayload 'DEVICE.STATE_WIFI_INFO', header
            break
        case messageTypes().DEVICE.STATE_INFO.type:
            def data = parent.parsePayload 'DEVICE.STATE_INFO', header
            break
        case messageTypes().LIGHT.STATE.type:
            def data = parent.parsePayload 'LIGHT.STATE', header
            device.setLabel data.label.trim()
            return [
                    createEvent(name: "hue", value: parent.scaleDown(data.hue, 100), displayed: getUseActivityLogDebug()),
                    createEvent(name: "saturation", value: parent.scaleDown(data.saturation, 100), displayed: getUseActivityLogDebug()),
                    createEvent(name: "level", value: parent.scaleDown(data.level, 100), displayed: getUseActivityLogDebug()),
                    createEvent(name: "colorTemperature", value: data.kelvin as Integer, displayed: getUseActivityLogDebug()),
                    createEvent(name: "switch", value: (data.level as Integer == 0 ? "off" : "on"), displayed: getUseActivityLog(), data: [syncing: "false"]),
            ]
        case messageTypes().DEVICE.STATE_POWER.type:
            Map data = parent.parsePayload 'DEVICE.STATE_POWER', header
            return [
                    createEvent(name: "switch", value: (data.level as Integer == 0 ? "off" : "on"), displayed: getUseActivityLog(), data: [syncing: "false"]),
                    createEvent(name: "level", value: (data.level as Integer == 0 ? 0 : 100), displayed: getUseActivityLog(), data: [syncing: "false"])
            ]
        case messageTypes().DEVICE.ACKNOWLEDGEMENT.type:
            Byte sequence = header.sequence
            parent.clearExpectedAckFor device, sequence
            break
        default:
            logDebug "Unhandled response for ${header.type}"
    }
}

private Map<String, Map<String, Map>> messageTypes() {
    parent.messageTypes()
}

private String myIp() {
    device.getDeviceNetworkId()
}

private void sendPacket(List buffer) {
    String stringBytes = hubitat.helper.HexUtils.byteArrayToHexString parent.asByteArray(buffer)
    sendHubCommand(
            new hubitat.device.HubAction(
                    stringBytes,
                    hubitat.device.Protocol.LAN,
                    [
                            type              : hubitat.device.HubAction.Type.LAN_TYPE_UDPCLIENT,
                            destinationAddress: myIp() + ":56700",
                            encoding          : hubitat.device.HubAction.Encoding.HEX_STRING
                    ]
            )
    )
}

def getUseActivityLog() {
    if (state.useActivityLog == null) {
        state.useActivityLog = true
    }
    return state.useActivityLog
}

def setUseActivityLog(value) {
    state.useActivityLog = value
}

def getUseActivityLogDebug() {
    if (state.useActivityLogDebug == null) {
        state.useActivityLogDebug = false
    }
    return state.useActivityLogDebug
}

def setUseActivityLogDebug(value) {
    state.useActivityLogDebug = value
}

void logDebug(msg) {
    log.debug msg
}

void logInfo(msg) {
    log.info msg
}

void logWarn(String msg) {
    log.warn msg
}
