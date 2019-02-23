import groovy.json.JsonSlurper
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

@Field Boolean wantBufferCaching = false
@Field Boolean wantDescriptorCaching = false

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
    page(name: 'mainPage'/*, nextPage: 'Discovery'*/)
    page(name: 'discoveryPage')
    page(name: 'namedColorsPage')
    page(name: 'testBedPage')
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "Options", install: true, uninstall: true) {
        section {
            input 'interCommandPause', 'number', defaultValue: 50, title: 'Time between commands (milliseconds)', submitOnChange: true
            input 'maxPasses', 'number', title: 'Maximum number of passes', defaultValue: 2, submitOnChange: true
            input 'refreshInterval', 'number', title: 'Discovery page refresh interval (seconds).<br><strong>WARNING</strong>: high refresh rates may interfere with discovery.', defaultValue: 10, submitOnChange: true
            input 'savePreferences', 'button', title: 'Save', submitOnChange: true
        }
        discoveryPageLink()
        colorsPageLink()
        testBedPageLink()
        includeStyles()
    }
}

def mainPageLink() {
    section {
        href(
                name: 'Main page',
                page: 'mainPage',
                description: 'Back to main page'
        )
    }
}

def discoveryPage() {
    dynamicPage(name: 'discoveryPage', title: 'Discovery', refreshInterval: refreshInterval()) {
        section {
            input 'discoverBtn', 'button', title: 'Discover devices'
            paragraph 'If not all of your devices are discovered the first time around, try the <strong>Discover only new devices</strong> button below'
            paragraph(
                    null == atomicState.scanPass ?
                            '' :
                            (
                                    ('DONE' == atomicState.scanPass) ?
                                            'Scanning complete' :
                                            "Scanning your network for devices, pass ${atomicState.scanPass} of ${maxScanPasses()}"
                            )

            )
            paragraph(
                    discoveryTextKnownDevices()
            )
            input 'discoverNewBtn', 'button', title: 'Discover only new devices'
            input 'clearCachesBtn', 'button', title: 'Clear caches'

        }
        mainPageLink()
        colorsPageLink()
        includeStyles()
    }
}

def namedColorsPage() {
    dynamicPage(name: 'namedColorsPage', title: 'Named Colors') {
        mainPageLink()
        section {
            paragraph(colorListHTML())
        }
        discoveryPageLink()
        includeStyles()
    }
}

def testBedPage() {
    dynamicPage(name: 'testBedPage', title: 'Testing stuff') {
        section
        input 'colors', 'text', title: 'Colors', defaultValue: '{"a": "Red", "b": "#FFDDAA", "c": "random", "d": {"h":20,"s":50,"v":100}}'
        input 'pattern', 'text', title: 'Descriptor', defaultValue: 'a:1,b:2,c:3,d:1'
        input 'testBtn', 'button', title: 'Test pattern'
    }
    mainPageLink()
    includeStyles()
}


private void includeStyles() {
    section {
        paragraph(styles())
    }
}

private discoveryPageLink() {
    section {
        href(
                name: 'Discovery',
                page: 'discoveryPage',
                description: 'Device discovery'
        )
    }
}

private testBedPageLink() {
    section {
        href(
                name: 'Testbed',
                page: 'testBedPage',
                description: 'For experimental stuff'
        )
    }
}

private colorsPageLink() {
    section {
        href(
                name: 'Named colors',
                page: 'namedColorsPage',
                description: 'Named colors'
        )
    }
}

private static String styles() {
    $/<style>
    h3.pre {
        background: #81BC00;
        font-size: larger;
        font-weight: bolder
    }
    h4.pre {
        background: #81BC00;
        font-size: larger
    }
    
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
    
    button span.state-incomplete-text {
        display: block 
    }
    
    button span {
        display: none
    }
    
    button br {
        display: none
    }    
    
</style>/$
}

String colorListHTML() {
    builder = new StringBuilder()
    builder << '<table class="colorList">'
    colorList().each {
        builder << '''<style>
            table.colorList {
                table-layout: auto;
                width: 100%;
                font-size: smaller
            }
            
            td.colorName {
                width: fit-content
            }
            td.colorSwatch {
                width: 70%
            }
            </style>
        '''
        builder << '<tr>'
        builder << "<td class='colorName'>$it.name</td>"
        builder << "<td class='colorSwatch' style='background-color:$it.rgb'>&nbsp;</td>"
        builder << '</tr>'
    }
    builder << '</table>'
    builder.toString()
}

private String discoveryTextKnownDevices() {
    if ((atomicState.numDevices == null) || (0 == atomicState.numDevices)) {
        return 'No devices known'
    }

    def deviceList = describeDevices() // don't inline this
    ("I have found <strong>${atomicState.numDevices}</strong> useable LIFX device"
            + ((1 == atomicState.numDevices) ? '' : 's')
            + ' so far:'
            + deviceList)

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
                    builder << (
                            device.error ?
                                    "<li class='device-error'>${device.label} (${device.error})</li>"
                                    : "<li class='device'>${getDeviceNameLink(device, ip)}</li>"
                    )
            }

            builder << '</ul>'
    }
    builder << '</ul>'
    builder.toString()
}

