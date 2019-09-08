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

        command "setState", ["MAP"]
        command "zonesSave", [[name: "Zone name*", type: "STRING"]]
        command "zonesDelete", [[name: "Zone name*", type: "STRING"]]
        command "zonesLoad", [[name: "Zone name*", type: "STRING",], [name: "Duration", type: "NUMBER"]]
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
    state.transitionTime = defaultTransition
    initialize()
}

def initialize() {
    unschedule()
    requestInfo()
    runEvery1Minute poll
}

@SuppressWarnings("unused")
def refresh() {

}

def zonesSave(String name) {
    if (name == '') {
        return
    }
//    logDebug "Saving as $name"
    def zones = state.namedZones ?: [:]
//    logDebug "Last zones: ${state.lastMultizone}"
    def theZones = (state.lastMultizone as Map)
    theZones.colors = theZones.colors.collectEntries { k, v -> [k as Integer, v]}
    def compressed = compressMultizoneData theZones
    zones[name] = compressed
    state.namedZones = zones
    state.knownZones = zones.keySet().toString()
}

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
//    logDebug "Sending $theZones"
    sendActions parent.deviceSetZones(device, theZones)
}

def zonesDelete(String name) {
    state.namedZones?.remove(name)
    updateKnownZones()
}

private void updateKnownZones() {
    state.knownZones = state.namedZones?.keySet().toString()
}

@SuppressWarnings("unused")
def poll() {
    parent.lifxQuery(device, 'DEVICE.GET_POWER') { List buffer -> sendPacket buffer }
    parent.lifxQuery(device, 'LIGHT.GET_STATE') { List buffer -> sendPacket buffer }
    parent.lifxQuery(device, 'MULTIZONE.GET_EXTENDED_COLOR_ZONES') { List buffer -> sendPacket buffer }
}

def requestInfo() {
    poll()
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
    sendActions parent.deviceSetMultiLevel(device, level as Number, getUseActivityLog(), duration)
}

@SuppressWarnings("unused")
def setState(value) {
    sendActions parent.deviceSetState(device, stringToMap(value), getUseActivityLog(), state.transitionTime ?: 0)
}

private void sendActions(Map<String, List> actions) {
    actions.commands?.each { item -> parent.lifxCommand(device, item.cmd, item.payload) { List buffer -> sendPacket buffer, true } }
    actions.events?.each { sendEvent it }
}

def parse(String description) {
    List<Map> events = parent.parseForDevice(device, description, getUseActivityLog())
    def multizoneEvent = events.find { it.name == 'multizone' }
    if (null != multizoneEvent) {
        def data = multizoneEvent.data as Map
//        String compressed = compressMultizoneData data
//        def multizoneHtml = renderMultizone(data)
        state.lastMultizone = multizoneEvent.data
    }
    events.collect { createEvent(it) }
}

private String myIp() {
    device.getDeviceNetworkId()
}

private void sendPacket(List buffer, boolean noResponseExpected = false) {
//    logDebug "Sending buffer $buffer"
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


String renderMultizone(HashMap hashMap) {
    def builder = new StringBuilder();
    builder << '<table cellspacing="0">'
    def count = hashMap.colors_count as Integer
    Map<Integer, Map> colours = hashMap.colors
    builder << '<tr>'
    for (int i = 0; i < count; i++) {
        colour = colours[i];
        def rgb = renderDatum(colours[i])
        builder << '<td height="2" width="1" style="background-color:' + rgb + ';color:' + rgb + '">&nbsp;'
    }
    builder << '</tr></table>'
    def result = builder.toString()

    result
}

String renderDatum(Map item) {
    def rgb = parent.hsvToRgbString(
            paretnt.scaleDown100(item.hue as Long),
            paretnt.scaleDown100(item.saturation as Long),
            paretnt.scaleDown100(item.brightness as Long)
    )
    "$rgb"
}
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
//    logDebug "Count: $count"
    Map<Integer, Map> colors = data.colors as Map<Integer, Map>
//    logDebug "colors: $colors"
    List<String> stringColors = []
    for (int i = 0; i < count; i++) {
        Map hsbk = colors[i]
//        logDebug "Colors[$i]: $hsbk"
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

