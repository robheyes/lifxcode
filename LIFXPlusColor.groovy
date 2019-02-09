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
    definition(name: "LIFXPlus Color", namespace: "robheyes", author: "Robert Alan Heyes") {
        capability "Bulb"
        capability "Color Temperature"
        capability "HealthCheck"
        capability "Polling"
        capability "Switch"
        capability "Switch Level"
        capability "Initialize"
        capability "Color Control"
        // capability "LightEffect"

        attribute "Group", "string"
        attribute "Label", "string"
        attribute "Location", "string"
        attribute "IrLevel", 'number'

        // need a command to set the ir level
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
    runEvery1Minute(poll)
}

def refresh() {

}

def poll() {
    sendCommand('LIGHT.GET_STATE', [:], false)
}

def on() {
    sendCommand('DEVICE.SET_POWER', [level: 65535])
    sendEvent(name: "switch", value: "on", displayed: getUseActivityLog(), data: [syncing: "false"])
}

def off() {
    sendCommand('DEVICE.SET_POWER', [level: 0])
    sendEvent(name: "switch", value: "off", displayed: getUseActivityLog(), data: [syncing: "false"])
}

def setColor(Map colorMap) {
// for now assume that we have the current state
    Map hsbkMap = parent.getCurrentHSBK(device)
    hsbkMap << getScaledColorMap(colorMap)
    hsbkMap.duration = 1000 * (state.colorTransitionTime ?: 0)
    sendCommand'LIGHT.SET_COLOR', hsbkMap
    sendColorMapEvent(hsbkMap)
}

def setHue(number) {
    Map hsbkMap = parent.getCurrentHSBK(device)
    hsbkMap.hue = parent.scaleUp(number, 100)
    sendCommand'LIGHT.SET_COLOR', hsbkMap
    sendColorMapEvent(hsbkMap)
}

def setSaturation(number) {
    Map hsbkMap = parent.getCurrentHSBK(device)
    hsbkMap.saturation = parent.scaleUp(number, 100)
    sendCommand'LIGHT.SET_COLOR', hsbkMap
    sendColorMapEvent(hsbkMap)
}

def setLevel(level, duration = 0) {
    log.debug("Begin setting light's level to ${level} over ${duration} seconds.")
    if (level > 100) {
        level = 100
    } else if ((level <= 0 || level == null) && duration == 0) {
        return off()
    }
//    Map hsbkMap = parent.getCurrentBK(device)
//    logDebug("BK Map: $hsbkMap")
    hsbkMap = [
            level     : parent.scaleUp(level as Long, 100),
            duration  : duration * 1000,
            hue       : 0,
            saturation: 0,
            kelvin    : device.currentColorTemperature
    ]
    logDebug "Map to be sent: $hsbkMap"
    sendCommand 'LIGHT.SET_COLOR', hsbkMap
    sendEvent(name: 'level', value: level, displayed: getUseActivityLog())
    pauseExecution 1000
    poll()
}

def setColorTemperature(temperature) {
    Map hsbkMap = parent.getCurrentHSBK(device)
    hsbkMap.kelvin = temperature
    hsbkMap.saturation = 0
    sendCommand'LIGHT.SET_COLOR', hsbkMap
    sendColorMapEvent(hsbkMap)

}

private void sendColorMapEvent(Map hsbkMap) {
    sendEvent(name: "hue", value: parent.scaleDown(hsbkMap.hue, 100), displayed: getUseActivityLogDebug())
    sendEvent(name: "saturation", value: parent.scaleDown(hsbkMap.saturation, 100), displayed: getUseActivityLogDebug())
    sendEvent(name: "level", value: parent.scaleDown(hsbkMap.level, 100), displayed: getUseActivityLogDebug())
    sendEvent(name: "colorTemperature", value: hsbkMap.kelvin as Integer, displayed: getUseActivityLogDebug())
    sendEvent(name: "switch", value: (hsbkMap.level as Integer == 0 ? "off" : "on"), displayed: getUseActivityLog(), data: [syncing: "false"])
}

