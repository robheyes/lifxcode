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
    definition(name: "LIFX Multizone Child", namespace: "robheyes", author: "David Kilgore", importUrl: 'https://raw.githubusercontent.com/robheyes/lifxcode/master/LIFXMultiZoneChild.groovy') {
        capability 'Light'
        capability 'ColorControl'
        capability 'ColorTemperature'
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
}

@SuppressWarnings("unused")
def refresh() {

}

def getZone() {
    return device.getDataValue("zone")
}

def on() {
    parent.setZones(getZone() + ': "[brightness:100]"')
    device.sendEvent(name: "switch", value: "on")
    device.sendEvent(name: "level", value: 100)
}

def off() {
    parent.setZones(getZone() + ': "[brightness:0]"')
    device.sendEvent(name: "switch", value: "off")
    device.sendEvent(name: "level", value: 0)
}

@SuppressWarnings("unused")
def setColor(Map colorMap) {
    parent.setZones(getZone() + ': "[hue: ' + colorMap.hue + ', saturation: ' + colorMap.saturation + ', brightness: '+ colorMap.level + ']"')
    device.sendEvent(name: "hue", value: colorMap.hue)
    device.sendEvent(name: "saturation", value: colorMap.saturation)
    device.sendEvent(name: "level", value: colorMap.level)
    if (colorMap.level > 0) {
        device.sendEvent(name: "switch", value: "on")
    } else if (colorMap.level == 0) {
        device.sendEvent(name: "switch", value: "off")
    }
}

@SuppressWarnings("unused")
def setHue(hue) {
    parent.setZones(getZone() + ': "[hue: ' + hue + ']"')
    device.sendEvent(name: "hue", value: hue)
}

@SuppressWarnings("unused")
def setSaturation(saturation) {
    parent.setZones(getZone() + ': "[saturation: ' + saturation + ']"')
    device.sendEvent(name: "saturation", value: saturation)
}

@SuppressWarnings("unused")
def setColorTemperature(temperature) {
    parent.setZones(getZone() + ': "[saturation: 0, kelvin: ' + temperature + ']"')
    device.sendEvent(name: "colorTemperature", value: temperature)
    device.sendEvent(name: "saturation", value: 0)
}

@SuppressWarnings("unused")
def setLevel(level, duration = 0) {
    parent.setZones(getZone() + ': "[brightness: ' + level + ']"', duration)
    device.sendEvent(name: "level", value: level)
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
