import groovy.transform.Field

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

definition(
        name: "LIFX Master",
        namespace: "robheyes",
        author: "Robert Alan Heyes",
        description: "Provides for discovery and control of LIFX devices",
        category: "Discovery",
        iconUrl: "",
        iconX2Url: "",
        iconX3Url: ""
)

preferences {
    page(name: 'mainPage')
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "", install: true, uninstall: true, refreshInterval: 10) {
        section('Options') {
            input 'interCommandPause', 'number', defaultValue: 50, title: 'Time between commands (milliseconds)', submitOnChange: true
//            input 'scanTimeLimit', 'number', title: 'Max scan time (seconds)', defaultValue: 600
            input 'maxPasses', 'number', title: 'Maximum number of passes', defaultValue: 2, submitOnChange: true
            input 'savePreferences', 'button', title: 'Save', submitOnChange: true
        }
        section('Discovery') {
            paragraph(styles())
            input 'discoverBtn', 'button', title: 'Discover devices'
            paragraph 'If not all of your devices are discovered the first time around, try the <strong>Discover only new devices</strong> button below'
            input 'discoverNewBtn', 'button', title: 'Discover only new devices'
            paragraph(
                    null == atomicState.scanPass ?
                            '' :
                            (
                                    ('DONE' == atomicState.scanPass) ?
                                            'Scanning complete' :
                                            "Scanning your network for devices, pass ${atomicState.scanPass}"
                            )

            )
            paragraph(
                    discoveryTextKnownDevices()
            )
        }
    }
}

private static String styles() {
    $/<style>
    ul {
        list-style-type: none;
    }
    
    ul.device-group {
        background: #81BC00;
        padding: 0;
    }
    
    ul.device {
        background: #D9ECB1;
    }
    
    li.device-group {
        font-weight: bold;
    }    
    
    li.device {
        font-weight: normal;
    }

    li.device-error {
        font-weight: bold;
        background: violet
    }
</style>/$
}

private String discoveryTextKnownDevices() {
    if ((atomicState.numDevices == null) || (0 == atomicState.numDevices)) {
        return 'No devices known'
    }

    ("I have found <strong>${atomicState.numDevices}</strong> LIFX "
            + ((1 == atomicState.numDevices) ? 'device' : 'useable devices')
            + ' so far:'
            + describeDevices())

}

private String describeDevices() {
    def sorted = getKnownIps().sort { a, b -> (a.value.label as String).compareToIgnoreCase(b.value.label as String) }
    def grouped = sorted.groupBy { it.value.group }
//    logDebug("Devices in groups = $grouped")


    def builder = new StringBuilder()
    builder << '<ul class="device-group">'
    grouped.each {
        groupName, devices ->
            builder << "<li class='device-group'>$groupName</li>"
            builder << '<ul class="device">'
            devices.each {
                ip, device ->
                    def item
                    builder << (device.error ? "<li class='device-error'>${device.label} (${device.error})</li>" : "<li class='device'>${device.label}</li>")
            }

            builder << '</ul>'
    }
    builder << '</ul>'
    builder.toString()
}

Integer interCommandPauseMilliseconds(int pass = 1) {
    (settings.interCommandPause ?: 50) + 10 * (pass - 1)
}

Integer maxScanTimeSeconds() {
    def overhead = 10
    def pause = (interCommandPauseMilliseconds() + 10) * 4 * 256

//    settings.scanTimeLimit ?: 600
    (pause * maxScanPasses()) / 1000 + 20
}

Integer maxScanPasses() {
    settings.maxPasses ?: 2
}

def updated() {
    logDebug 'LIFX updating'
    initialize()
}

def installed() {
    logDebug 'LIFX installed'
    initialize()
}

def uninstalled() {
    logDebug 'LIFX uninstalling - removing children'
    removeChildren()
    unsubscribe()
}

def initialize() {
//    refresh()
    updateKnownDevices()
}

private void updateKnownDevices() {
    def knownDevices = knownDeviceLabels()
    atomicState.numDevices = knownDevices.size()
//    atomicState.deviceNames = knownDevices.toSorted { a, b -> a.compareToIgnoreCase(b) }
}


def appButtonHandler(btn) {
    log.debug "appButtonHandler called with ${btn}"
    state.btnCall = btn
    if (state.btnCall == "discoverBtn") {
        refresh()
    } else if (state.btnCall == 'discoverNewBtn') {
        discoverNew()
    }
}

def setScanPass(pass) {
    atomicState.scanPass = pass ?: null
}

def refresh() {
    removeChildren()

    String subnet = getSubnet()
    if (!subnet) {
        return
    }
    discovery()
}

def discoverNew() {
    removeDiscoveryDevice()
    discovery()
}

private void discovery() {
    atomicState.scanPass = null
    updateKnownDevices()
    clearDeviceDefinitions()
//    def scanTimeSeconds = maxScanTimeSeconds()
//    logDebug "Max scan time = $scanTimeSeconds"
    def discoveryDevice = addChildDevice 'robheyes', 'LIFX Discovery', 'LIFX Discovery'
    // now schedule removal of the discovery device after a delay
    subscribe discoveryDevice, 'lifxdiscovery.complete', removeDiscoveryDevice
//    subscribe discoveryDevice, 'progress', progress
}

def progress(evt) {
    // no idea why this doesn't work when the subscribe is enabled
    logDebug "Event $evt"
    def percent = evt.getFloatValue()
    logDebug "%age $percent"
}

// used by runIn - DND
void removeDiscoveryDevice(evt) {
    logDebug("Event: $evt")
    logInfo 'Removing LIFX Discovery device'
    unsubscribe()
    atomicState.scanPass = 'DONE'
    try {
        deleteChildDevice 'LIFX Discovery'
    } catch (Exception e) {
        // don't care, let it fail
    }
}

void removeChildren() {
    logInfo "Removing child devices"
    childDevices.each {
        if (it != null) {
            deleteChildDevice it.deviceNetworkId
        }
    }
    atomicState.knownIps = [:]
    updateKnownDevices()
}

// Common device commands
Map<String, List> deviceOnOff(String value, Boolean displayed) {
    def actions = makeActions()
    actions.commands << makeCommand('DEVICE.SET_POWER', [level: value == 'on' ? 65535 : 0])
    actions.events << [name: "switch", value: value, displayed: displayed, data: [syncing: "false"]]
    actions
}

Map<String, List> deviceSetColor(device, Map colorMap, Boolean displayed, duration = 0) {
    def hsbkMap = getCurrentHSBK device
    hsbkMap << getScaledColorMap(colorMap)
    hsbkMap.duration = 1000 * (colorMap.duration ?: duration)

    deviceSetHSBKAndPower(duration, hsbkMap, displayed)
}

Map<String, List> deviceSetHue(device, Number hue, Boolean displayed, duration = 0) {
    def hsbkMap = getCurrentHSBK device
    hsbkMap.hue = scaleUp100 hue
    hsbkMap.duration = 1000 * duration

    deviceSetHSBKAndPower(duration, hsbkMap, displayed)
}

Map<String, List> deviceSetSaturation(device, Number saturation, Boolean displayed, duration = 0) {
    def hsbkMap = getCurrentHSBK device
    hsbkMap.saturation = scaleUp100 saturation
    hsbkMap.duration = 1000 * duration

    deviceSetHSBKAndPower(duration, hsbkMap, displayed)
}

Map<String, List> deviceSetColorTemperature(device, Number temperature, Boolean displayed, duration = 0) {
    def hsbkMap = getCurrentHSBK device
    hsbkMap.saturation = 0
    hsbkMap.kelvin = temperature
    hsbkMap.duration = 1000 * duration

    deviceSetHSBKAndPower(duration, hsbkMap, displayed)
}

