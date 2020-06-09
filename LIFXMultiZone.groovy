/**
 *
 * Copyright 2018, 2019 Robert Heyes. All Rights Reserved
 *
 *  This software is free for Private Use. You may use and modify the software without distributing it.
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
        attribute "multizone", "string"
        attribute "effect", "string"

        command "setState", ["MAP"]
        command "zonesSave", [[name: "Zone name*", type: "STRING"]]
        command "zonesDelete", [[name: "Zone name*", type: "STRING"]]
        command "zonesLoad", [[name: "Zone name*", type: "STRING",], [name: "Duration", type: "NUMBER"]]
        command "setZones", [[name: "Zone HBSK Map*", type: "STRING"], [name: "Duration", type: "NUMBER"]]
        command "setEffect", [[name: "Effect type*", type: "ENUM", constraints: ["MOVE", "OFF"]], [name: "Speed", type: "NUMBER"], [name: "Direction", type: "ENUM", constraints: ["forward", "reverse"]]]
        command "createChildDevices", [[name: "Label prefix*", type: "STRING"]]
        command "deleteChildDevices"
        command 'setWaveform', [[name: 'Waveform*', type: 'ENUM', constraints:['SAW', 'SINE', 'HALF_SINE', 'TRIANGLE', 'PULSE']], [name: 'Color*', type: 'STRING'], [name: 'Transient', type: 'ENUM', constraints: ['true', 'false']], [name: 'Period', type: 'NUMBER'], [name: 'Cycles', type: 'NUMBER'], [name: 'Skew Ratio', type: 'NUMBER']]
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
    state.transitionTime = defaultTransition
    state.useActivityLog = useActivityLogFlag
    state.useActivityLogDebug = useDebugActivityLogFlag
    unschedule()
    getDeviceFirmware()
    requestInfo()
    runEvery1Minute poll
}

@SuppressWarnings("unused")
def refresh() {

}

def createChildDevices(String prefix) {
    def zoneCount = (state.lastMultizone as Map).zone_count
    for (i=0; i<state.zoneCount; i++) {
        try {
            addChildDevice(
                'robheyes',
                'LIFX Multizone Child',
                device.getDeviceNetworkId() + "_zone$i",
                [
                        label   : "$prefix Zone $i",
                        zone    : "$i"
                ]
            )
        } catch (com.hubitat.app.exception.UnknownDeviceTypeException e) {
            logWarn "${e.message} - you need to install the appropriate driver"
        } catch (IllegalArgumentException ignored) {
            // Intentionally ignored. Expected if device already present
        }
        
    }
}

def deleteChildDevices() {
    def children = getChildDevices()
    for (child in children) {
        deleteChildDevice(child.getDeviceNetworkId())
    }
}

@SuppressWarnings("unused")
def zonesSave(String name) {
    if (name == '') {
        return
    }
    def zones = state.namedZones ?: [:]
    def theZones = (state.lastMultizone as Map)
    theZones.colors = theZones.colors.collectEntries { k, v -> [k as Integer, v] }
    def compressed = compressMultizoneData theZones
    zones[name] = compressed
    state.namedZones = zones
    state.knownZones = zones.keySet().toString()
}

def setZones(String colors, duration = 0) {
    def theZones = (state.lastMultizone as Map)
    theZones.colors = theZones.colors.collectEntries { k, v -> [k as Integer, v] }
    def colorsMap = stringToMap(colors)
    colorsMap = colorsMap.collectEntries {k, v -> [k as Integer, stringToMap(v)] }
    for (i=0; i<82; i++) {
        //use special index 999 to apply attributes to all zones - overrides any zone-specific inputs
        def indexToApply = colorsMap[999] ? 999 : i
        if (colorsMap[i] != null || colorsMap[999] != null) {
            String namedColor = colorsMap[indexToApply].color ?: colorsMap[indexToApply].colour
            Map realColor
            if (namedColor) {
                Map myColor
                myColor = (null == namedColor) ? null : parent.lookupColor(namedColor.replace('_', ' '))
                realColor = [
                    hue       : parent.scaleUp(myColor.h ?: 0, 360),
                    saturation: parent.scaleUp100(myColor.s ?: 0),
                    brightness: parent.scaleUp100(myColor.v ?: 50)
                ]
            } else {
                realColor = parent.getScaledColorMap(colorsMap[indexToApply])
            }
            theZones.colors[i] = theZones.colors[i] + realColor
        }
    }
    theZones['apply'] = 1
    theZones['duration'] = duration
    sendActions parent.deviceSetZones(device, theZones)
}

@SuppressWarnings("unused")
def zonesLoad(String name, duration = 0) {
    if (null == state.namedZones) {
        logWarn 'No saved zones'
    }
    def zoneString = state.namedZones[name]
    if (null == zoneString) {
        logWarn "No such zone $name"
        return
    }

    def theZones = parent.getZones(zoneString)
    theZones['apply'] = 1
    theZones['duration'] = duration * 1000
    logDebug "Sending $theZones"
    sendActions parent.deviceSetZones(device, theZones)
}

def zonesDelete(String name) {
    state.namedZones?.remove(name)
    updateKnownZones()
}

private void updateKnownZones() {
    state.knownZones = state.namedZones?.keySet().toString()
}

def setEffect(String effectType, speed = 30, String direction = 'forward') {
    sendActions parent.deviceSetMultiZoneEffect(effectType, speed.toInteger(), direction)
}

@SuppressWarnings("unused")
def poll() {
    parent.lifxQuery(device, 'DEVICE.GET_POWER') { List buffer -> sendPacket buffer }
    parent.lifxQuery(device, 'LIGHT.GET_STATE') { List buffer -> sendPacket buffer }
    parent.lifxQuery(device, 'MULTIZONE.GET_EXTENDED_COLOR_ZONES') { List buffer -> sendPacket buffer }
    parent.lifxQuery(device, 'MULTIZONE.GET_MULTIZONE_EFFECT') { List buffer -> sendPacket buffer }
}

def requestInfo() {
    poll()
}

def getDeviceFirmware() {
    parent.lifxQuery(device, 'DEVICE.GET_HOST_FIRMWARE') { List buffer -> sendPacket buffer }
}

def updateChildDevices(multizoneData) {
    def power = device.currentValue("switch")
    def colors = (multizoneData as Map).colors
    colors = colors.collectEntries { k, v -> [k as Integer, v] }
    def children = getChildDevices()
    for (child in children) {
        def zone = child.getDataValue("zone") as Integer
        child.sendEvent(name: "hue", value: parent.scaleDown100(colors[zone].hue))
        child.sendEvent(name: "level", value: parent.scaleDown100(colors[zone].brightness))
        child.sendEvent(name: "saturation", value: parent.scaleDown100(colors[zone].saturation))
        child.sendEvent(name: "colorTemperature", value: colors[zone].kelvin)
        colors[zone].brightness ? child.sendEvent(name: "switch", value: power) : child.sendEvent(name: "switch", value: "off")
    }
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
    setZones('999:"[hue: ' + number + ']"', state.transitionTime ?: 0)
}

@SuppressWarnings("unused")
def setSaturation(number) {
    setZones('999:"[saturation: ' + number + ']"', state.transitionTime ?: 0)
}

@SuppressWarnings("unused")
def setColorTemperature(temperature) {
    setZones('999:"[saturation: 0, kelvin: ' + temperature + ']"', state.transitionTime ?: 0)
}

@SuppressWarnings("unused")
def setLevel(level, duration = 0) {
    setZones('999:"[brightness: ' + level + ']"', duration)
}

@SuppressWarnings("unused")
def setState(value) {
    sendActions parent.deviceSetState(device, stringToMap(value), getUseActivityLog(), state.transitionTime ?: 0)
}

def setWaveform(String waveform, String color, String isTransient = 'true', period = 5, cycles = 3.40282346638528860e38, skew = 0.5) {
    sendActions parent.deviceSetWaveform(device, isTransient.toBoolean(), stringToMap(color), period.toInteger(), cycles.toFloat(), skew.toFloat(), waveform)
}

private void sendActions(Map<String, List> actions) {
    actions.commands?.each { item -> parent.lifxCommand(device, item.cmd, item.payload) { List buffer -> sendPacket buffer, true } }
    actions.events?.each { sendEvent it }
}

def parse(String description) {
    List<Map> events = parent.parseForDevice(device, description, getUseActivityLog())
    def firmwareEvent = events.find { it.name == 'firmware' }
    firmwareEvent?.data ? state.firmware = firmwareEvent.data : null
    def multizoneEvent = events.find { it.name == 'multizone' }
    if (multizoneEvent?.data) {
        updateChildDevices(multizoneEvent.data)
        state.lastMultizone = multizoneEvent?.data
    }
    events.collect { createEvent(it) }
}

private String myIp() {
    device.getDeviceNetworkId()
}

private void sendPacket(List buffer, boolean noResponseExpected = false) {
    logDebug "Sending buffer $buffer"
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

//
//String renderMultizone(HashMap hashMap) {
//    def builder = new StringBuilder();
//    builder << '<table cellspacing="0">'
//    def count = hashMap.colors_count as Integer
//    Map<Integer, Map> colours = hashMap.colors
//    builder << '<tr>'
//    for (int i = 0; i < count; i++) {
//        colour = colours[i];
//        def rgb = renderDatum(colours[i])
//        builder << '<td height="2" width="1" style="background-color:' + rgb + ';color:' + rgb + '">&nbsp;'
//    }
//    builder << '</tr></table>'
//    def result = builder.toString()
//
//    result
//}

//String renderDatum(Map item) {
//    def rgb = parent.hsvToRgbString(
//            parent.scaleDown100(item.hue as Long),
//            parent.scaleDown100(item.saturation as Long),
//            parent.scaleDown100(item.brightness as Long)
//    )
//    "$rgb"
//}
String rle(List<String> colors) {
    StringBuilder builder = new StringBuilder('*')
    StringBuilder uniqueBuilder = new StringBuilder('@')
    String current = null
    Integer count = 0
    boolean allUnique = true
    colors.each {
        uniqueBuilder << it
        uniqueBuilder << "\n"
        if (it != current) {
            if (count > 0) {
                builder << sprintf("%02x\n", count)
            }
            count = 1
            current = it
            builder << current
        } else {
            count++
            allUnique = false
        }
    }
    if (count != 0) {
        builder << sprintf('%02x', count)
    }
    if (allUnique) {
        uniqueBuilder.toString()
    } else {
        builder.toString()
    }
}

String hsbkToString(Map hsbk) {
    sprintf '%04x%04x%04x%04x', hsbk.hue, hsbk.saturation, hsbk.brightness, hsbk.kelvin
}

String compressMultizoneData(Map data) {
    Integer count = data.colors_count as Integer
    logDebug "Count: $count"
    Map<Integer, Map> colors = data.colors as Map<Integer, Map>
    logDebug "colors: $colors"
    List<String> stringColors = []
    for (int i = 0; i < count; i++) {
        Map hsbk = colors[i]
        logDebug "Colors[$i]: $hsbk"
        stringColors << hsbkToString(hsbk)
    }
    rle stringColors
}

def getUseActivityLog() {
    if (state.useActivityLog == null) {
        state.useActivityLog = true
    }
    return state.useActivityLog
}

def setUseActivityLog(value) {
    log.debug("Setting useActivityLog to ${value ? 'true':'false'}")
    state.useActivityLog = value
}

def getUseActivityLogDebug() {
    if (state.useActivityLogDebug == null) {
        state.useActivityLogDebug = false
    }
    return state.useActivityLogDebug
}

def setUseActivityLogDebug(value) {
    log.debug("Setting useActivityLogDebug to ${value ? 'true':'false'}")
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

