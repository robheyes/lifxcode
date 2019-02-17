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
    definition(name: "LIFX White", namespace: "robheyes", author: "Robert Alan Heyes") {
        capability "Bulb"
        capability "ColorTemperature"
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
    sendActions parent.deviceOnOff('on', getUseActivityLog())
}

def off() {
    sendActions parent.deviceOnOff('off', getUseActivityLog())
}

// DND Yet!!!

//def setLevel(level, duration = 0) {
////    log.debug("Begin setting light's level to ${level} over ${duration} seconds.")
//    if (level > 100) {
//        level = 100
//    } else if ((level <= 0 || level == null) && duration == 0) {
//        return off()
//    }
//    Map hsbkMap = parent.getCurrentBK device
//    hsbkMap.level = parent.scaleUp(level, 100)
//    hsbkMap.duration = duration * 1000
//    lifxCommand'LIGHT.SET_COLOR', hsbkMap
//    sendLevelAndSwitchEvents(hsbkMap)
//}

//private void sendLevelAndSwitchEvents(Map hsbkMap) {
//    sendEvent(name: "level", value: parent.scaleDown(hsbkMap.level, 100), displayed: getUseActivityLogDebug())
//    sendEvent(name: "switch", value: (hsbkMap.level as Integer == 0 ? "off" : "on"), displayed: getUseActivityLog(), data: [syncing: "false"])
//}


def setLevel(level, duration = 0) {
    sendActions parent.deviceSetLevel(device, level as Number, getUseActivityLog(), duration)
}


def setColorTemperature(temperature) {
    sendActions parent.deviceSetColorTemperature(device, temperature, getUseActivityLog(), state.colorTransitionTime ?: 0)
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
    def rawBytes = parent.asByteArray(buffer)
    String stringBytes = hubitat.helper.HexUtils.byteArrayToHexString(rawBytes)
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