Map<String, List> deviceSetLevel(device, Number level, Boolean displayed, duration = 0) {
    if ((level <= 0 || null == level) && 0 == duration) {
        return deviceOnOff('off', displayed)
    }
    if (level > 100) {
        level = 100
    }
    def hsbkMap = getCurrentHSBK(device)
//    logDebug("BK Map: $hsbkMap")
    if (hsbkMap.saturation == 0) {
        hsbkMap.level = scaleUp100 level
        hsbkMap.hue = 0
        hsbkMap.saturation = 0
        hsbkMap.duration = duration * 1000
    } else {
        hsbkMap = [
                hue       : scaleUp100(device.currentHue),
                saturation: scaleUp100(device.currentSaturation),
                level     : scaleUp100(level),
                kelvin    : device.currentColorTemperature,
                duration  : duration * 1000,
        ]
    }

    deviceSetHSBKAndPower(duration, hsbkMap, displayed)
}

Map<String, List> deviceSetState(device, Map myStateMap, Boolean displayed, duration = 0) {
    def power = myStateMap.power
    String color = myStateMap.color
    duration = (myStateMap.duration ?: duration)
    Map myColor = lookupColor(color)
    if (myColor) {
        def realColor = [
                hue       : scaleUp(myColor.hue, 360),
                saturation: scaleUp100(myColor.sat),
                level     : scaleUp100(myStateMap.level ?: myColor.lvl),
                kelvin    : device.currentColorTemperature,
                duration  : duration * 1000
        ]
        if (myColor.name) {
            realColor.name = myColor.name
        }
        deviceSetHSBKAndPower(duration, realColor, displayed, power)
    } else if (myStateMap.level) {
        deviceSetLevel(device, myStateMap.level, displayed, duration)
        // I think this should use the code from setLevel with brightness as the level value
    } else {
        [:] // do nothing
    }

}

List<Map> parseForDevice(device, String description, Boolean displayed) {
    Map header = parseHeaderFromDescription description
    switch (header.type) {
        case messageTypes().DEVICE.STATE_VERSION.type:
            log.warn("STATE_VERSION type ignored")
            return []
        case messageTypes().DEVICE.STATE_LABEL.type:
            def data = parsePayload 'DEVICE.STATE_LABEL', header
            String label = data.label
            device.setLabel(label.trim())
            return []
        case messageTypes().DEVICE.STATE_GROUP.type:
            def data = parsePayload 'DEVICE.STATE_GROUP', header
            String group = data.label
            return [[name: 'Group', value: group]]
        case messageTypes().DEVICE.STATE_LOCATION.type:
            def data = parsePayload 'DEVICE.STATE_LOCATION', header
            String location = data.label
            return [[name: 'Location', value: location]]
        case messageTypes().DEVICE.STATE_HOST_INFO.type:
            def data = parsePayload 'DEVICE.STATE_HOST_INFO', header
            logDebug("Wifi data $data")
            break
        case messageTypes().DEVICE.STATE_INFO.type:
            def data = parsePayload 'DEVICE.STATE_INFO', header
            break
        case messageTypes().LIGHT.STATE.type:
            def data = parsePayload 'LIGHT.STATE', header
//            logDebug "State: $data"
            device.setLabel data.label.trim()
            List<Map> result = [[name: "level", value: scaleDown(data.color.level, 100), displayed: displayed]]
            if (device.hasCapability('Color Control')) {
                result.add([name: "hue", value: scaleDown(data.color.hue, 100), displayed: displayed])
                result.add([name: "saturation", value: scaleDown(data.color.saturation, 100), displayed: displayed])
            }
            if (device.hasCapability('Color Temperature')) {
                result.add([name: "colorTemperature", value: data.color.kelvin as Integer, displayed: displayed])
            }
            if (device.hasCapability('Switch')) {
                result.add([name: 'switch', value: (data.power == 65535) ? 'on' : 'off', displayed: displayed])
            }
            return result
        case messageTypes().DEVICE.STATE_POWER.type:
            Map data = parsePayload 'DEVICE.STATE_POWER', header
            return [[name: "switch", value: (data.level as Integer == 0 ? "off" : "on"), displayed: displayed, data: [syncing: "false"]],]
        case messageTypes().LIGHT.STATE_POWER.type:
            Map data = parsePayload 'LIGHT.STATE_POWER', header
            logDebug "Data returned is $data"
            return [
                    [name: "switch", value: (data.level as Integer == 0 ? "off" : "on"), displayed: displayed, data: [syncing: "false"]],
                    [name: "level", value: (data.level as Integer == 0 ? 0 : 100), displayed: displayed, data: [syncing: "false"]]
            ]
        case messageTypes().DEVICE.ACKNOWLEDGEMENT.type:
            Byte sequence = header.sequence
            clearExpectedAckFor device, sequence
            return []
        default:
            logDebug "Unhandled response for ${header.type}"
            return []
    }
    return []
}

Map discoveryParse(String description) {
    Map deviceParams = parseDeviceParameters description
    String ip = convertIpLong(deviceParams.ip as String)
    Map parsed = parseHeader deviceParams
    final String mac = deviceParams.mac
    switch (parsed.type) {
        case messageTypes().DEVICE.STATE_VERSION.type:
            def existing = getDeviceDefinition mac
            if (!existing) {
                createDeviceDefinition parsed, ip, mac
                return [ip: ip, type: messageTypes().DEVICE.GET_GROUP.type as int]
            }
            break
        case messageTypes().DEVICE.STATE_LABEL.type:
            def data = parsePayload 'DEVICE.STATE_LABEL', parsed
            updateDeviceDefinition mac, [label: data.label]
            break
        case messageTypes().DEVICE.STATE_GROUP.type:
            def data = parsePayload 'DEVICE.STATE_GROUP', parsed
            updateDeviceDefinition mac, [group: data.label]
            return [ip: ip, type: messageTypes().DEVICE.GET_LOCATION.type as int]
            break
        case messageTypes().DEVICE.STATE_LOCATION.type:
            def data = parsePayload 'DEVICE.STATE_LOCATION', parsed
            updateDeviceDefinition mac, [location: data.label]
            return [ip: ip, type: messageTypes().DEVICE.GET_LABEL.type as int]
            break
        case messageTypes().DEVICE.STATE_WIFI_INFO.type:
            break
        case messageTypes().DEVICE.STATE_INFO.type:
            break
    }
}

private Map lookupColor(String color) {
    Map myColor
    if (color == "random") {
        myColor = pickRandomColor()
        log.info "Setting random color: ${myColor.name}"
    } else if (color?.startsWith('#')) {
        // convert rgb to hsv
        Map rgb = hexToColor color
        myColor = rgbToHSV rgb.r, rgb.g, rgb.b, 'high'
    } else {
        myColor = colorList().find { (it.name as String).equalsIgnoreCase(color) }
    }
    myColor.name = color
    myColor
}

private static Map pickRandomColor() {
    def colors = colorList()
    def tempRandom = Math.abs(new Random().nextInt() % colors.size())
    colors[tempRandom]
}

private List<Map> colorList() {
    colorMap
}

private static Map<String, List> deviceSetHSBKAndPower(duration, Map<String, Number> hsbkMap, boolean displayed, String power = 'on') {
    def actions = makeActions()

    if (null != power) {
//        logDebug "power change required $power"
        def powerLevel = 'on' == power ? 65535 : 0
        actions.commands << makeCommand('LIGHT.SET_POWER', [level: powerLevel, duration: duration])
        actions.events << [name: "switch", value: power, displayed: displayed, data: [syncing: "false"]]
    }

    if (hsbkMap) {
//        logDebug "color change required"
        actions.commands << makeCommand('LIGHT.SET_COLOR', hsbkMap)
        actions.events = actions.events + makeColorMapEvents(hsbkMap, displayed)
    }

    actions
}

