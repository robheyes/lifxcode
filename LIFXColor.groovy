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
    definition(name: "LIFX Color", namespace: "robheyes", author: "Robert Alan Heyes", importUrl: 'https://raw.githubusercontent.com/robheyes/lifxcode/master/LIFXColor.groovy') {
        capability "Bulb"
        capability "ColorTemperature"
        capability "Polling"
        capability "Switch"
        capability "Switch Level"
        capability "Initialize"
        capability "ColorControl"
        capability "ColorMode"

        attribute "label", "string"
        attribute "group", "string"
        attribute "location", "string"
        attribute "lightStatus", "string"
        attribute "wifiStatus", "map"

        command "setState", ["MAP"]
    }

    preferences {
        input "useActivityLogFlag", "bool", title: "Enable activity logging", required: false
        input "useDebugActivityLogFlag", "bool", title: "Enable debug logging", required: false
        input "defaultTransition", "decimal", title: "Color map level transition time", description: "Set color time (seconds)", required: true, defaultValue: 0.0
    }
}

@SuppressWarnings("unused")
def installed() {
    initialize()
}

@SuppressWarnings("unused")
def updated() {
    state.transitionTime = defaultTransition
    state.useActivityLog = useActivityLogFlag
    statue.useActivityLogDebug = useDebugActivityLogFlag
    initialize()
}

def initialize() {
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
    parent.lifxQuery (device, 'LIGHT.GET_STATE') { List buffer -> sendPacket buffer }
}

def on() {
    sendActions parent.deviceOnOff('on', getUseActivityLog(), state.transitionTime ?: 0)
}

def off() {
    sendActions parent.deviceOnOff('off', getUseActivityLog(), state.transitionTime ?: 0)
}

@SuppressWarnings("unused")
def setColor(Map colorMap) {
    sendActions parent.deviceSetColor(device, colorMap, getUseActivityLogDebug(), state.transitionTime ?: 0)
}

@SuppressWarnings("unused")
def setHue(hue) {
    sendActions parent.deviceSetHue(device, hue, getUseActivityLog(), state.transitionTime ?: 0)
}

@SuppressWarnings("unused")
def setSaturation(saturation) {
    sendActions parent.deviceSetSaturation(device, saturation, getUseActivityLog(), state.transitionTime ?: 0)
}

@SuppressWarnings("unused")
def setColorTemperature(temperature) {
    sendActions parent.deviceSetColorTemperature(device, temperature, getUseActivityLog(), state.transitionTime ?: 0)
}

@SuppressWarnings("unused")
def setLevel(level, duration = 0) {
    sendActions parent.deviceSetLevel(device, level as Number, getUseActivityLog(), duration)
}

@SuppressWarnings("unused")
def setState(value) {
    sendActions parent.deviceSetState(device, stringToMap(value), getUseActivityLog(), state.transitionTime ?: 0)
}

private void sendActions(Map<String, List> actions) {
    actions.commands?.eachWithIndex { item, index -> parent.lifxCommand(device, item.cmd, item.payload, index as Byte) { List buffer -> sendPacket buffer } }
    actions.events?.each { sendEvent it }
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