private String getDeviceNameLink(device, ip) {
    def realDevice = getChildDevice(ip)
    "<a href='/device/edit/${realDevice?.getId()}', target='_blank'>$device.label</a>"
}

Integer interCommandPauseMilliseconds(int pass = 1) {
    (settings.interCommandPause ?: 50) + 10 * (pass - 1)
}


Integer maxScanPasses() {
    settings.maxPasses ?: 2
}

Integer refreshInterval() {
    settings.refreshInterval ?: 10
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
    updateKnownDevices()
}

private void updateKnownDevices() {
    def knownDevices = knownDeviceLabels()
    atomicState.numDevices = knownDevices.size()
}


def appButtonHandler(btn) {
    log.debug "appButtonHandler called with ${btn}"
    if (btn == "discoverBtn") {
        refresh()
    } else if (btn == 'discoverNewBtn') {
        discoverNew()
    } else if (btn == 'clearCachesBtn') {
        clearCachedDescriptors()
        clearDeviceDefinitions()
        clearBufferCache()
    } else if (btn == 'testBtn') {
        testColorMapBuilder()
    }
}

def testColorMapBuilder() {
    Map<String, Map> map = buildColorMaps(settings.colors)
    logDebug "Map is $map"
    def hsbkMaps = makeColorMaps map, settings.pattern as String
    logDebug "Big map is $hsbkMaps"
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
    def discoveryDevice = addChildDevice 'robheyes', 'LIFX Discovery', 'LIFX Discovery'
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
    clearKnownIps()
    updateKnownDevices()
}

