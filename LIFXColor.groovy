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

        attribute "label", "string"
        attribute "group", "string"
        attribute "location", "string"
        attribute "colorName", "string"
        attribute "lightStatus", "string"
        attribute "wifiStatus", "map"

        command "setState", ["MAP"]
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
//    logDebug('Initialised')
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
    sendActions parent.deviceOnOff('on', getUseActivityLog())
}

def off() {
    sendActions parent.deviceOnOff('off', getUseActivityLog())
}

def setColor(Map colorMap) {
    sendActions parent.deviceSetColor(device, colorMap, getUseActivityLogDebug(), state.colorTransitionTime ?: 0)
}

def setHue(hue) {
    sendActions parent.deviceSetHue(device, hue, getUseActivityLog(), state.colorTransitionTime ?: 0)
}

def setSaturation(saturation) {
    sendActions parent.deviceSetSaturation(device, saturation, getUseActivityLog(), state.colorTransitionTime ?: 0)
}

def setColorTemperature(temperature) {
    sendActions parent.deviceSetColorTemperature(device, temperature, getUseActivityLog(), state.colorTransitionTime ?: 0)
}

def setLevel(level, duration = 0) {
    sendActions parent.deviceSetLevel(device, level as Number, getUseActivityLog(), duration)
}

def setState(value) {
    sendActions parent.deviceSetState(device, stringToMap(value), getUseActivityLog(), state.colorTransitionTime ?: 0)
}


private void sendActions(Map<String, List> actions) {
    actions.commands?.eachWithIndex { item, index -> lifxCommand item.cmd, item.payload, index as Byte }
    actions.events?.each { sendEvent it }
}

private void lifxQuery(String deviceAndType) {
    sendCommand deviceAndType, [:], true, false, 0 as Byte
}

private void lifxCommand(String deviceAndType, Map payload, Byte index = 0) {
    sendCommand deviceAndType, payload, false, true, index
}

private void sendCommand(String deviceAndType, Map payload = [:], boolean responseRequired = true, boolean ackRequired = false, Byte index = 0) {
    resendUnacknowledgedCommand()
    def parts = deviceAndType.split(/\./)
    def buffer = []
    byte sequence = parent.makePacket buffer, parts[0], parts[1], payload, responseRequired, ackRequired, index
    if (ackRequired) {
        parent.expectAckFor device, sequence, buffer
    }
    sendPacket buffer
}

private void resendUnacknowledgedCommand() {
    def expectedSequence = parent.ackWasExpected device
    if (expectedSequence) {
        List resendBuffer = parent.getBufferToResend device, expectedSequence
        parent.clearExpectedAckFor device, expectedSequence
        sendPacket resendBuffer
    }
}

def requestInfo() {
    lifxQuery('LIGHT.GET_STATE')
}

def parse(String description) {
    List<Map> events = parent.parseForDevice(device, description, getUseActivityLog())
    events.collect { createEvent(it) }
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