private Map<String, Integer> getScaledColorMap(Map colorMap) {
    [
            hue       : parent.scaleUp(colorMap.hue, 100) as Integer,
            saturation: parent.scaleUp(colorMap.saturation, 100) as Integer,
            level     : parent.scaleUp(colorMap.level, 100) as Integer,
    ]
}

private void sendCommand(int messageType, List payload = [], boolean responseRequired = false) {
    def buffer = []
    parent.makePacket(buffer, messageType, responseRequired, payload)
    sendPacket(buffer)
}

private void sendCommand(String device, String type, Map payload = [:], boolean responseRequired = true) {
    def buffer = []
    parent.makePacket(buffer, device, type, payload, responseRequired)
    sendPacket(buffer)
}

private void sendCommand(String deviceAndType, Map payload = [:], boolean responseRequired = false) {
    //logDebug("Sending command $deviceAndType")
    def parts = deviceAndType.split(/\./)
    sendCommand(parts[0], parts[1], payload, responseRequired)
}

def requestInfo() {
    sendCommand(messageTypes().LIGHT.GET_STATE.type as Integer)
}

def parse(String description) {
    Map header = parent.parseHeaderFromDescription(description)
    switch (header.type) {
        case messageTypes().DEVICE.STATE_VERSION.type:
            log.warn("STATE_VERSION type ignored")
            break
        case messageTypes().DEVICE.STATE_LABEL.type:
            def data = parent.parsePayload('DEVICE.STATE_LABEL', header)
            String label = data.label
            device.setLabel(label.trim())
            break
        case messageTypes().DEVICE.STATE_GROUP.type:
            def data = parent.parsePayload('DEVICE.STATE_GROUP', header)
            String group = data.label
            return createEvent(name: 'Group', value: group)
        case messageTypes().DEVICE.STATE_LOCATION.type:
            def data = parent.parsePayload('DEVICE.STATE_LOCATION', header)
            String location = data.label
            return createEvent(name: 'Location', value: location)
        case messageTypes().DEVICE.STATE_WIFI_INFO.type:
            def data = parent.parsePayload('DEVICE.STATE_WIFI_INFO', header)
            break
        case messageTypes().DEVICE.STATE_INFO.type:
            def data = parent.parsePayload('DEVICE.STATE_INFO', header)
            break
        case messageTypes().LIGHT.STATE.type:
            def data = parent.parsePayload('LIGHT.STATE', header)
            device.setLabel(data.label.trim())
            return [
                    createEvent(name: "hue", value: parent.scaleDown(data.hue, 100), displayed: getUseActivityLogDebug()),
                    createEvent(name: "saturation", value: parent.scaleDown(data.saturation, 100), displayed: getUseActivityLogDebug()),
                    createEvent(name: "level", value: parent.scaleDown(data.level, 100), displayed: getUseActivityLogDebug()),
                    createEvent(name: "colorTemperature", value: data.kelvin as Integer, displayed: getUseActivityLogDebug()),
                    createEvent(name: "switch", value: (data.level as Integer == 0 ? "off" : "on"), displayed: getUseActivityLog(), data: [syncing: "false"]),
            ]
        case messageTypes().DEVICE.STATE_POWER.type:
            Map data = parent.parsePayload('DEVICE.STATE_POWER', header)
            return [
                    createEvent(name: "switch", value: (data.level as Integer == 0 ? "off" : "on"), displayed: getUseActivityLog(), data: [syncing: "false"]),
                    createEvent(name: "level", value: (data.level as Integer == 0 ? 0 : 100), displayed: getUseActivityLog(), data: [syncing: "false"])
            ]
        default:
            logDebug "Unhandled response for ${header.type}"
    }
}


private Map<String, Map<String, Map>> messageTypes() {
    parent.messageTypes()
}

private def myIp() {
    device.getDeviceNetworkId()
}

private def sendPacket(List buffer) {
    String ipAddress = myIp()
    def rawBytes = parent.asByteArray(buffer)
    String stringBytes = hubitat.helper.HexUtils.byteArrayToHexString(rawBytes)
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
    log.debug(msg)
}

void logInfo(msg) {
    log.info(msg)
}

void logWarn(String msg) {
    log.warn(msg)
}