private static List makeColorMapEvents(Map hsbkMap, Boolean displayed) {
    List<Map> colorMap = [
            [name: 'hue', value: scaleDown100(hsbkMap.hue), displayed: displayed],
            [name: 'saturation', value: scaleDown100(hsbkMap.saturation), displayed: displayed],
            [name: 'level', value: scaleDown100(hsbkMap.level), displayed: displayed],
            [name: 'colorTemperature', value: hsbkMap.kelvin as Integer, displayed: displayed]
    ]
    if (hsbkMap.name) {
        colorMap.add([name: 'colorName', value: hsbkMap.name, displayed: displayed])
    }
    colorMap
}

private static Map<String, Number> getScaledColorMap(Map colorMap) {
    [
            hue       : scaleUp100(colorMap.hue) as Integer,
            saturation: scaleUp100(colorMap.saturation) as Integer,
            level     : scaleUp100(colorMap.level) as Integer,
            kelvin    : colorMap.kelvin
    ]
}


private static Map makeCommand(String command, Map payload) {
    [cmd: command, payload: payload]
}

private static Map<String, List> makeActions() {
    [commands: [], events: []]
}

private static Map<String, Number> getCurrentHSBK(theDevice) {
    [
            hue       : scaleUp(theDevice.currentHue, 100),
            saturation: scaleUp(theDevice.currentSaturation, 100),
            level     : scaleUp(theDevice.currentValue('level') as Long, 100),
            kelvin    : theDevice.currentValue('colorTemperature')
    ]
}

private static Float scaleDown100(value) {
    scaleDown(value, 100)
}

private static Long scaleUp100(value) {
    scaleUp(value, 100)
}

private static Float scaleDown(value, maxValue) {
    (value * maxValue) / 65535
}

private static Long scaleUp(value, maxValue) {
    (value * 65535) / maxValue
}

private Map lookupDeviceAndType(String device, String type) {
    messageTypes()[device][type]
}

private String lookupDescriptorForDeviceAndType(String device, String type) {
    lookupDeviceAndType(device, type).descriptor
}

Map parseHeader(Map deviceParams) {
    List<Map> headerDescriptor = getDescriptor 'size:2l,misc:2l,source:4l,target:8a,frame_reserved:6a,flags:1,sequence:1,protocol_reserved:8a,type:2l,protocol_reserved2:2'
    parseBytes(headerDescriptor, (hubitat.helper.HexUtils.hexStringToIntArray(deviceParams.payload) as List<Long>).each {
        it & 0xff
    })
}

void createDeviceDefinition(Map parsed, String ip, String mac) {
    List<Map> stateVersionDescriptor = getDescriptor 'vendor:4l,product:4l,version:4l'
    def version = parseBytes stateVersionDescriptor, parsed.remainder as List<Long>
    def device = deviceVersion version
    device['ip'] = ip
    device['mac'] = mac
    saveDeviceDefinition device
}

Map getDeviceDefinition(String mac) {
    Map devices = getDeviceDefinitions()

    devices[mac]
}

private void clearDeviceDefinitions() {
    atomicState.devices = [:]
}

private Map getDeviceDefinitions() {
    atomicState.devices
}

private void saveDeviceDefinitions(Map devices) {
    atomicState.devices = devices
}

void saveDeviceDefinition(Map device) {
//    logDebug("Saving device $device")
    Map devices = getDeviceDefinitions()

    devices[device.mac] = device

    saveDeviceDefinitions devices
}

void deleteDeviceDefinition(Map device) {
    Map devices = getDeviceDefinitions()

    devices.remove device.mac

    saveDeviceDefinitions devices
}

void updateDeviceDefinition(String mac, Map properties) {
    Map device = getDeviceDefinition mac
    if (!device) {
        return
    }
    properties.each { key, val -> (device[key] = val) }
//        logDebug("Device being updated is $device")
//    if (!isDeviceComplete(device)) {
//        saveDeviceDefinition device
//        return
//    }
//
//    makeRealDevice(device)
//
    isDeviceComplete(device) ? makeRealDevice(device) : saveDeviceDefinition(device)
}

List knownDeviceLabels() {
    def knownDevices = getKnownIps()
    knownDevices.values().each { it.label }.asList()
}

private void makeRealDevice(Map device) {
    addToKnownIps device
    try {
        addChildDevice 'robheyes', device.deviceName, device.ip, null, [group: device.group, label: device.label, location: device.location]
        addToKnownIps device
        updateKnownDevices()
        logInfo "Added device $device.label of type $device.deviceName with ip address $device.ip"
    } catch (com.hubitat.app.exception.UnknownDeviceTypeException e) {
        // ignore this, expected if device already present
        logWarn "${e.message} - you need to install the appropriate driver"
        device.error = "No driver installed for $device.deviceName"
        addToKnownIps(device)
    } catch (IllegalArgumentException e) {
        // ignore
    }
    deleteDeviceDefinition device
}

private void addToKnownIps(Map device) {
    def knownIps = getKnownIps()
    knownIps[device.ip as String] = device //[ip: device.ip, label: device.label, error: device.error, mac: device.mac]
    atomicState.knownIps = knownIps
}

Boolean isKnownIp(String ip) {
    def knownIps = getKnownIps()
    null != knownIps[ip]
}

private void clearKnownIps() {
    atomicState.knownIps = [:]
}

private Map<String, Map> getKnownIps() {
    atomicState.knownIps ?: [:]
}

private static Boolean isDeviceComplete(Map device) {
    List missing = matchKeys device, ['ip', 'mac', 'group', 'label', 'location']
    missing.isEmpty()
}

private static List matchKeys(Map device, List<String> expected) {
    def result = []
    expected.each {
        if (!device.containsKey(it)) {
            result << it
        }
    }
    result
}


void expectAckFor(com.hubitat.app.DeviceWrapper device, Byte sequence, List buffer) {
    def expected = atomicState.expectedAckFor ?: [:]
    expected[device.getDeviceNetworkId() as String] = [sequence: sequence, buffer: buffer]
    atomicState.expectedAckFor = expected
}


Byte ackWasExpected(com.hubitat.app.DeviceWrapper device) {
    def expected = atomicState.expectedAckFor ?: [:]
    expected[device.getDeviceNetworkId() as String]?.sequence as Byte
}

void clearExpectedAckFor(com.hubitat.app.DeviceWrapper device, Byte sequence) {
    def expected = atomicState.expectedAckFor ?: [:]
    expected.remove(device.getDeviceNetworkId())
    atomicState.expectedAckFor = expected
}

List getBufferToResend(com.hubitat.app.DeviceWrapper device, Byte sequence) {
    def expected = atomicState.expectedAckFor ?: [:]
    Map expectation = expected[device.getDeviceNetworkId()]
    if (null == expectation) {
        null
    }
    if (expectation?.sequence == sequence) {
        expectation?.buffer
    } else {
        null
    }
}

String convertIpLong(String ip) {
    sprintf '%d.%d.%d.%d', hubitat.helper.HexUtils.hexStringToIntArray(ip)
}

private static String applySubscript(String descriptor, Number subscript) {
    descriptor.replace('!', subscript.toString())
}

private static String makeHSBKDescriptorList(int start, int end) {
    final def subscriptedColor = 'hue_!:2l;saturation_!;level_!:2l;kelvin_!:2l'
    def temp = []
    start.upto(end) {
        temp << applySubscript(subscriptedColor, it)
    }
    temp.join(';')
}

Map<String, Map<String, Map>> messageTypes() {
    return msgTypes
}

