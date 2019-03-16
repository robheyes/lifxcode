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

        attribute "label", "string"
        attribute "group", "string"
        attribute "location", "string"
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
    lifxQuery 'DEVICE.GET_POWER'
    lifxQuery 'LIGHT.GET_STATE'
//    lifxQuery 'MULTIZONE.GET_EXTENDED_COLOR_ZONES'
}

def on() {
    sendActions parent.deviceOnOff('on', getUseActivityLog(), state.transitionTime ?: 0)
}

def off() {
    sendActions parent.deviceOnOff('off', getUseActivityLog(), state.transitionTime ?: 0)
}

@SuppressWarnings("unused")
def setColor(Map colorMap) {

}

@SuppressWarnings("unused")
def setHue(number) {

}

@SuppressWarnings("unused")
def setSaturation(number) {

}

@SuppressWarnings("unused")
def setColorTemperature(temperature) {

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

