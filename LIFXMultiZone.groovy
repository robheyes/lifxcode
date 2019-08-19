/**
 *
 * Copyright 2018, 2019 Robert Heyes. All Rights Reserved
 *
 *  This software if free for Private Use. You may use and modify the software without distributing it.
 *  You may not grant a sublicense to modify and distribute this software to third parties.
 *  Software is provided without warranty and your use of it is at your own risk.
 *
 */
metadata {
    definition(name: 'LIFX Multizone', namespace: 'robheyes', author: 'Robert Alan Heyes', importUrl: 'https://raw.githubusercontent.com/robheyes/lifxcode/master/LIFXMultiZone.groovy') {
        capability 'Light'
        capability 'ColorControl'
        capability 'ColorTemperature'
        capability 'Polling'
        capability 'Initialize'
        capability 'Switch'
        capability "Switch Level"

        attribute "label", "string"
        attribute "group", "string"
        attribute "location", "string"

        command "setState", ["MAP"]
    }


    preferences {
        input 'logEnable', 'bool', title: 'Enable debug logging', required: false
    }
}

@SuppressWarnings("unused")
def installed() {
    initialize()
}

@SuppressWarnings("unused")
def updated() {
    unsubscribe 'LIFX.MULTIZONEQUERY'
    state.transitionTime = defaultTransition
    initialize()
    subscribe 'LIFX.MULTIZONEQUERY'
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
    parent.lifxQuery (device, 'DEVICE.GET_POWER') { List buffer -> sendPacket buffer }
    parent.lifxQuery (device, 'LIGHT.GET_STATE') { List buffer -> sendPacket buffer }
//    parent.lifxQuery (device, 'MULTIZONE.GET_EXTENDED_COLOR_ZONES') { List buffer -> sendPacket buffer }
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
def setColor(Map colorMap) {
    sendActions parent.deviceSetColor(device, colorMap, getUseActivityLogDebug(), state.transitionTime ?: 0)
}

@SuppressWarnings("unused")
def setHue(number) {
    logWarn "Setting hue is not supported"
}

@SuppressWarnings("unused")
def setSaturation(number) {
    logWarn "Setting saturation is not supported"
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
    actions.commands?.eachWithIndex { item, index -> parent.lifxCommand(device, item.cmd, item.payload, index as Byte) { List buffer -> sendPacket buffer, true } }
    actions.events?.each { sendEvent it }
}

def parse(String description) {
    List<Map> events = parent.parseForDevice(device, description, getUseActivityLog())
    events.collect { createEvent(it) }
}

private String myIp() {
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