private static Map deviceVersion(Map device) {
    switch (device.product) {
        case 1:
            return [
                    name      : 'Original 1000',
                    deviceName: 'LIFX Color',
                    features  : [color: true, infrared: false, multizone: false, temperature_range: [min: 2500, max: 9000], chain: false]
            ]
        case 3:
            return [
                    name      : 'Color 650',
                    deviceName: 'LIFX Color',
                    features  : [color: true, infrared: false, multizone: false, temperature_range: [min: 2500, max: 9000], chain: false]
            ]
        case 10:
            return [
                    name      : 'White 800 (Low Voltage)',
                    deviceName: 'LIFX White',
                    features  : [color: false, infrared: false, multizone: false, temperature_range: [min: 2700, max: 6500], chain: false]
            ]
        case 11:
            return [
                    name      : 'White 800 (High Voltage)',
                    deviceName: 'LIFX White',
                    features  : [color: false, infrared: false, multizone: false, temperature_range: [min: 2700, max: 6500], chain: false]
            ]
        case 18:
            return [
                    name      : 'White 900 BR30 (Low Voltage)',
                    deviceName: 'LIFX White',
                    features  : [color: false, infrared: false, multizone: false, temperature_range: [min: 2700, max: 6500], chain: false]
            ]
        case 20:
            return [
                    name      : 'Color 1000 BR30',
                    deviceName: 'LIFX Color',
                    features  : [color: true, infrared: false, multizone: false, temperature_range: [min: 2500, max: 9000], chain: false]
            ]
        case 22:
            return [
                    name      : 'Color 1000',
                    deviceName: 'LIFX Color',
                    features  : [color: true, infrared: false, multizone: false, temperature_range: [min: 2500, max: 9000], chain: false]
            ]
        case 27:
        case 43:
            return [
                    name      : 'LIFX A19',
                    deviceName: 'LIFX Color',
                    features  : [color: true, infrared: false, multizone: false, temperature_range: [min: 2500, max: 9000], chain: false]
            ]
        case 28:
        case 44:
            return [
                    name      : 'LIFX BR30',
                    deviceName: 'LIFX Color',
                    features  : [color: true, infrared: false, multizone: false, temperature_range: [min: 2500, max: 9000], chain: false]
            ]
        case 29:
        case 45:
            return [
                    name      : 'LIFX+ A19',
                    deviceName: 'LIFXPlus Color',
                    features  : [color: true, infrared: true, multizone: false, temperature_range: [min: 2500, max: 9000], chain: false]
            ]
        case 30:
        case 46:
            return [
                    name      : 'LIFX+ BR30',
                    deviceName: 'LIFXPlus Color',
                    features  : [color: true, infrared: true, multizone: false, temperature_range: [min: 2500, max: 9000], chain: false]
            ]
        case 31:
            return [
                    name      : 'LIFX Z',
                    deviceName: 'LIFX Multizone',
                    features  : [color: true, infrared: false, multizone: true, temperature_range: [min: 2500, max: 9000], chain: false]
            ]
        case 32:
            return [
                    name      : 'LIFX Z 2',
                    deviceName: 'LIFX Multizone',
                    features  : [color: true, infrared: false, multizone: true, temperature_range: [min: 2500, max: 9000], chain: false]
            ]
        case 36:
        case 37:
            return [
                    name      : 'LIFX Downlight',
                    deviceName: 'LIFX Color',
                    features  : [color: true, infrared: false, multizone: false, temperature_range: [min: 2500, max: 9000], chain: false]
            ]
        case 38:
        case 56:
            return [
                    name      : 'LIFX Beam',
                    deviceName: 'LIFX Multizone',
                    features  : [color: true, infrared: false, multizone: true, temperature_range: [min: 2500, max: 9000], chain: false]
            ]
        case 49:
            return [
                    name      : 'LIFX Mini',
                    deviceName: 'LIFX Color',
                    features  : [color: true, infrared: false, multizone: false, temperature_range: [min: 2500, max: 9000], chain: false]
            ]
        case 50:
        case 60:
            return [
                    name      : 'LIFX Mini Day and Dusk',
                    deviceName: 'LIFX Day and Dusk',
                    features  : [color: false, infrared: false, multizone: false, temperature_range: [min: 1500, max: 4000], chain: false]
            ]
        case 51:
        case 61:
            return [
                    name      : 'LIFX Mini White',
                    deviceName: 'LIFX White Mono',
                    features  : [color: false, infrared: false, multizone: false, temperature_range: [min: 2700, max: 2700], chain: false]
            ]
        case 52:
            return [
                    name      : 'LIFX GU10',
                    deviceName: 'LIFX Color',
                    features  : [color: true, infrared: false, multizone: false, temperature_range: [min: 2500, max: 9000], chain: false]
            ]
        case 55:
            return [
                    name      : 'LIFX Tile',
                    deviceName: 'LIFX Tile',
                    features  : [color: true, infrared: false, multizone: false, temperature_range: [min: 2500, max: 9000], chain: true]
            ]

        case 59:
            return [
                    name      : 'LIFX Mini Color',
                    deviceName: 'LIFX Color',
                    features  : [color: true, infrared: false, multizone: false, temperature_range: [min: 2500, max: 9000], chain: false]
            ]
        default:
            return [name: "Unknown LIFX device with product id ${device.product}"]
    }
}


private static def rgbToHSV(r = 255, g = 255, b = 255, resolution = "low") {
    // Takes RGB (0-255) and returns HSV in 0-360, 0-100, 0-100
    // resolution ("low", "high") will return a hue between 0-100, or 0-360, respectively.

    r /= 255
    g /= 255
    b /= 255

    float h
    float s

    float max = Math.max(Math.max(r, g), b)
    float min = Math.min(Math.min(r, g), b)
    float delta = (max - min)
    float v = (max * 100.0)

    max != 0.0 ? (s = delta / max * 100.0) : (s = 0)

    if (s == 0.0) {
        h = 0.0
    } else {
        if (r == max) {
            h = ((g - b) / delta)
        } else if (g == max) {
            h = (2 + (b - r) / delta)
        } else if (b == max) {
            h = (4 + (r - g) / delta)
        }
    }

    h *= 60.0
    h < 0 ? (h += 360) : null

    resolution == "low" ? h /= 3.6 : null
    return [hue: h, sat: s, lvl: v]
}

private static Map hexToColor(String hex) {
    hex = hex.replace("#", "");
    switch (hex.length()) {
        case 6:
            return [
                    r: Integer.valueOf(hex.substring(0, 2), 16),
                    g: Integer.valueOf(hex.substring(2, 4), 16),
                    b: Integer.valueOf(hex.substring(4, 6), 16)
            ]
        case 8:
            return [
                    r: Integer.valueOf(hex.substring(0, 2), 16),
                    g: Integer.valueOf(hex.substring(2, 4), 16),
                    b: Integer.valueOf(hex.substring(4, 6), 16),
                    a: Integer.valueOf(hex.substring(6, 8), 16)
            ]
    }
    return null;
}

private static Map parseDeviceParameters(String description) {
    def deviceParams = [:]
    description.findAll(~/(\w+):(\w+)/) {
        (deviceParams[it[1]] = it[2])
    }
    deviceParams
}

private Map parseHeaderFromDescription(String description) {
    parseHeader parseDeviceParameters(description)
}

private Map parsePayload(String theDevice, String theType, Map header) {
    parseBytes lookupDescriptorForDeviceAndType(theDevice, theType), getRemainder(header)
}

private Map parsePayload(String deviceAndType, Map header) {
    def parts = deviceAndType.split(/\./)
    parsePayload parts[0], parts[1], header
}

private Map parseBytes(String descriptor, List<Long> bytes) {
//    logDebug("Looking for descriptor for ${descriptor}")
    parseBytes getDescriptor(descriptor), bytes
}

private void processSegment(Map result, List<Long> data, Map item, name) {
    switch (kind) {
        case 'B':
        case 'W':
        case 'I':
        case 'L':
            data.reverse()
            storeValue result, data, item.size, name
            break
        case 'F':
            data = data.reverse()
            Long value = 0
            data.each { value = (value * 256) + it }
            def theFloat = Float.intBitsToFloat(value)
            result.put name, theFloat
            break
        case 'T':
            result.put name, new String((data.findAll { it != 0 }) as byte[])
            break
        case 'H':
            Map color = parseBytes 'hue:w;saturation:w;level:w,kelvin:w', data
            result.put name, color
            break
        default:
            throw new RuntimeException("Unexpected item kind '$kind'")
    }
}

