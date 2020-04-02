/**
 *
 *  Copyright 2020 David Kilgore. All Rights Reserved
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
    definition(name: "LIFX Multizone Child", namespace: "robheyes", author: "David Kilgore", importUrl: 'https://raw.githubusercontent.com/dkilgore90/lifxcode/master/LIFXMultiZoneChild.groovy') {
        capability 'Light'
        capability 'ColorControl'
        capability 'ColorTemperature'
        capability 'Polling'
        capability 'Initialize'
        capability 'Switch'
        capability "Switch Level"

        attribute "label", "string"
		attribute "zone", "number"
        command "setState", ["MAP"]
    }

    preferences {
        input "useActivityLogFlag", "bool", title: "Enable activity logging", required: false
        input "useDebugActivityLogFlag", "bool", title: "Enable debug logging", required: false
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
    state.useActivityLog = useActivityLogFlag
    state.useActivityLogDebug = useDebugActivityLogFlag
    unschedule()
    requestInfo()
    runEvery1Minute poll
}

@SuppressWarnings("unused")
def refresh() {

}

@SuppressWarnings("unused")
def poll() {
    return parent.lifxQuery(device, 'LIGHT.GET_STATE') { List buffer -> sendPacket buffer }
}

def requestInfo() {
    parent.lifxQuery(device, 'LIGHT.GET_STATE') { List buffer -> sendPacket buffer }
}

def on() {
    parent.setZones('$zone: "[brightness:100]"')
}

def off() {
    parent.setZones('$zone: "[brightness:0]"')
}

@SuppressWarnings("unused")
def setColor(Map colorMap) {
    parent.setZones('$zone: "[hue: $colorMap.hue, saturation: $colorMap.saturation, brightness: $colorMap.level]"')
}

@SuppressWarnings("unused")
def setHue(hue) {
    parent.setZones('$zone: "[hue: $hue]"')
}

@SuppressWarnings("unused")
def setSaturation(saturation) {
    parent.setZones('$zone: "[saturation: $saturation]"')
}

@SuppressWarnings("unused")
def setColorTemperature(temperature) {
    parent.setZones('$zone: "[kelvin: $temperature]"')
}

@SuppressWarnings("unused")
def setLevel(level, duration = 0) {
    parent.setZones('$zone: "[brightness: $level]"', duration)
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

Boolean getUseActivityLogDebug() {
    if (state.useActivityLogDebug == null) {
        state.useActivityLogDebug = false
    }
    return state.useActivityLogDebug as Boolean
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