// Common device commands
Map<String, List> deviceOnOff(String value, Boolean displayed) {
    def actions = makeActions()
    actions.commands << makeCommand('DEVICE.SET_POWER', [powerLevel: value == 'on' ? 65535 : 0])
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

Map<String, List> deviceSetIRLevel(device, Number level, Boolean displayed, duration = 0) {
    def actions = makeActions()
    actions.commands << makeCommand('LIGHT.SET_INFRARED', [irLevel: value == scaleUp100(level)])
    actions.events << [name: "IRLevel", value: level, displayed: displayed, data: [syncing: "false"]]
    actions
}

Map<String, List> deviceSetLevel(device, Number level, Boolean displayed, duration = 0) {
    if ((level <= 0 || null == level) && 0 == duration) {
        return deviceOnOff('off', displayed)
    }
    if (level > 100) {
        level = 100
    }
    def hsbkMap = getCurrentHSBK(device)
    if (hsbkMap.saturation == 0) {
        hsbkMap.brightness = scaleUp100 level
        hsbkMap.hue = 0
        hsbkMap.saturation = 0
        hsbkMap.duration = duration * 1000
    } else {
        hsbkMap = [
                hue       : scaleUp100(device.currentHue),
                saturation: scaleUp100(device.currentSaturation),
                brightness: scaleUp100(level),
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
                hue       : scaleUp(myColor.h, 360),
                saturation: scaleUp100(myColor.s),
                brightness: scaleUp100(myStateMap.level ?: myColor.v),
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
            List<Map> result = [[name: "level", value: scaleDown100(data.color.brightness), displayed: displayed]]
            if (device.hasCapability('Color Control')) {
                result.add([name: "hue", value: scaleDown100(data.color.hue), displayed: displayed])
                result.add([name: "saturation", value: scaleDown100(data.color.saturation), displayed: displayed])
            }
            if (device.hasCapability('Color Temperature')) {
                result.add([name: "colorTemperature", value: data.color.kelvin as Integer, displayed: displayed])
            }
            if (device.hasCapability('Switch')) {
                result.add([name: 'switch', value: (data.power == 65535) ? 'on' : 'off', displayed: displayed])
            }
            return result
        case messageTypes().LIGHT.STATE_INFRARED.type:
            def data = parsePayload 'LIGHT.STATE_INFRARED', header
            logDebug "IR Data: $data"
            return [[name: 'IRLevel', value: scaleDown100(data.irLevel), displayed: displayed]]
        case messageTypes().DEVICE.STATE_POWER.type:
            Map data = parsePayload 'DEVICE.STATE_POWER', header
            return [[name: "switch", value: (data.powerLevel as Integer == 0 ? "off" : "on"), displayed: displayed, data: [syncing: "false"]],]
        case messageTypes().LIGHT.STATE_POWER.type:
            Map data = parsePayload 'LIGHT.STATE_POWER', header
            logDebug "Data returned is $data"
            return [
                    [name: "switch", value: (data.powerLevel as Integer == 0 ? "off" : "on"), displayed: displayed, data: [syncing: "false"]],
                    [name: "level", value: (data.powerLevel as Integer == 0 ? 0 : 100), displayed: displayed, data: [syncing: "false"]]
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
        case messageTypes().DEVICE.STATE_LOCATION.type:
            def data = parsePayload 'DEVICE.STATE_LOCATION', parsed
            updateDeviceDefinition mac, [location: data.label]
            return [ip: ip, type: messageTypes().DEVICE.GET_LABEL.type as int]
        case messageTypes().DEVICE.STATE_WIFI_INFO.type:
            break
        case messageTypes().DEVICE.STATE_INFO.type:
            break
    }
}

private Map lookupColor(String color) {
    Map myColor
    logDebug "color $color"
    if (color == "random") {
        myColor = pickRandomColor()
        logDebug "myColor $myColor"
        log.info "Setting random color: ${myColor.name}"
    } else if (color?.startsWith('#')) {
        // convert rgb to hsv
        Map rgb = hexToColor color
        myColor = rgbToHSV rgb.r, rgb.g, rgb.b, 'high'
    } else {
        myColor = colorList().find { (it.name as String).equalsIgnoreCase(color) }
        if (null == myColor) {
            throw new RuntimeException("No color found for $color")
        }
    }
    myColor.name = color
    myColor
}

private Map pickRandomColor() {
    def colors = colorList()
    def tempRandom = Math.abs(new Random().nextInt() % colors.size())
    colors[tempRandom]
}

List<Map> colorList() {
    colorMap
}

Map buildColorMaps(String jsonString) {
    def slurper = new JsonSlurper()
    Map map = slurper.parseText jsonString
    Map<String, Map> result = [:]
    map.each {
        key, value ->
            result[key] = getScaledColorMap transformColorValue(value)
    }
    result
}

Map transformColorValue(String value) {
    transformColorValue(lookupColor(value))
}

Map transformColorValue(Map hsv) {
    [hue: hsv.h, saturation: hsv.s, brightness: hsv.v]
}

List<Map> makeColorMaps(Map<String, Map> namedColors, String descriptor) {
    List<Map> result = []
    logDebug "descriptor is $descriptor"
    def darkPixel = [hue: 0, saturation: 0, brightness: 0, kelvin: 0]
    descriptor.findAll(~/(\w+):(\d+)/) {
        section ->
            String name = section[1]
            Integer count = section[2].toInteger()
            def color = namedColors[name]
            1.upto(count) {
                result << color ?: darkPixel
            }
    }
    result
}

private static Map<String, List> deviceSetHSBKAndPower(duration, Map<String, Number> hsbkMap, boolean displayed, String power = 'on') {
    def actions = makeActions()

    if (null != power) {
//        logDebug "power change required $power"
        def powerLevel = 'on' == power ? 65535 : 0
        actions.commands << makeCommand('LIGHT.SET_POWER', [powerLevel: powerLevel, duration: duration])
        actions.events << [name: "switch", value: power, displayed: displayed, data: [syncing: "false"]]
    }

    if (hsbkMap) {
//        logDebug "color change required"
        actions.commands << makeCommand('LIGHT.SET_COLOR', [color: hsbkMap])
        actions.events = actions.events + makeColorMapEvents(hsbkMap, displayed)
    }

    actions
}

private static List makeColorMapEvents(Map hsbkMap, Boolean displayed) {
    List<Map> colorMap = [
            [name: 'hue', value: scaleDown100(hsbkMap.hue), displayed: displayed],
            [name: 'saturation', value: scaleDown100(hsbkMap.saturation), displayed: displayed],
            [name: 'level', value: scaleDown100(hsbkMap.brightness), displayed: displayed],
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
            brightness: scaleUp100(colorMap.level ?: colorMap.brightness) as Integer,
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
            brightness: scaleUp(theDevice.currentValue('level') as Long, 100),
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
    Float result = ((value * maxValue) / 65535)
    result.round(2)
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
    List<Map> headerDescriptor = getDescriptor 'size:w,misc:w,source:i,target:ba8,frame_reserved:ba6,flags:b,sequence:b,protocol_reserved:ba8,type:w,protocol_reserved2:w'
    parseBytes(headerDescriptor, (hubitat.helper.HexUtils.hexStringToIntArray(deviceParams.payload) as List<Long>).each {
        it & 0xff
    })
}

void createDeviceDefinition(Map parsed, String ip, String mac) {
    List<Map> stateVersionDescriptor = getDescriptor 'vendor:i,product:i,version:i'
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

    isDeviceComplete(device) ? makeRealDevice(device) : saveDeviceDefinition(device)
}

List knownDeviceLabels() {
    getKnownIps().values().each { it.label }.asList()
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

Map<String, Map<String, Map>> messageTypes() {
    return msgTypes
}

private static Map deviceVersion(Map device) {
    switch (device.product) {
        case 1:
            return [
                    name      : 'Original 1000',
                    deviceName: 'LIFX Color',
                    features  : [
                            color            : true,
                            infrared         : false,
                            multizone        : false,
                            temperature_range: [min: 2500, max: 9000],
                            chain            : false
                    ]
            ]
        case 3:
            return [
                    name      : 'Color 650',
                    deviceName: 'LIFX Color',
                    features  : [
                            color            : true,
                            infrared         : false,
                            multizone        : false,
                            temperature_range: [min: 2500, max: 9000],
                            chain            : false
                    ]
            ]
        case 10:
            return [
                    name      : 'White 800 (Low Voltage)',
                    deviceName: 'LIFX White',
                    features  : [
                            color            : false,
                            infrared         : false,
                            multizone        : false,
                            temperature_range: [min: 2700, max: 6500],
                            chain            : false
                    ]
            ]
        case 11:
            return [
                    name      : 'White 800 (High Voltage)',
                    deviceName: 'LIFX White',
                    features  : [
                            color            : false,
                            infrared         : false,
                            multizone        : false,
                            temperature_range: [min: 2700, max: 6500],
                            chain            : false
                    ]
            ]
        case 18:
            return [
                    name      : 'White 900 BR30 (Low Voltage)',
                    deviceName: 'LIFX White',
                    features  : [
                            color            : false,
                            infrared         : false,
                            multizone        : false,
                            temperature_range: [min: 2700, max: 6500],
                            chain            : false
                    ]
            ]
        case 20:
            return [
                    name      : 'Color 1000 BR30',
                    deviceName: 'LIFX Color',
                    features  : [
                            color            : true,
                            infrared         : false,
                            multizone        : false,
                            temperature_range: [min: 2500, max: 9000],
                            chain            : false
                    ]
            ]
        case 22:
            return [
                    name      : 'Color 1000',
                    deviceName: 'LIFX Color',
                    features  : [
                            color            : true,
                            infrared         : false,
                            multizone        : false,
                            temperature_range: [min: 2500, max: 9000],
                            chain            : false
                    ]
            ]
        case 27:
        case 43:
            return [
                    name      : 'LIFX A19',
                    deviceName: 'LIFX Color',
                    features  : [
                            color            : true,
                            infrared         : false,
                            multizone        : false,
                            temperature_range: [min: 2500, max: 9000],
                            chain            : false
                    ]
            ]
        case 28:
        case 44:
            return [
                    name      : 'LIFX BR30',
                    deviceName: 'LIFX Color',
                    features  : [
                            color            : true,
                            infrared         : false,
                            multizone        : false,
                            temperature_range: [min: 2500, max: 9000],
                            chain            : false
                    ]
            ]
        case 29:
        case 45:
            return [
                    name      : 'LIFX+ A19',
                    deviceName: 'LIFXPlus Color',
                    features  : [
                            color            : true,
                            infrared         : true,
                            multizone        : false,
                            temperature_range: [min: 2500, max: 9000],
                            chain            : false
                    ]
            ]
        case 30:
        case 46:
            return [
                    name      : 'LIFX+ BR30',
                    deviceName: 'LIFXPlus Color',
                    features  : [
                            color            : true,
                            infrared         : true,
                            multizone        : false,
                            temperature_range: [min: 2500, max: 9000],
                            chain            : false
                    ]
            ]
        case 31:
            return [
                    name      : 'LIFX Z',
                    deviceName: 'LIFX Multizone',
                    features  : [
                            color            : true,
                            infrared         : false,
                            multizone        : true,
                            temperature_range: [min: 2500, max: 9000],
                            chain            : false
                    ]
            ]
        case 32:
            return [
                    name      : 'LIFX Z 2',
                    deviceName: 'LIFX Multizone',
                    features  : [
                            color              : true,
                            infrared           : false,
                            multizone          : true,
                            temperature_range  : [min: 2500, max: 9000],
                            chain              : false,
                            min_ext_mz_firmware: 1532997580
                    ]
            ]
        case 36:
        case 37:
            return [
                    name      : 'LIFX Downlight',
                    deviceName: 'LIFX Color',
                    features  : [
                            color            : true,
                            infrared         : false,
                            multizone        : false,
                            temperature_range: [min: 2500, max: 9000],
                            chain            : false
                    ]
            ]
        case 38:
            return [
                    name      : 'LIFX Beam',
                    deviceName: 'LIFX Multizone',
                    features  : [
                            color              : true,
                            infrared           : false,
                            multizone          : true,
                            temperature_range  : [min: 2500, max: 9000],
                            chain              : false,
                            min_ext_mz_firmware: 1532997580
                    ]
            ]

        case 49:
            return [
                    name      : 'LIFX Mini',
                    deviceName: 'LIFX Color',
                    features  : [
                            color            : true,
                            infrared         : false,
                            multizone        : false,
                            temperature_range: [min: 2500, max: 9000],
                            chain            : false
                    ]
            ]
        case 50:
        case 60:
            return [
                    name      : 'LIFX Mini Day and Dusk',
                    deviceName: 'LIFX Day and Dusk',
                    features  : [
                            color            : false,
                            infrared         : false,
                            multizone        : false,
                            temperature_range: [min: 1500, max: 4000],
                            chain            : false
                    ]
            ]
        case 51:
        case 61:
            return [
                    name      : 'LIFX Mini White',
                    deviceName: 'LIFX White Mono',
                    features  : [
                            color            : false,
                            infrared         : false,
                            multizone        : false,
                            temperature_range: [min: 2700, max: 2700],
                            chain            : false
                    ]
            ]
        case 52:
            return [
                    name      : 'LIFX GU10',
                    deviceName: 'LIFX Color',
                    features  : [
                            color            : true,
                            infrared         : false,
                            multizone        : false,
                            temperature_range: [min: 2500, max: 9000],
                            chain            : false
                    ]
            ]
        case 55:
            return [
                    name      : 'LIFX Tile',
                    deviceName: 'LIFX Tile',
                    features  : [
                            color            : true,
                            infrared         : false,
                            multizone        : false,
                            temperature_range: [min: 2500, max: 9000],
                            chain            : true
                    ]
            ]
        case 56:
            return [
                    name      : 'LIFX Beam',
                    deviceName: 'LIFX Multizone',
                    features  : [
                            color            : true,
                            infrared         : false,
                            multizone        : true,
                            temperature_range: [min: 2500, max: 9000],
                            chain            : false,
                    ]
            ]
        case 59:
            return [
                    name      : 'LIFX Mini Color',
                    deviceName: 'LIFX Color',
                    features  : [
                            color            : true,
                            infrared         : false,
                            multizone        : false,
                            temperature_range: [min: 2500, max: 9000],
                            chain            : false
                    ]
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
    return [h: h, s: s, v: v]
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
    parseBytes getDescriptor(descriptor), bytes
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
        assert (data.size() == totalLength)
        offset = nextOffset

        if (item.isArray) {
            def subMap = [:]
            for (int i = 0; i < item.count; i++) {
                processSegment subMap, data, item, i
            }
            result.put item.name, subMap
        } else {
            processSegment result, data, item, item.name
        }
    }
    if (offset < bytes.size()) {
        result.put 'remainder', bytes[offset..-1]
    }
    return result
}

private void processSegment(Map result, List<Long> data, Map item, name) {
    switch (item.kind) {
        case 'B':
        case 'W':
        case 'I':
        case 'L':
            data = data.reverse()
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
            Map color = parseBytes 'hue:w;saturation:w;brightness:w,kelvin:w', data
            result.put name, color
            break
        default:
            throw new RuntimeException("Unexpected item kind '$kind'")
    }
}

private void storeValue(Map result, List<Long> data, numBytes, index, boolean trace = false) {
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

    result.put index, theValue
}

List makePayload(String device, String type, Map payload) {
    def descriptor = getDescriptor lookupDescriptorForDeviceAndType(device, type)
    def result = []
    descriptor.each {
        Map item ->
            if ('H' == item.kind) {
                add result, (payload.color['hue'] ?: 0) as short
                add result, (payload.color['saturation'] ?: 0) as short
                add result, (payload.color['brightness'] ?: 0) as short
                add result, (payload.color['kelvin'] ?: 0) as short
                return
            }
            def value = payload[item.name] ?: 0
            //TODO possibly extend this to the other types A,S & B
            switch (item.size as int) {
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
    List<Map> candidate

    if (wantDescriptorCaching) {
        def cachedDescriptors = atomicState.cachedDescriptors
        if (null == cachedDescriptors) {
            cachedDescriptors = new HashMap<String, List<Map>>()
        }

        candidate = cachedDescriptors.get(desc)
    }
    if (!candidate) {
        candidate = makeDescriptor desc
        if (wantDescriptorCaching) {
            cachedDescriptors[desc] = (candidate)
            atomicState.cachedDescriptors = cachedDescriptors
        }
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
    if (wantBufferCaching) {
        def hashKey = targetAddress.toString() + messageType.toString() + payload.toString() + (ackRequired ? 'T' : 'F') + (responseRequired ? 'T' : 'F')
        aBuffer = lookupBuffer(hashKey)
        if (aBuffer) {
            add buffer, aBuffer as byte[]

            put buffer, 23, lastSequence as byte // store the sequence
            return lastSequence
        }
    }
    createFrame buffer, targetAddress.every { it == 0 }
    createFrameAddress buffer, targetAddress, ackRequired, responseRequired, lastSequence
    createProtocolHeader buffer, messageType as short
    createPayload buffer, payload as byte[]

    put buffer, 0, buffer.size() as short

    if (wantBufferCaching) {
        storeBuffer hashKey, buffer
    }
    return lastSequence
}

private void clearBufferCache() {
    atomicState.bufferCache = [:]
}

private List lookupBuffer(String hashKey) {
    def cache = getBufferCache()
    cache[hashKey]
}

private Map<String, List> getBufferCache() {
    atomicState.bufferCache ?: [:]
}

private void storeBuffer(String hashKey, List buffer) {
    def cache = getBufferCache()
    cache[hashKey] = buffer
    atomicState.bufferCache = cache
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

private static void add(List buffer, List other) {
    for (value in other) {
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
                [name: 'Alice Blue', rgb: '#F0F8FF', h: 208, s: 100, v: 97],
                [name: 'Antique White', rgb: '#FAEBD7', h: 34, s: 78, v: 91],
                [name: 'Aqua', rgb: '#00FFFF', h: 180, s: 100, v: 50],
                [name: 'Aquamarine', rgb: '#7FFFD4', h: 160, s: 100, v: 75],
                [name: 'Azure', rgb: '#F0FFFF', h: 180, s: 100, v: 97],
                [name: 'Beige', rgb: '#F5F5DC', h: 60, s: 56, v: 91],
                [name: 'Bisque', rgb: '#FFE4C4', h: 33, s: 100, v: 88],
                [name: 'Blanched Almond', rgb: '#FFEBCD', h: 36, s: 100, v: 90],
                [name: 'Blue', rgb: '#0000FF', h: 240, s: 100, v: 50],
                [name: 'Blue Violet', rgb: '#8A2BE2', h: 271, s: 76, v: 53],
                [name: 'Brown', rgb: '#A52A2A', h: 0, s: 59, v: 41],
                [name: 'Burly Wood', rgb: '#DEB887', h: 34, s: 57, v: 70],
                [name: 'Cadet Blue', rgb: '#5F9EA0', h: 182, s: 25, v: 50],
                [name: 'Chartreuse', rgb: '#7FFF00', h: 90, s: 100, v: 50],
                [name: 'Chocolate', rgb: '#D2691E', h: 25, s: 75, v: 47],
                [name: 'Cool White', rgb: '#F3F6F7', h: 187, s: 19, v: 96],
                [name: 'Coral', rgb: '#FF7F50', h: 16, s: 100, v: 66],
                [name: 'Corn Flower Blue', rgb: '#6495ED', h: 219, s: 79, v: 66],
                [name: 'Corn Silk', rgb: '#FFF8DC', h: 48, s: 100, v: 93],
                [name: 'Crimson', rgb: '#DC143C', h: 348, s: 83, v: 58],
                [name: 'Cyan', rgb: '#00FFFF', h: 180, s: 100, v: 50],
                [name: 'Dark Blue', rgb: '#00008B', h: 240, s: 100, v: 27],
                [name: 'Dark Cyan', rgb: '#008B8B', h: 180, s: 100, v: 27],
                [name: 'Dark Golden Rod', rgb: '#B8860B', h: 43, s: 89, v: 38],
                [name: 'Dark Gray', rgb: '#A9A9A9', h: 0, s: 0, v: 66],
                [name: 'Dark Green', rgb: '#006400', h: 120, s: 100, v: 20],
                [name: 'Dark Khaki', rgb: '#BDB76B', h: 56, s: 38, v: 58],
                [name: 'Dark Magenta', rgb: '#8B008B', h: 300, s: 100, v: 27],
                [name: 'Dark Olive Green', rgb: '#556B2F', h: 82, s: 39, v: 30],
                [name: 'Dark Orange', rgb: '#FF8C00', h: 33, s: 100, v: 50],
                [name: 'Dark Orchid', rgb: '#9932CC', h: 280, s: 61, v: 50],
                [name: 'Dark Red', rgb: '#8B0000', h: 0, s: 100, v: 27],
                [name: 'Dark Salmon', rgb: '#E9967A', h: 15, s: 72, v: 70],
                [name: 'Dark Sea Green', rgb: '#8FBC8F', h: 120, s: 25, v: 65],
                [name: 'Dark Slate Blue', rgb: '#483D8B', h: 248, s: 39, v: 39],
                [name: 'Dark Slate Gray', rgb: '#2F4F4F', h: 180, s: 25, v: 25],
                [name: 'Dark Turquoise', rgb: '#00CED1', h: 181, s: 100, v: 41],
                [name: 'Dark Violet', rgb: '#9400D3', h: 282, s: 100, v: 41],
                [name: 'Daylight White', rgb: '#CEF4FD', h: 191, s: 9, v: 90],
                [name: 'Deep Pink', rgb: '#FF1493', h: 328, s: 100, v: 54],
                [name: 'Deep Sky Blue', rgb: '#00BFFF', h: 195, s: 100, v: 50],
                [name: 'Dim Gray', rgb: '#696969', h: 0, s: 0, v: 41],
                [name: 'Dodger Blue', rgb: '#1E90FF', h: 210, s: 100, v: 56],
                [name: 'Fire Brick', rgb: '#B22222', h: 0, s: 68, v: 42],
                [name: 'Floral White', rgb: '#FFFAF0', h: 40, s: 100, v: 97],
                [name: 'Forest Green', rgb: '#228B22', h: 120, s: 61, v: 34],
                [name: 'Fuchsia', rgb: '#FF00FF', h: 300, s: 100, v: 50],
                [name: 'Gainsboro', rgb: '#DCDCDC', h: 0, s: 0, v: 86],
                [name: 'Ghost White', rgb: '#F8F8FF', h: 240, s: 100, v: 99],
                [name: 'Gold', rgb: '#FFD700', h: 51, s: 100, v: 50],
                [name: 'Golden Rod', rgb: '#DAA520', h: 43, s: 74, v: 49],
                [name: 'Gray', rgb: '#808080', h: 0, s: 0, v: 50],
                [name: 'Green', rgb: '#008000', h: 120, s: 100, v: 25],
                [name: 'Green Yellow', rgb: '#ADFF2F', h: 84, s: 100, v: 59],
                [name: 'Honeydew', rgb: '#F0FFF0', h: 120, s: 100, v: 97],
                [name: 'Hot Pink', rgb: '#FF69B4', h: 330, s: 100, v: 71],
                [name: 'Indian Red', rgb: '#CD5C5C', h: 0, s: 53, v: 58],
                [name: 'Indigo', rgb: '#4B0082', h: 275, s: 100, v: 25],
                [name: 'Ivory', rgb: '#FFFFF0', h: 60, s: 100, v: 97],
                [name: 'Khaki', rgb: '#F0E68C', h: 54, s: 77, v: 75],
                [name: 'Lavender', rgb: '#E6E6FA', h: 240, s: 67, v: 94],
                [name: 'Lavender Blush', rgb: '#FFF0F5', h: 340, s: 100, v: 97],
                [name: 'Lawn Green', rgb: '#7CFC00', h: 90, s: 100, v: 49],
                [name: 'Lemon Chiffon', rgb: '#FFFACD', h: 54, s: 100, v: 90],
                [name: 'Light Blue', rgb: '#ADD8E6', h: 195, s: 53, v: 79],
                [name: 'Light Coral', rgb: '#F08080', h: 0, s: 79, v: 72],
                [name: 'Light Cyan', rgb: '#E0FFFF', h: 180, s: 100, v: 94],
                [name: 'Light Golden Rod Yellow', rgb: '#FAFAD2', h: 60, s: 80, v: 90],
                [name: 'Light Gray', rgb: '#D3D3D3', h: 0, s: 0, v: 83],
                [name: 'Light Green', rgb: '#90EE90', h: 120, s: 73, v: 75],
                [name: 'Light Pink', rgb: '#FFB6C1', h: 351, s: 100, v: 86],
                [name: 'Light Salmon', rgb: '#FFA07A', h: 17, s: 100, v: 74],
                [name: 'Light Sea Green', rgb: '#20B2AA', h: 177, s: 70, v: 41],
                [name: 'Light Sky Blue', rgb: '#87CEFA', h: 203, s: 92, v: 75],
                [name: 'Light Slate Gray', rgb: '#778899', h: 210, s: 14, v: 53],
                [name: 'Light Steel Blue', rgb: '#B0C4DE', h: 214, s: 41, v: 78],
                [name: 'Light Yellow', rgb: '#FFFFE0', h: 60, s: 100, v: 94],
                [name: 'Lime', rgb: '#00FF00', h: 120, s: 100, v: 50],
                [name: 'Lime Green', rgb: '#32CD32', h: 120, s: 61, v: 50],
                [name: 'Linen', rgb: '#FAF0E6', h: 30, s: 67, v: 94],
                [name: 'Maroon', rgb: '#800000', h: 0, s: 100, v: 25],
                [name: 'Medium Aquamarine', rgb: '#66CDAA', h: 160, s: 51, v: 60],
                [name: 'Medium Blue', rgb: '#0000CD', h: 240, s: 100, v: 40],
                [name: 'Medium Orchid', rgb: '#BA55D3', h: 288, s: 59, v: 58],
                [name: 'Medium Purple', rgb: '#9370DB', h: 260, s: 60, v: 65],
                [name: 'Medium Sea Green', rgb: '#3CB371', h: 147, s: 50, v: 47],
                [name: 'Medium Slate Blue', rgb: '#7B68EE', h: 249, s: 80, v: 67],
                [name: 'Medium Spring Green', rgb: '#00FA9A', h: 157, s: 100, v: 49],
                [name: 'Medium Turquoise', rgb: '#48D1CC', h: 178, s: 60, v: 55],
                [name: 'Medium Violet Red', rgb: '#C71585', h: 322, s: 81, v: 43],
                [name: 'Midnight Blue', rgb: '#191970', h: 240, s: 64, v: 27],
                [name: 'Mint Cream', rgb: '#F5FFFA', h: 150, s: 100, v: 98],
                [name: 'Misty Rose', rgb: '#FFE4E1', h: 6, s: 100, v: 94],
                [name: 'Moccasin', rgb: '#FFE4B5', h: 38, s: 100, v: 85],
                [name: 'Navajo White', rgb: '#FFDEAD', h: 36, s: 100, v: 84],
                [name: 'Navy', rgb: '#000080', h: 240, s: 100, v: 25],
                [name: 'Old Lace', rgb: '#FDF5E6', h: 39, s: 85, v: 95],
                [name: 'Olive', rgb: '#808000', h: 60, s: 100, v: 25],
                [name: 'Olive Drab', rgb: '#6B8E23', h: 80, s: 60, v: 35],
                [name: 'Orange', rgb: '#FFA500', h: 39, s: 100, v: 50],
                [name: 'Orange Red', rgb: '#FF4500', h: 16, s: 100, v: 50],
                [name: 'Orchid', rgb: '#DA70D6', h: 302, s: 59, v: 65],
                [name: 'Pale Golden Rod', rgb: '#EEE8AA', h: 55, s: 67, v: 80],
                [name: 'Pale Green', rgb: '#98FB98', h: 120, s: 93, v: 79],
                [name: 'Pale Turquoise', rgb: '#AFEEEE', h: 180, s: 65, v: 81],
                [name: 'Pale Violet Red', rgb: '#DB7093', h: 340, s: 60, v: 65],
                [name: 'Papaya Whip', rgb: '#FFEFD5', h: 37, s: 100, v: 92],
                [name: 'Peach Puff', rgb: '#FFDAB9', h: 28, s: 100, v: 86],
                [name: 'Peru', rgb: '#CD853F', h: 30, s: 59, v: 53],
                [name: 'Pink', rgb: '#FFC0CB', h: 350, s: 100, v: 88],
                [name: 'Plum', rgb: '#DDA0DD', h: 300, s: 47, v: 75],
                [name: 'Powder Blue', rgb: '#B0E0E6', h: 187, s: 52, v: 80],
                [name: 'Purple', rgb: '#800080', h: 300, s: 100, v: 25],
                [name: 'Red', rgb: '#FF0000', h: 0, s: 100, v: 50],
                [name: 'Rosy Brown', rgb: '#BC8F8F', h: 0, s: 25, v: 65],
                [name: 'Royal Blue', rgb: '#4169E1', h: 225, s: 73, v: 57],
                [name: 'Saddle Brown', rgb: '#8B4513', h: 25, s: 76, v: 31],
                [name: 'Salmon', rgb: '#FA8072', h: 6, s: 93, v: 71],
                [name: 'Sandy Brown', rgb: '#F4A460', h: 28, s: 87, v: 67],
                [name: 'Sea Green', rgb: '#2E8B57', h: 146, s: 50, v: 36],
                [name: 'Sea Shell', rgb: '#FFF5EE', h: 25, s: 100, v: 97],
                [name: 'Sienna', rgb: '#A0522D', h: 19, s: 56, v: 40],
                [name: 'Silver', rgb: '#C0C0C0', h: 0, s: 0, v: 75],
                [name: 'Sky Blue', rgb: '#87CEEB', h: 197, s: 71, v: 73],
                [name: 'Slate Blue', rgb: '#6A5ACD', h: 248, s: 53, v: 58],
                [name: 'Slate Gray', rgb: '#708090', h: 210, s: 13, v: 50],
                [name: 'Snow', rgb: '#FFFAFA', h: 0, s: 100, v: 99],
                [name: 'Soft White', rgb: '#B6DA7C', h: 83, s: 44, v: 67],
                [name: 'Spring Green', rgb: '#00FF7F', h: 150, s: 100, v: 50],
                [name: 'Steel Blue', rgb: '#4682B4', h: 207, s: 44, v: 49],
                [name: 'Tan', rgb: '#D2B48C', h: 34, s: 44, v: 69],
                [name: 'Teal', rgb: '#008080', h: 180, s: 100, v: 25],
                [name: 'Thistle', rgb: '#D8BFD8', h: 300, s: 24, v: 80],
                [name: 'Tomato', rgb: '#FF6347', h: 9, s: 100, v: 64],
                [name: 'Turquoise', rgb: '#40E0D0', h: 174, s: 72, v: 56],
                [name: 'Violet', rgb: '#EE82EE', h: 300, s: 76, v: 72],
                [name: 'Warm White', rgb: '#DAF17E', h: 72, s: 20, v: 72],
                [name: 'Wheat', rgb: '#F5DEB3', h: 39, s: 77, v: 83],
                [name: 'White', rgb: '#FFFFFF', h: 0, s: 0, v: 100],
                [name: 'White Smoke', rgb: '#F5F5F5', h: 0, s: 0, v: 96],
                [name: 'Yellow', rgb: '#FFFF00', h: 60, s: 100, v: 50],
                [name: 'Yellow Green', rgb: '#9ACD32', h: 80, s: 61, v: 50],
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
                SET_POWER          : [type: 21, descriptor: 'powerLevel:w'],
                STATE_POWER        : [type: 22, descriptor: 'powerLevel:w'],
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
                SET_POWER            : [type: 117, descriptor: 'powerLevel:w;duration:i'],
                STATE_POWER          : [type: 118, descriptor: 'powerLevel:w'],
                GET_INFRARED         : [type: 120, descriptor: ''],
                STATE_INFRARED       : [type: 121, descriptor: 'irLevel:w'],
                SET_INFRARED         : [type: 122, descriptor: 'irLevel:w'],
        ],
        MULTIZONE: [
                SET_COLOR_ZONES           : [type: 501, descriptor: "startIndex:b;endIndex:b;color:h;duration:i;apply:b"],
                GET_COLOR_ZONES           : [type: 502, descriptor: 'startIndex:b;endIndex:b'],
                STATE_ZONE                : [type: 503, descriptor: "count:b;index:b;color:h"],
                STATE_MULTIZONE           : [type: 506, descriptor: "count:b;index:b;colors:ha8"],
                SET_EXTENDED_COLOR_ZONES  : [type: 510, descriptor: 'duration:i;apply:b;index:w;colorsCount:b;colors:ha82'],
                GET_EXTENDED_COLOR_ZONES  : [type: 511, descriptor: ''],
                STATE_EXTENDED_COLOR_ZONES: [type: 512, descriptor: 'index:w;count:w;colors_count:b;colors:ha82'],
        ]
]