private Map parseBytes(List<Map> descriptor, List<Long> bytes) {
    Map result = new HashMap()
    int offset = 0
    for (item in descriptor) {
        String kind = item.kind
        // partition the data
        int totalLength = item.size * item.count
        int nextOffset = offset + totalLength
        List<Long> data = bytes.subList offset, nextOffset
        assert (data.size() == item.bytes as int)
        offset = nextOffset

        if (item.isArray) {
            def subMap = [:]
            for (int i = 0; i < item.count; i++) {
                processSegment subMap, data, item.kind, item.size, i
            }
            result.put item.name, subMap
        } else {
            processSegment result, data, item.kind, item.size, item.name
        }
//        if ('H' == kind) {
//            int nextOffset = offset + 8
//            List<Long> data = bytes.subList offset, nextOffset
//            assert (data.size() == item.bytes as int)
//            offset = nextOffset
//            Map color = parseBytes('hue:2l;saturation:2l;level:2l,kelvin:2l', data)
//            result.put 'hue', color.hue
//            result.put 'saturation', color.saturation
//            result.put 'level', color.level
//            result.put 'kelvin', color.kelvin
//            result.put item.name, color
//            return result
//        }
//
//        int nextOffset = offset + (item.bytes as int)
//
//        List<Long> data = bytes.subList offset, nextOffset
//        assert (data.size() == item.bytes as int)
//        offset = nextOffset
//        // assume big kind
//        //for now
//
//        if ('A' == kind) {
//            result.put(item.name, data)
//            return result
//        }
//        if (kind.startsWith('A')) {
//            def realKind = kind[1]
//
//        }
//        if ('S' == kind) {
//            result.put(item.name, new String((data.findAll { it != 0 }) as byte[]))
//            return result
//        }
//        if ('F' == kind) {
//            data = data.reverse()
//            BigInteger value = 0
//            data.each { value = (value * 256) + it }
//            def theFloat = Float.intBitsToFloat(value)
//            result.put(item.name, theFloat)
//            return result
//        }
//        if ('B' != kind) {
//            data = data.reverse()
//        }
//
//        storeValue(result, data, item.bytes, item.name)
    }
    if (offset < bytes.size()) {
        result.put 'remainder', bytes[offset..-1]
    }
    return result
}
//
//private Map parseBytes(List<Map> descriptor, List<Long> bytes) {
//    Map result = new HashMap()
//    int offset = 0
//    descriptor.each { item ->
//        String kind = item.kind
//        Int length
//        if ('H' == kind) {
//            int nextOffset = offset + 8
//            List<Long> data = bytes.subList offset, nextOffset
//            assert (data.size() == item.bytes as int)
//            offset = nextOffset
//            Map color = parseBytes('hue:2l;saturation:2l;level:2l,kelvin:2l', data)
//            result.put 'hue', color.hue
//            result.put 'saturation', color.saturation
//            result.put 'level', color.level
//            result.put 'kelvin', color.kelvin
//            result.put item.name, color
//            return result
//        }
//
//        int nextOffset = offset + (item.bytes as int)
//
//        List<Long> data = bytes.subList offset, nextOffset
//        assert (data.size() == item.bytes as int)
//        offset = nextOffset
//        // assume big kind
//        //for now
//
//        if ('A' == kind) {
//            result.put(item.name, data)
//            return result
//        }
//        if (kind.startsWith('A')) {
//            def realKind = kind[1]
//
//        }
//        if ('S' == kind) {
//            result.put(item.name, new String((data.findAll { it != 0 }) as byte[]))
//            return result
//        }
//        if ('F' == kind) {
//            data = data.reverse()
//            BigInteger value = 0
//            data.each { value = (value * 256) + it }
//            def theFloat = Float.intBitsToFloat(value)
//            result.put(item.name, theFloat)
//            return result
//        }
//        if ('B' != kind) {
//            data = data.reverse()
//        }
//
//        storeValue(result, data, item.bytes, item.name)
//    }
//    if (offset < bytes.size()) {
//        result.put 'remainder', bytes[offset..-1]
//    }
//    return result
//}

private void storeValue(Map result, List<Long> data, numBytes, String name, boolean trace = false) {
    BigInteger value = 0
    data.each { value = (value * 256) + it }
    def theValue
    switch (numBytes) {
        case 1:
            theValue = (value & 0xFF) as long
            break
        case 2:
            theValue = (value & 0xFFFF) as long
            break
        case 3: case 4:
            theValue = (value & 0xFFFFFFFF) as long
            break
        default: // this should complain if longer than 8 bytes
            theValue = (value & 0xFFFFFFFFFFFFFFFF) as long
    }
    result.put name, theValue
}

List makePayload(String device, String type, Map payload) {
    def descriptor = getDescriptor lookupDescriptorForDeviceAndType(device, type)
    def result = []
    descriptor.each {
        Map item ->
            if ('H' == item.kind) {
                add result, (payload['hue'] ?: 0) as short
                add result, (payload['saturation'] ?: 0) as short
                add result, (payload['level'] ?: 0) as short
                add result, (payload['kelvin'] ?: 0) as short
                return
            }
            def value = payload[item.name] ?: 0
            //TODO possibly extend this to the other types A,S & B
            switch (item.bytes as int) {
                case 1:
                    add result, value as byte
                    break
                case 2:
                    add result, value as short
                    break
                case 3: case 4:
                    add result, value as int
                    break
                default: // this should complain if longer than 8 bytes
                    add result, value as long
            }
    }
    result as List<Byte>
}

private static List<Long> getRemainder(header) { header.remainder as List<Long> }

void clearCachedDescriptors() { atomicState.cachedDescriptors = null }

private List<Map> getDescriptor(String desc) {
    def cachedDescriptors = atomicState.cachedDescriptors
    if (null == cachedDescriptors) {
        cachedDescriptors = new HashMap<String, List<Map>>()
    }
    List<Map> candidate = cachedDescriptors.get(desc)
    if (!candidate) {
        candidate = makeDescriptor desc
        cachedDescriptors[desc] = (candidate)
        atomicState.cachedDescriptors = cachedDescriptors
    }
    candidate
}

private static Number itemLength(String kind) {
    switch (kind) {
        case 'B': return 1
        case 'W': return 2
        case 'I': return 4
        case 'L': return 8
        case 'H': return 8
        case 'F': return 4
        case 'T': return 1 // length of character
        default:
            throw new RuntimeException("Unexpected item kind '$kind'")
    }
}

private static List<Map> makeDescriptor(String desc) {
    desc.findAll(~/(\w+):([bBwWiIlLhHfFtT][aA]?)(\d+)?/) {
        full ->
            def theKind = full[2].toUpperCase()
            def baseKind = theKind[0]
            def isArray = theKind.length() > 1 && theKind[1] == 'A'
            [
                    name   : full[1],
                    kind   : baseKind,
                    isArray: isArray,
                    count  : full[3]?.toInteger() ?: 1,
                    size   : itemLength(baseKind)
            ]
    }
}

String getSubnet() {
    def ip = getHubIP()
    def m = ip =~ /^(\d{1,3}\.\d{1,3}\.\d{1,3}\.)\d{1,3}/
    if (!m) {
        logWarn('ip does not match pattern')
        return null
    }
    return m.group(1)
}

//def /* Hub */ getHub() {
//    location.hubs[0]
//}

static Integer getTypeFor(String dev, String act) {
    def deviceAndType = lookupDeviceAndType dev, act
    deviceAndType.type as Integer
}

String getHubIP() {
    def hub = location.hubs[0]

    hub.localIP
}

