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
    definition(name: "LIFX Day and Dusk", namespace: "robheyes", author: "Robert Alan Heyes") {
        capability "Bulb"
        capability "ColorTemperature"
        capability "ColorMode"
        capability "HealthCheck"
        capability "Polling"
        capability "Switch"
        capability "SwitchLevel"
        capability "Initialize"

        attribute "Label", "string"
        attribute "Group", "string"
        attribute "Location", "string"
    }

    preferences {
        input "logEnable", "bool", title: "Enable debug logging", required: false
        input "defaultTransition", "decimal", title: "Level transition time", description: "Set color time (seconds)", required: true, defaultValue: 0.0
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
    colorMode = 'CT'
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


def setLevel(level, duration = 0) {
    sendActions parent.deviceSetLevel(device, level as Number, getUseActivityLog(), duration)
}

// DND YET

//def setLevel(level, duration = 0) {
//    log.debug("Begin setting light's level to ${level} over ${duration} seconds.")
//    if (level > 100) {
//        level = 100
//    } else if ((level <= 0 || level == null) && duration == 0) {
//        return off()
//    }
////    Map hsbkMap = parent.getCurrentBK(device)
////    logDebug("BK Map: $hsbkMap")
//    hsbkMap = [
//            level     : parent.scaleUp(level as Long, 100),
//            duration  : duration * 1000,
//            hue       : 0,
//            saturation: 0,
//            kelvin    : device.currentColorTemperature
//    ]
//    logDebug "Map to be sent: $hsbkMap"
//    lifxCommand 'LIGHT.SET_COLOR', hsbkMap
//    sendEvent name: 'level', value: level, displayed: getUseActivityLog()
//}

def setColorTemperature(temperature) {
    sendActions parent.deviceSetColorTemperature(device, temperature, getUseActivityLog(), state.colorTransitionTime ?: 0)
}

private void lifxQuery(String deviceAndType) {
    sendCommand deviceAndType, [:], true
}

private void lifxCommand(String deviceAndType, Map payload) {
    sendCommand deviceAndType, payload, false, true
}

private void sendActions(Map<String, List> actions) {
    actions.commands?.each { lifxCommand it.cmd, it.payload }
    actions.events?.each { sendEvent it }
}

private void sendCommand(String deviceAndType, Map payload = [:], boolean responseRequired = true, boolean ackRequired = false) {
    resendUnacknowledgedCommand()
    def parts = deviceAndType.split(/\./)
    def buffer = []
    byte sequence = parent.makePacket buffer, parts[0], parts[1], payload, responseRequired, ackRequired
    if (ackRequired) {
//        logDebug "Sending packet with sequence $sequence"
        parent.expectAckFor device, sequence, buffer
    }
    sendPacket buffer
}

private void resendUnacknowledgedCommand() {
    def expectedSequence = parent.ackWasExpected(device)
    if (expectedSequence) {
        List resendBuffer = parent.getBufferToResend device, expectedSequence
        logWarn "resend buffer is $resendBuffer"
        parent.clearExpectedAckFor device, expectedSequence
        sendPacket resendBuffer
    }
}

def requestInfo() {
    lifxQuery 'LIGHT.GET_STATE'
}

def parse(String description) {
    List<Map> events = parent.parseForDevice(device, description, getUseActivityLog())
    events.collect { createEvent(it) }
}

private def myIp() {
    device.getDeviceNetworkId()
}

private def sendPacket(List buffer) {
    String stringBytes = hubitat.helper.HexUtils.byteArrayToHexString parent.asByteArray(buffer)
    sendHubCommand(
            new hubitat.device.HubAction(
                    stringBytes,
                    hubitat.device.Protocol.LAN,
                    [
                            type              : hubitat.device.HubAction.Type.LAN_TYPE_UDPCLIENT,
                            destinationAddress: (myIp() as String) + ":56700",
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
