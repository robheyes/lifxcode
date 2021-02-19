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
    definition(name: "LIFX White Mono", namespace: "robheyes", author: "Robert Alan Heyes", importUrl: 'https://raw.githubusercontent.com/robheyes/lifxcode/master/LIFXWhiteMono.groovy') {
        capability "Bulb"
        capability "Polling"
        capability "Switch"
        capability "Switch Level"
        capability "Initialize"

        attribute "label", "string"
        attribute "group", "string"
        attribute "location", "string"

        command 'setWaveform', [[name: 'Waveform*', type: 'ENUM', constraints:['SAW', 'SINE', 'HALF_SINE', 'TRIANGLE', 'PULSE']], [name: 'Color*', type: 'STRING'], [name: 'Transient', type: 'ENUM', constraints: ['true', 'false']], [name: 'Period', type: 'NUMBER'], [name: 'Cycles', type: 'NUMBER'], [name: 'Skew Ratio', type: 'NUMBER']]
    }

    preferences {
        input "useActivityLogFlag", "bool", title: "Enable activity logging", required: false
        input "useDebugActivityLogFlag", "bool", title: "Enable debug logging", required: false
        input "defaultTransition", "decimal", title: "Transition time", description: "Set transition time (seconds)", required: true, defaultValue: 0.0
        input "changeLevelStep", 'decimal', title: "Change level step size", description: "", required: false, defaultValue: 1
        input "changeLevelEvery", 'number', title: "Change Level every x milliseconds", description: "", required: false, defaultValue: 20

    }
}

@SuppressWarnings("unused")
def installed() {
    initialize()
}

@SuppressWarnings("unused")
def updated() {
    initialize()
}

def initialize() {
    state.transitionTime = defaultTransition
    state.useActivityLog = useActivityLogFlag
    state.useActivityLogDebug = useDebugActivityLogFlag
    state.changeLevelEvery = changeLevelEvery
    state.changeLevelStep = changeLevelStep
    unschedule()
    requestInfo()
    runEvery1Minute poll
}

@SuppressWarnings("unused")
def refresh() {

}

@SuppressWarnings("unused")
def poll() {
    parent.lifxQuery(device, 'LIGHT.GET_STATE') { List buffer -> sendPacket buffer }
}

def requestInfo() {
    parent.lifxQuery(device, 'LIGHT.GET_STATE') { List buffer -> sendPacket buffer }
}

def on() {
    sendActions parent.deviceOnOff('on', getUseActivityLog(), state.transitionTime ?: 0)
}

def off() {
    sendActions parent.deviceOnOff('off', getUseActivityLog(), state.transitionTime ?: 0)
}

@SuppressWarnings("unused")
def setLevel(level, duration = 0) {
    sendActions parent.deviceSetLevel(device, level as Number, getUseActivityLog(), duration)
}

@SuppressWarnings("unused")
def setWaveform(String waveform, String color, String isTransient = 'true', period = 5, cycles = 3.40282346638528860e38, skew = 0.5) {
    sendActions parent.deviceSetWaveform(device, isTransient.toBoolean(), stringToMap(color), period.toInteger(), cycles.toFloat(), skew.toFloat(), waveform)
}

@SuppressWarnings("unused")
def startLevelChange(direction) {
//    logDebug "startLevelChange called with $direction"
    enableLevelChange()
    if (changeLevelStep && changeLevelEvery) {
        doLevelChange(direction == 'up' ? 1 : -1)
    } else {
        logDebug "No parameters"
    }
}

@SuppressWarnings("unused")
def stopLevelChange() {
    sendEvent([name: "cancelLevelChange", value: 'yes', displayed: false])
}

def enableLevelChange() {
    sendEvent([name: "cancelLevelChange", value: 'no', displayed: false])
}

def doLevelChange(direction) {
    def cancelling = device.currentValue('cancelLevelChange') ?: 'no'
    if (cancelling == 'yes') {
        runInMillis 2 * (changeLevelEvery as Integer), "enableLevelChange"
        return;
    }
    def newLevel = device.currentValue('level') + ((direction as Float) * (changeLevelStep as Float))
    def lastStep = false
    if (newLevel < 0) {
        newLevel = 0
        lastStep = true
    } else if (newLevel > 100) {
        newLevel = 100
        lastStep = true
    }
    sendActions parent.deviceSetLevel(device, newLevel, getUseActivityLog(), (changeLevelEvery - 1) / 1000)
    if (!lastStep) {
        runInMillis changeLevelEvery as Integer, "doLevelChange", [data: direction]
    }
}

private void sendActions(Map<String, List> actions) {
    actions.commands?.each { item -> parent.lifxCommand(device, item.cmd, item.payload) { List buffer -> sendPacket buffer, true } }
    actions.events?.each { sendEvent it }
}

def parse(String description) {
    List<Map> events = parent.parseForDevice(device, description, getUseActivityLog())
    events.collect { createEvent(it) }
}

private def myIp() {
    device.getDeviceNetworkId()
}

private void sendPacket(List buffer, boolean noResponseExpected = false) {
    String stringBytes = hubitat.helper.HexUtils.byteArrayToHexString parent.asByteArray(buffer)
    sendHubCommand(
            new hubitat.device.HubAction(
                    stringBytes,
                    hubitat.device.Protocol.LAN,
                    [
                            type              : hubitat.device.HubAction.Type.LAN_TYPE_UDPCLIENT,
                            destinationAddress: myIp() + ":56700",
                            encoding          : hubitat.device.HubAction.Encoding.HEX_STRING,
                            ignoreResponse    : noResponseExpected
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
    log.debug("Setting useActivityLog to ${value ? 'true' : 'false'}")
    state.useActivityLog = value
}

def getUseActivityLogDebug() {
    if (state.useActivityLogDebug == null) {
        state.useActivityLogDebug = false
    }
    return state.useActivityLogDebug
}

def setUseActivityLogDebug(value) {
    log.debug("Setting useActivityLogDebug to ${value ? 'true' : 'false'}")
    state.useActivityLogDebug = value
}

void logDebug(msg) {
    if (getUseActivityLogDebug()) {
        log.debug msg
    }
}

void logInfo(msg) {
    if (getUseActivityLog()) {
        log.info msg
    }
}

void logWarn(String msg) {
    if (getUseActivityLog()) {
        log.warn msg
    }
}