byte makePacket(List buffer, String device, String type, Map payload, Boolean responseRequired = true, Boolean ackRequired = false, Byte sequence = null) {
    def listPayload = makePayload(device, type, payload)
    int messageType = lookupDeviceAndType(device, type).type
    makePacket(buffer, [0, 0, 0, 0, 0, 0] as byte[], messageType, ackRequired, responseRequired, listPayload, sequence)
}

// fills the buffer with the LIFX packet and returns the sequence number
byte makePacket(List buffer, byte[] targetAddress, int messageType, Boolean ackRequired = false, Boolean responseRequired = false, List payload = [], Byte sequence = null) {
    def lastSequence = sequence ?: sequenceNumber()
    createFrame buffer, targetAddress.every { it == 0 }
    createFrameAddress buffer, targetAddress, ackRequired, responseRequired, lastSequence
    createProtocolHeader buffer, messageType as short
    createPayload buffer, payload as byte[]

    put buffer, 0, buffer.size() as short
    return lastSequence
}


byte[] asByteArray(List buffer) {
    (buffer.each { it as byte }) as byte[]
}

Byte sequenceNumber() {
    atomicState.sequence = ((atomicState.sequence ?: 0) + 1) % 128
}

/** Protocol packet building */

private static def createFrame(List buffer, boolean tagged) {
    add buffer, 0 as short
    add buffer, 0x00 as byte
    add buffer, (tagged ? 0x34 : 0x14) as byte
    add buffer, lifxSource()
}

private static int lifxSource() {
    0x48454C44 // = HELD: Hubitat Elevation LIFX Device :)
}

private static def createFrameAddress(List buffer, byte[] target, boolean ackRequired, boolean responseRequired, Byte sequenceNumber) {
    add buffer, target
    add buffer, 0 as short
    fill buffer, 0 as byte, 6
    add buffer, ((ackRequired ? 0x02 : 0) | (responseRequired ? 0x01 : 0)) as byte
    add buffer, sequenceNumber as byte
}

private static def createProtocolHeader(List buffer, short messageType) {
    fill buffer, 0 as byte, 8
    add buffer, messageType
    add buffer, 0 as short
}

private static def createPayload(List buffer, byte[] payload) {
    add buffer, payload
}

/** LOW LEVEL BUFFER FILLING */
private static void add(List buffer, byte value) {
    buffer.add Byte.toUnsignedInt(value)
}

private static void add(List buffer, short value) {
    def lower = value & 0xff
    add buffer, lower as byte
    add buffer, ((value - lower) >>> 8) as byte
}

private static void add(List buffer, int value) {
    def lower = value & 0xffff
    add buffer, lower as short
    add buffer, Integer.divideUnsigned(value - lower, 0x10000) as short
}

private static void add(List buffer, long value) {
    def lower = value & 0xffffffff
    add buffer, lower as int
    add buffer, Long.divideUnsigned(value - lower, 0x100000000) as int
}

private static void add(List buffer, byte[] values) {
    for (value in values) {
        add buffer, value
    }
}

private static void fill(List buffer, byte value, int count) {
    for (int i = 0; i < count; i++) {
        add buffer, value
    }
}

private static void put(List buffer, int index, byte value) {
    buffer.set index, Byte.toUnsignedInt(value)
}

private static void put(List buffer, int index, short value) {
    def lower = value & 0xff
    put buffer, index, lower as byte
    put buffer, index + 1, ((value - lower) >>> 8) as byte
}

/** LOGGING **/
void logDebug(msg) {
    log.debug msg
}

void logInfo(msg) {
    log.info msg
}

void logWarn(String msg) {
    log.warn msg
}


@Field List<Map> colorMap =
        [
                [name: 'Alice Blue', rgb: '#F0F8FF', hue: 208, sat: 100, lvl: 97],
                [name: 'Antique White', rgb: '#FAEBD7', hue: 34, sat: 78, lvl: 91],
                [name: 'Aqua', rgb: '#00FFFF', hue: 180, sat: 100, lvl: 50],
                [name: 'Aquamarine', rgb: '#7FFFD4', hue: 160, sat: 100, lvl: 75],
                [name: 'Azure', rgb: '#F0FFFF', hue: 180, sat: 100, lvl: 97],
                [name: 'Beige', rgb: '#F5F5DC', hue: 60, sat: 56, lvl: 91],
                [name: 'Bisque', rgb: '#FFE4C4', hue: 33, sat: 100, lvl: 88],
                [name: 'Blanched Almond', rgb: '#FFEBCD', hue: 36, sat: 100, lvl: 90],
                [name: 'Blue', rgb: '#0000FF', hue: 240, sat: 100, lvl: 50],
                [name: 'Blue Violet', rgb: '#8A2BE2', hue: 271, sat: 76, lvl: 53],
                [name: 'Brown', rgb: '#A52A2A', hue: 0, sat: 59, lvl: 41],
                [name: 'Burly Wood', rgb: '#DEB887', hue: 34, sat: 57, lvl: 70],
                [name: 'Cadet Blue', rgb: '#5F9EA0', hue: 182, sat: 25, lvl: 50],
                [name: 'Chartreuse', rgb: '#7FFF00', hue: 90, sat: 100, lvl: 50],
                [name: 'Chocolate', rgb: '#D2691E', hue: 25, sat: 75, lvl: 47],
                [name: 'Cool White', rgb: '#F3F6F7', hue: 187, sat: 19, lvl: 96],
                [name: 'Coral', rgb: '#FF7F50', hue: 16, sat: 100, lvl: 66],
                [name: 'Corn Flower Blue', rgb: '#6495ED', hue: 219, sat: 79, lvl: 66],
                [name: 'Corn Silk', rgb: '#FFF8DC', hue: 48, sat: 100, lvl: 93],
                [name: 'Crimson', rgb: '#DC143C', hue: 348, sat: 83, lvl: 58],
                [name: 'Cyan', rgb: '#00FFFF', hue: 180, sat: 100, lvl: 50],
                [name: 'Dark Blue', rgb: '#00008B', hue: 240, sat: 100, lvl: 27],
                [name: 'Dark Cyan', rgb: '#008B8B', hue: 180, sat: 100, lvl: 27],
                [name: 'Dark Golden Rod', rgb: '#B8860B', hue: 43, sat: 89, lvl: 38],
                [name: 'Dark Gray', rgb: '#A9A9A9', hue: 0, sat: 0, lvl: 66],
                [name: 'Dark Green', rgb: '#006400', hue: 120, sat: 100, lvl: 20],
                [name: 'Dark Khaki', rgb: '#BDB76B', hue: 56, sat: 38, lvl: 58],
                [name: 'Dark Magenta', rgb: '#8B008B', hue: 300, sat: 100, lvl: 27],
                [name: 'Dark Olive Green', rgb: '#556B2F', hue: 82, sat: 39, lvl: 30],
                [name: 'Dark Orange', rgb: '#FF8C00', hue: 33, sat: 100, lvl: 50],
                [name: 'Dark Orchid', rgb: '#9932CC', hue: 280, sat: 61, lvl: 50],
                [name: 'Dark Red', rgb: '#8B0000', hue: 0, sat: 100, lvl: 27],
                [name: 'Dark Salmon', rgb: '#E9967A', hue: 15, sat: 72, lvl: 70],
                [name: 'Dark Sea Green', rgb: '#8FBC8F', hue: 120, sat: 25, lvl: 65],
                [name: 'Dark Slate Blue', rgb: '#483D8B', hue: 248, sat: 39, lvl: 39],
                [name: 'Dark Slate Gray', rgb: '#2F4F4F', hue: 180, sat: 25, lvl: 25],
                [name: 'Dark Turquoise', rgb: '#00CED1', hue: 181, sat: 100, lvl: 41],
                [name: 'Dark Violet', rgb: '#9400D3', hue: 282, sat: 100, lvl: 41],
                [name: 'Daylight White', rgb: '#CEF4FD', hue: 191, sat: 9, lvl: 90],
                [name: 'Deep Pink', rgb: '#FF1493', hue: 328, sat: 100, lvl: 54],
                [name: 'Deep Sky Blue', rgb: '#00BFFF', hue: 195, sat: 100, lvl: 50],
                [name: 'Dim Gray', rgb: '#696969', hue: 0, sat: 0, lvl: 41],
                [name: 'Dodger Blue', rgb: '#1E90FF', hue: 210, sat: 100, lvl: 56],
                [name: 'Fire Brick', rgb: '#B22222', hue: 0, sat: 68, lvl: 42],
                [name: 'Floral White', rgb: '#FFFAF0', hue: 40, sat: 100, lvl: 97],
                [name: 'Forest Green', rgb: '#228B22', hue: 120, sat: 61, lvl: 34],
                [name: 'Fuchsia', rgb: '#FF00FF', hue: 300, sat: 100, lvl: 50],
                [name: 'Gainsboro', rgb: '#DCDCDC', hue: 0, sat: 0, lvl: 86],
                [name: 'Ghost White', rgb: '#F8F8FF', hue: 240, sat: 100, lvl: 99],
                [name: 'Gold', rgb: '#FFD700', hue: 51, sat: 100, lvl: 50],
                [name: 'Golden Rod', rgb: '#DAA520', hue: 43, sat: 74, lvl: 49],
                [name: 'Gray', rgb: '#808080', hue: 0, sat: 0, lvl: 50],
                [name: 'Green', rgb: '#008000', hue: 120, sat: 100, lvl: 25],
                [name: 'Green Yellow', rgb: '#ADFF2F', hue: 84, sat: 100, lvl: 59],
                [name: 'Honeydew', rgb: '#F0FFF0', hue: 120, sat: 100, lvl: 97],
                [name: 'Hot Pink', rgb: '#FF69B4', hue: 330, sat: 100, lvl: 71],
                [name: 'Indian Red', rgb: '#CD5C5C', hue: 0, sat: 53, lvl: 58],
                [name: 'Indigo', rgb: '#4B0082', hue: 275, sat: 100, lvl: 25],
                [name: 'Ivory', rgb: '#FFFFF0', hue: 60, sat: 100, lvl: 97],
                [name: 'Khaki', rgb: '#F0E68C', hue: 54, sat: 77, lvl: 75],
                [name: 'Lavender', rgb: '#E6E6FA', hue: 240, sat: 67, lvl: 94],
                [name: 'Lavender Blush', rgb: '#FFF0F5', hue: 340, sat: 100, lvl: 97],
                [name: 'Lawn Green', rgb: '#7CFC00', hue: 90, sat: 100, lvl: 49],
                [name: 'Lemon Chiffon', rgb: '#FFFACD', hue: 54, sat: 100, lvl: 90],
                [name: 'Light Blue', rgb: '#ADD8E6', hue: 195, sat: 53, lvl: 79],
                [name: 'Light Coral', rgb: '#F08080', hue: 0, sat: 79, lvl: 72],
                [name: 'Light Cyan', rgb: '#E0FFFF', hue: 180, sat: 100, lvl: 94],
                [name: 'Light Golden Rod Yellow', rgb: '#FAFAD2', hue: 60, sat: 80, lvl: 90],
                [name: 'Light Gray', rgb: '#D3D3D3', hue: 0, sat: 0, lvl: 83],
                [name: 'Light Green', rgb: '#90EE90', hue: 120, sat: 73, lvl: 75],
                [name: 'Light Pink', rgb: '#FFB6C1', hue: 351, sat: 100, lvl: 86],
                [name: 'Light Salmon', rgb: '#FFA07A', hue: 17, sat: 100, lvl: 74],
                [name: 'Light Sea Green', rgb: '#20B2AA', hue: 177, sat: 70, lvl: 41],
                [name: 'Light Sky Blue', rgb: '#87CEFA', hue: 203, sat: 92, lvl: 75],
                [name: 'Light Slate Gray', rgb: '#778899', hue: 210, sat: 14, lvl: 53],
                [name: 'Light Steel Blue', rgb: '#B0C4DE', hue: 214, sat: 41, lvl: 78],
                [name: 'Light Yellow', rgb: '#FFFFE0', hue: 60, sat: 100, lvl: 94],
                [name: 'Lime', rgb: '#00FF00', hue: 120, sat: 100, lvl: 50],
                [name: 'Lime Green', rgb: '#32CD32', hue: 120, sat: 61, lvl: 50],
                [name: 'Linen', rgb: '#FAF0E6', hue: 30, sat: 67, lvl: 94],
                [name: 'Maroon', rgb: '#800000', hue: 0, sat: 100, lvl: 25],
                [name: 'Medium Aquamarine', rgb: '#66CDAA', hue: 160, sat: 51, lvl: 60],
                [name: 'Medium Blue', rgb: '#0000CD', hue: 240, sat: 100, lvl: 40],
                [name: 'Medium Orchid', rgb: '#BA55D3', hue: 288, sat: 59, lvl: 58],
                [name: 'Medium Purple', rgb: '#9370DB', hue: 260, sat: 60, lvl: 65],
                [name: 'Medium Sea Green', rgb: '#3CB371', hue: 147, sat: 50, lvl: 47],
                [name: 'Medium Slate Blue', rgb: '#7B68EE', hue: 249, sat: 80, lvl: 67],
                [name: 'Medium Spring Green', rgb: '#00FA9A', hue: 157, sat: 100, lvl: 49],
                [name: 'Medium Turquoise', rgb: '#48D1CC', hue: 178, sat: 60, lvl: 55],
                [name: 'Medium Violet Red', rgb: '#C71585', hue: 322, sat: 81, lvl: 43],
                [name: 'Midnight Blue', rgb: '#191970', hue: 240, sat: 64, lvl: 27],
                [name: 'Mint Cream', rgb: '#F5FFFA', hue: 150, sat: 100, lvl: 98],
                [name: 'Misty Rose', rgb: '#FFE4E1', hue: 6, sat: 100, lvl: 94],
                [name: 'Moccasin', rgb: '#FFE4B5', hue: 38, sat: 100, lvl: 85],
                [name: 'Navajo White', rgb: '#FFDEAD', hue: 36, sat: 100, lvl: 84],
                [name: 'Navy', rgb: '#000080', hue: 240, sat: 100, lvl: 25],
                [name: 'Old Lace', rgb: '#FDF5E6', hue: 39, sat: 85, lvl: 95],
                [name: 'Olive', rgb: '#808000', hue: 60, sat: 100, lvl: 25],
                [name: 'Olive Drab', rgb: '#6B8E23', hue: 80, sat: 60, lvl: 35],
                [name: 'Orange', rgb: '#FFA500', hue: 39, sat: 100, lvl: 50],
                [name: 'Orange Red', rgb: '#FF4500', hue: 16, sat: 100, lvl: 50],
                [name: 'Orchid', rgb: '#DA70D6', hue: 302, sat: 59, lvl: 65],
                [name: 'Pale Golden Rod', rgb: '#EEE8AA', hue: 55, sat: 67, lvl: 80],
                [name: 'Pale Green', rgb: '#98FB98', hue: 120, sat: 93, lvl: 79],
                [name: 'Pale Turquoise', rgb: '#AFEEEE', hue: 180, sat: 65, lvl: 81],
                [name: 'Pale Violet Red', rgb: '#DB7093', hue: 340, sat: 60, lvl: 65],
                [name: 'Papaya Whip', rgb: '#FFEFD5', hue: 37, sat: 100, lvl: 92],
                [name: 'Peach Puff', rgb: '#FFDAB9', hue: 28, sat: 100, lvl: 86],
                [name: 'Peru', rgb: '#CD853F', hue: 30, sat: 59, lvl: 53],
                [name: 'Pink', rgb: '#FFC0CB', hue: 350, sat: 100, lvl: 88],
                [name: 'Plum', rgb: '#DDA0DD', hue: 300, sat: 47, lvl: 75],
                [name: 'Powder Blue', rgb: '#B0E0E6', hue: 187, sat: 52, lvl: 80],
                [name: 'Purple', rgb: '#800080', hue: 300, sat: 100, lvl: 25],
                [name: 'Red', rgb: '#FF0000', hue: 0, sat: 100, lvl: 50],
                [name: 'Rosy Brown', rgb: '#BC8F8F', hue: 0, sat: 25, lvl: 65],
                [name: 'Royal Blue', rgb: '#4169E1', hue: 225, sat: 73, lvl: 57],
                [name: 'Saddle Brown', rgb: '#8B4513', hue: 25, sat: 76, lvl: 31],
                [name: 'Salmon', rgb: '#FA8072', hue: 6, sat: 93, lvl: 71],
                [name: 'Sandy Brown', rgb: '#F4A460', hue: 28, sat: 87, lvl: 67],
                [name: 'Sea Green', rgb: '#2E8B57', hue: 146, sat: 50, lvl: 36],
                [name: 'Sea Shell', rgb: '#FFF5EE', hue: 25, sat: 100, lvl: 97],
                [name: 'Sienna', rgb: '#A0522D', hue: 19, sat: 56, lvl: 40],
                [name: 'Silver', rgb: '#C0C0C0', hue: 0, sat: 0, lvl: 75],
                [name: 'Sky Blue', rgb: '#87CEEB', hue: 197, sat: 71, lvl: 73],
                [name: 'Slate Blue', rgb: '#6A5ACD', hue: 248, sat: 53, lvl: 58],
                [name: 'Slate Gray', rgb: '#708090', hue: 210, sat: 13, lvl: 50],
                [name: 'Snow', rgb: '#FFFAFA', hue: 0, sat: 100, lvl: 99],
                [name: 'Soft White', rgb: '#B6DA7C', hue: 83, sat: 44, lvl: 67],
                [name: 'Spring Green', rgb: '#00FF7F', hue: 150, sat: 100, lvl: 50],
                [name: 'Steel Blue', rgb: '#4682B4', hue: 207, sat: 44, lvl: 49],
                [name: 'Tan', rgb: '#D2B48C', hue: 34, sat: 44, lvl: 69],
                [name: 'Teal', rgb: '#008080', hue: 180, sat: 100, lvl: 25],
                [name: 'Thistle', rgb: '#D8BFD8', hue: 300, sat: 24, lvl: 80],
                [name: 'Tomato', rgb: '#FF6347', hue: 9, sat: 100, lvl: 64],
                [name: 'Turquoise', rgb: '#40E0D0', hue: 174, sat: 72, lvl: 56],
                [name: 'Violet', rgb: '#EE82EE', hue: 300, sat: 76, lvl: 72],
                [name: 'Warm White', rgb: '#DAF17E', hue: 72, sat: 20, lvl: 72],
                [name: 'Wheat', rgb: '#F5DEB3', hue: 39, sat: 77, lvl: 83],
                [name: 'White', rgb: '#FFFFFF', hue: 0, sat: 0, lvl: 100],
                [name: 'White Smoke', rgb: '#F5F5F5', hue: 0, sat: 0, lvl: 96],
                [name: 'Yellow', rgb: '#FFFF00', hue: 60, sat: 100, lvl: 50],
                [name: 'Yellow Green', rgb: '#9ACD32', hue: 80, sat: 61, lvl: 50],
        ]

@Field Map msgTypes = [
        DEVICE   : [
                GET_SERVICE        : [type: 2, descriptor: ''],
                STATE_SERVICE      : [type: 3, descriptor: 'service:b;port:i'],
                GET_HOST_INFO      : [type: 12, descriptor: ''],
                STATE_HOST_INFO    : [type: 13, descriptor: 'signal:i;tx:i;rx:i,reservedHost:w'],
                GET_HOST_FIRMWARE  : [type: 14, descriptor: ''],
                STATE_HOST_FIRMWARE: [type: 15, descriptor: 'build:l;reservedFirmware:l;version:i'],
                GET_WIFI_INFO      : [type: 16, descriptor: ''],
                STATE_WIFI_INFO    : [type: 17, descriptor: 'signal:i;tx:i;rx:i,reservedWifi:w'],
                GET_WIFI_FIRMWARE  : [type: 18, descriptor: ''],
                STATE_WIFI_FIRMWARE: [type: 19, descriptor: 'build:l;reservedFirmware:l;version:i'],
                GET_POWER          : [type: 20, descriptor: ''],
                SET_POWER          : [type: 21, descriptor: 'level:w'],
                STATE_POWER        : [type: 22, descriptor: 'level:w'],
                GET_LABEL          : [type: 23, descriptor: ''],
                SET_LABEL          : [type: 24, descriptor: 'label:t32'],
                STATE_LABEL        : [type: 25, descriptor: 'label:t32'],
                GET_VERSION        : [type: 32, descriptor: ''],
                STATE_VERSION      : [type: 33, descriptor: 'vendor:i;product:i;version:i'],
                GET_INFO           : [type: 34, descriptor: ''],
                STATE_INFO         : [type: 35, descriptor: 'time:l;uptime:l;downtime:l'],
                ACKNOWLEDGEMENT    : [type: 45, descriptor: ''],
                GET_LOCATION       : [type: 48, descriptor: ''],
                SET_LOCATION       : [type: 49, descriptor: 'location:ba16;label:t32;updated_at:l'],
                STATE_LOCATION     : [type: 50, descriptor: 'location:ba16;label:t32;updated_at:l'],
                GET_GROUP          : [type: 51, descriptor: ''],
                SET_GROUP          : [type: 52, descriptor: 'group:ba16;label:t32;updated_at:l'],
                STATE_GROUP        : [type: 53, descriptor: 'group:ba16;label:t32;updated_at:l'],
                ECHO_REQUEST       : [type: 58, descriptor: 'payload:ba64'],
                ECHO_RESPONSE      : [type: 59, descriptor: 'payload:ba64'],
        ],
        LIGHT    : [
                GET_STATE            : [type: 101, descriptor: ''],
                SET_COLOR            : [type: 102, descriptor: "reservedColor:b;color:h;duration:i"],
                SET_WAVEFORM         : [type: 103, descriptor: "reservedWaveform:b;transient:b;color:h;period:i;cycles:i;skew_ratio:w;waveform:b"],
                SET_WAVEFORM_OPTIONAL: [type: 119, descriptor: "reservedWaveform:b;transient:b;color:h;period:i;cycles:i;skew_ratio:w;waveform:b;setColor:h"],
                STATE                : [type: 107, descriptor: "color:h;reserved1State:w;power:w;label:t32;reserved2state:l"],
                GET_POWER            : [type: 116, descriptor: ''],
                SET_POWER            : [type: 117, descriptor: 'level:w;duration:i'],
                STATE_POWER          : [type: 118, descriptor: 'level:w'],
                GET_INFRARED         : [type: 120, descriptor: ''],
                STATE_INFRARED       : [type: 121, descriptor: 'brightness:w'],
                SET_INFRARED         : [type: 122, descriptor: 'brightness:w'],
        ],
        MULTIZONE: [
                SET_COLOR_ZONES: [type: 501, descriptor: "startIndex:b;endIndex:b;color:h;duration:i;apply:b"],
                GET_COLOR_ZONES: [type: 502, descriptor: 'startIndex:b;endIndex:b'],
                STATE_ZONE     : [type: 503, descriptor: "count:b;index:b;color:h"],
                STATE_MULTIZONE: [type: 506, descriptor: "count:b;index:b;colors:ha8"]
        ]
]
