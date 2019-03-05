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

@Field Boolean wantBufferCaching = false // should probably remove this?
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
            input 'interCommandPause', 'number', defaultValue: 40, title: 'Time between commands (milliseconds)', submitOnChange: true
            input 'maxPasses', 'number', title: 'Maximum number of passes', defaultValue: 2, submitOnChange: true
            input 'refreshInterval', 'number', title: 'Discovery page refresh interval (seconds).<br><strong>WARNING</strong>: high refresh rates may interfere with discovery.', defaultValue: 6, submitOnChange: true
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
            paragraph "<strong>RECOMMENDATION</strong>: It is advisable to configure your router's DHCP settings to use fixed IP addresses for all LIFX devices"
            input 'discoverBtn', 'button', title: 'Discover devices'
            paragraph 'If you have added a new device, or not all of your devices are discovered the first time around, try the <strong>Discover only new devices</strong> button below'
            paragraph(
                    null == atomicState.scanPass ?
                            '' :
                            (
                                    ('DONE' == atomicState.scanPass) ?
                                            'Scanning complete' :
                                            "Scanning your network for devices <div class='meter'>" +
                                                    "<span style='width:${getProgressPercentage()}'><strong>${getProgressPercentage()}</strong></span>" +
                                                    "</div>"
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
            input 'sortOrder', 'enum', title: 'Sort by: ', options: ['0': 'Alphabetical', '1': 'By Hue', '2': 'By Level', '3': 'By RGB'], submitOnChange: true
            paragraph(
                    colorListHTML(settings.sortOrder)
            )
        }
        discoveryPageLink()
        includeStyles()
    }
}

def testBedPage() {
    dynamicPage(name: 'testBedPage', title: 'Testing stuff') {
        section {
            input 'colors', 'text', title: 'Colors', defaultValue: '{"a": "Red", "b": "#FFDDAA", "c": "random", "d": {"h":20,"s":50,"v":100}}'
            input 'colors2', 'color_map', title: 'Colors', defaultValue: '[a: [color: Red], b: [color:"#FFDDAA"], c: [color: random], d: [h:20,s:50,v:100]]'
            input 'pattern', 'text', title: 'Descriptor', defaultValue: 'a:1,b:2,c:3,d:1'
            input 'testBtn', 'button', title: 'Test pattern'
        }
        mainPageLink()
        includeStyles()
    }
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
    
/* Progress bar - modified from https://css-tricks.com/examples/ProgressBars/ */
    .meter {
        height: 20px;  /* Can be anything */
        position: relative;
        background: #D9ECB1;
        -moz-border-radius: 5px;
        -webkit-border-radius: 5px;
        border-radius: 5px;
        padding: 0px;
    }

    .meter > span {
          display: block;
          height: 100%;
          border-top-right-radius: 2px;
          border-bottom-right-radius: 2px;
          border-top-left-radius: 5px;
          border-bottom-left-radius: 5px;
          background-color: #81BC00;
          position: relative;
          overflow: hidden;
          text-align: center;
    }
</style>/$
}

String colorListHTML(String sortOrder) {
    logDebug("sort order is $sortOrder")
    builder = new StringBuilder()
    builder << '<table class="colorList">'
    colorList(sortOrder).each {
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
                                    : "<li class='device'>${getDeviceNameLink(device)}</li>"
                    )
            }

            builder << '</ul>'
    }
    builder << '</ul>'
    builder.toString()
}

private String getDeviceNameLink(device) {
    def realDevice = getChildDevice(device.ip)
    "<a href='/device/edit/${realDevice?.getId()}', target='_blank'>$device.label</a>"
}

Integer interCommandPauseMilliseconds(int pass = 1) {
    (settings.interCommandPause ?: 40) + 10 * (pass - 1)
}


Integer maxScanPasses() {
    settings.maxPasses ?: 2
}

Integer refreshInterval() {
    settings.refreshInterval ?: 6
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
    if (btn == "discoverBtn") {
        refresh()
    } else if (btn == 'discoverNewBtn') {
        discoverNew()
    } else if (btn == 'refreshExistingBtn') {
        refreshExisting()
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

    discovery('discovery')
}

def discoverNew() {
    removeDiscoveryDevice()
    discovery('discovery')
}

def refreshExisting() {
    removeDiscoveryDevice()

//    discovery('refresh')

}

String discoveryType() {
    return atomicState.discoveryType
}

private void discovery(String discoveryType) {
    atomicState.discoveryType = discoveryType
    atomicState.scanPass = null
    updateKnownDevices()
    clearDeviceDefinitions()
    atomicState.progressPercent = 0
    def discoveryDevice = addChildDevice 'robheyes', 'LIFX Discovery', 'LIFX Discovery'
    subscribe discoveryDevice, 'lifxdiscovery.complete', removeDiscoveryDevice
    subscribe discoveryDevice, 'progress', progress
}

/** DND event handler */
def progress(evt) {
    def percent = evt.getIntegerValue()
    atomicState.progressPercent = percent
}

def getProgressPercentage() {
    def percent = atomicState.progressPercent ?: 0
    "$percent%"
}

void removeDiscoveryDevice(evt) {
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
Map<String, List> deviceOnOff(String value, Boolean displayed, duration = 0) {
    def actions = makeActions()
    actions.commands << makeCommand('LIGHT.SET_POWER', [powerLevel: value == 'on' ? 65535 : 0, duration: duration * 1000])
    actions.events << [name: "switch", value: value, displayed: displayed, data: [syncing: "false"]]
    actions
}

Map<String, List> deviceSetColor(device, Map colorMap, Boolean displayed, duration = 0) {
    logDebug "Duration1: $duration"
    def hsbkMap = getCurrentHSBK device
    hsbkMap << getScaledColorMap(colorMap)
    hsbkMap.duration = 1000 * (colorMap.duration ?: duration)

    deviceSetHSBKAndPower(device, duration, hsbkMap, displayed)
}

Map<String, List> deviceSetHue(device, Number hue, Boolean displayed, duration = 0) {
    def hsbkMap = getCurrentHSBK device
    hsbkMap.hue = scaleUp100 hue
    hsbkMap.duration = 1000 * duration

    deviceSetHSBKAndPower(device, duration, hsbkMap, displayed)
}

Map<String, List> deviceSetSaturation(device, Number saturation, Boolean displayed, duration = 0) {
    def hsbkMap = getCurrentHSBK device
    hsbkMap.saturation = scaleUp100 saturation
    hsbkMap.duration = 1000 * duration

    deviceSetHSBKAndPower(device, duration, hsbkMap, displayed)
}

Map<String, List> deviceSetColorTemperature(device, Number temperature, Boolean displayed, duration = 0) {
    def hsbkMap = getCurrentHSBK device
    hsbkMap.saturation = 0
    hsbkMap.kelvin = temperature
    hsbkMap.duration = 1000 * duration

    deviceSetHSBKAndPower(device, duration, hsbkMap, displayed)
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

    deviceSetHSBKAndPower(device, duration, hsbkMap, displayed)
}

Map<String, List> deviceSetState(device, Map myStateMap, Boolean displayed, duration = 0) {
    def power = myStateMap.power
    def level = myStateMap.level ?: myStateMap.brightness
    def kelvin = myStateMap.kelvin ?: myStateMap.temperature
    String color = myStateMap.color ?: myStateMap.colour
    duration = (myStateMap.duration ?: duration) * 1000

    Map myColor
    myColor = (null == color) ? null : lookupColor(color)
    if (myColor) {
        Map realColor = [
                hue       : scaleUp(myColor.h ?: 0, 360),
                saturation: scaleUp100(myColor.s ?: 0),
                brightness: scaleUp100(level ?: (myColor.v ?: 50)),
                kelvin    : kelvin ?: device.currentColorTemperature,
                duration  : duration
        ]
        if (myColor.name) {
            realColor.name = myColor.name
        }
        deviceSetHSBKAndPower(device, duration, realColor, displayed, power)
    } else if (kelvin) {
        def realColor = [
                hue       : 0,
                saturation: 0,
                kelvin    : kelvin,
                brightness: scaleUp100(level ?: 100),
                duration  : duration
        ]
        deviceSetHSBKAndPower(device, duration, realColor, displayed, power)
    } else if (level) {
        deviceSetLevel(device, level, displayed, duration)
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
            return [[name: 'group', value: group]]
        case messageTypes().DEVICE.STATE_LOCATION.type:
            def data = parsePayload 'DEVICE.STATE_LOCATION', header
            String location = data.label
            return [[name: 'location', value: location]]
        case messageTypes().DEVICE.STATE_HOST_INFO.type:
            def data = parsePayload 'DEVICE.STATE_HOST_INFO', header
            logDebug("Wifi data $data")
            break
        case messageTypes().DEVICE.STATE_INFO.type:
            def data = parsePayload 'DEVICE.STATE_INFO', header
            break
        case messageTypes().LIGHT.STATE.type:
            def data = parsePayload 'LIGHT.STATE', header
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
//            logDebug "IR Data: $data"
            return [[name: 'IRLevel', value: scaleDown100(data.irLevel), displayed: displayed]]
        case messageTypes().DEVICE.STATE_POWER.type:
            Map data = parsePayload 'DEVICE.STATE_POWER', header
            return [[name: "switch", value: (data.powerLevel as Integer == 0 ? "off" : "on"), displayed: displayed, data: [syncing: "false"]],]
        case messageTypes().LIGHT.STATE_POWER.type:
            Map data = parsePayload 'LIGHT.STATE_POWER', header
            return [
                    [name: "switch", value: (data.powerLevel as Integer == 0 ? "off" : "on"), displayed: displayed, data: [syncing: "false"]],
                    [name: "level", value: (data.powerLevel as Integer == 0 ? 0 : 100), displayed: displayed, data: [syncing: "false"]]
            ]
        case messageTypes().DEVICE.ACKNOWLEDGEMENT.type:
            Byte sequence = header.sequence
            clearExpectedAckFor device, sequence
            return []
        default:
            logWarn "Unhandled response for ${header.type}"
            return []
    }
    return []
}

Map<String, List> discoveryParse(String description) {
    def actions = makeActions()
    Map deviceParams = parseDeviceParameters description
    String ip = convertIpLong(deviceParams.ip as String)
    Map parsed = parseHeader deviceParams

    final String mac = deviceParams.mac
    switch (parsed.type) {
        case messageTypes().DEVICE.STATE_VERSION.type:
            def existing = getDeviceDefinition mac
            if (!existing) {
                createDeviceDefinition parsed, ip, mac
                actions.commands << [ip: ip, type: messageTypes().DEVICE.GET_GROUP.type as int]
            }
            break
        case messageTypes().DEVICE.STATE_LABEL.type:
            def data = parsePayload 'DEVICE.STATE_LABEL', parsed
            def device = updateDeviceDefinition mac, ip, [label: data.label]
            if (device) {
                sendEvent ip, [name: 'label', value: data.label]
            }
            break
        case messageTypes().DEVICE.STATE_GROUP.type:
            def data = parsePayload 'DEVICE.STATE_GROUP', parsed
            def device = updateDeviceDefinition mac, ip, [group: data.label]
            if (device) {
                sendEvent ip, [name: 'group', value: data.label]
            }
            actions.commands << [ip: ip, type: messageTypes().DEVICE.GET_LOCATION.type as int]
            break
        case messageTypes().DEVICE.STATE_LOCATION.type:
            def data = parsePayload 'DEVICE.STATE_LOCATION', parsed
            def device = updateDeviceDefinition mac, ip, [location: data.label]
            if (device) {
                sendEvent ip, [name: 'location', value: data.label]
            }
            actions.commands << [ip: ip, type: messageTypes().DEVICE.GET_LABEL.type as int]
            break
        case messageTypes().DEVICE.STATE_WIFI_INFO.type:
            break
        case messageTypes().DEVICE.STATE_INFO.type:
            break
    }
    return actions
}

private Map lookupColor(String color) {
    Map foundColor
    if (color == "random") {
        foundColor = pickRandomColor()
        log.info "Setting random color: ${foundColor.name}"
    } else if (color?.startsWith('#')) {
        foundColor = [rgb: color]
    } else {
        foundColor = colorList().find { (it.name as String).equalsIgnoreCase(color) }
        if (!foundColor) {
            throw new RuntimeException("No color found for $color")
        }
    }
    Map myColor = getHexColor(foundColor.rgb)
    myColor.name = color

    myColor
}

private static Map getHexColor(String color) {
    Map rgb = hexToColor color
    rgbToHSV rgb.r, rgb.g, rgb.b, 'high'
}

private Map pickRandomColor() {
    def colors = colorList()
    def tempRandom = Math.abs(new Random().nextInt() % colors.size())
    colors[tempRandom]
}

List<Map> colorList(String sortOrder) {
    if (!(!sortOrder || '0' == sortOrder)) {
        switch (sortOrder) {
            case '1':
                List<Map> colorMapHSV = colorMap.collect {
                    it.hsv = getHexColor(it.rgb)
                    it
                }
                colorMapHSV.sort { a, b -> compareHSV(a.hsv, b.hsv) }
                return colorMapHSV
            case '2':
                List<Map> colorMapHSV = colorMap.collect {
                    it.hsv = getHexColor(it.rgb)
                    it
                }
                colorMapHSV.sort { a, b -> compareVHS(a.hsv, b.hsv) }
                return colorMapHSV
            case '3':
                List<Map> colorMapRGB = colorMap.collect {
                    it.rgbMap = hexToColor(it.rgb)
                    it
                }
                colorMapRGB.sort { a, b -> compareRGB(b.rgbMap, a.rgbMap) }
                return colorMapRGB
        }
    }
    colorMap
}

static int compareRGB(Map a, Map b) {
    def result = (a.r as short).compareTo(b.r as short)
    if (0 == result) {
        result = (a.g as short).compareTo(b.g as short)
    }
    if (0 == result) {
        result = (a.b as short).compareTo(b.b as short)
    }
    result
}

static int compareHSV(Map a, Map b) {
    def result = (a.h as float).compareTo(b.h as float)
    if (0 == result) {
        result = (a.s as float).compareTo(b.s as float)
    }
    if (0 == result) {
        result = (a.v as float).compareTo(b.v as float)
    }
    result
}

static int compareVHS(Map a, Map b) {
    def result = (a.v as float).compareTo(b.v as float)
    if (0 == result) {
        result = (a.h as float).compareTo(b.h as float)
    }
    if (0 == result) {
        result = (a.s as float).compareTo(b.s as float)
    }
    result
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

Map buildColorMaps(Map map) {
    logDebug "Map2 is $map"
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

static Map transformColorValue(Map hsv) {
    [hue: hsv.h, saturation: hsv.s, brightness: hsv.v]
}

List<Map> makeColorMaps(Map<String, Map> namedColors, String descriptor) {
    List<Map> result = []
//    logDebug "descriptor is $descriptor"
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

private Map<String, List> deviceSetHSBKAndPower(device, duration, Map<String, Number> hsbkMap, boolean displayed, String power = 'on') {
    logDebug("Map $hsbkMap")
    def actions = makeActions()
    if (hsbkMap) {
        actions.commands << makeCommand('LIGHT.SET_COLOR', [color: hsbkMap, duration: hsbkMap.duration])
        actions.events = actions.events + makeColorMapEvents(hsbkMap, displayed)
    }

    if (null != power && device.currentSwitch != power) {
        def powerLevel = 'on' == power ? 65535 : 0
        actions.commands << makeCommand('LIGHT.SET_POWER', [powerLevel: powerLevel, duration: duration])
        actions.events << [name: "switch", value: power, displayed: displayed, data: [syncing: "false"]]
    }

    actions
}

private List makeColorMapEvents(Map hsbkMap, Boolean displayed) {
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
    def result = [:]
    def brightness = colorMap.level ?: colorMap.brightness

    colorMap.hue ? result.hue = scaleUp100(colorMap.hue) as Integer : null
    colorMap.saturation ? result.saturation = scaleUp100(colorMap.saturation) as Integer : null
    colorMap.saturation ? result.saturation = scaleUp100(colorMap.saturation) as Integer : null
    brightness ? result.brightness = scaleUp100(brightness) as Integer : null
    result.kelvin = colorMap.kelvin
    result
}

private static Map makeCommand(String command, Map payload) {
    [cmd: command, payload: payload]
}

private static Map<String, List> makeActions() {
    [commands: [], events: []]
}

private static Map<String, Number> getCurrentHSBK(theDevice) {
    [
            hue       : scaleUp(theDevice.currentHue ?: 0, 100),
            saturation: scaleUp(theDevice.currentSaturation ?: 0, 100),
            brightness: scaleUp(theDevice.currentLevel as Long, 100),
            kelvin    : theDevice.currentcolorTemperature
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
    Map devices = getDeviceDefinitions()

    devices[device.mac] = device

    saveDeviceDefinitions devices
}

void deleteDeviceDefinition(Map device) {
    Map devices = getDeviceDefinitions()

    devices.remove device.mac

    saveDeviceDefinitions devices
}


String updateDeviceDefinition(String mac, String ip, Map properties) {
    Map device = getDeviceDefinition mac
    if (!device) {
        // perhaps it's a real device?
        return getChildDevice(ip) // @TODO Change to mac
    }
    properties.each { key, val -> (device[key] = val) }

    isDeviceComplete(device) ? makeRealDevice(device) : saveDeviceDefinition(device)
    null
}

List knownDeviceLabels() {
    getKnownIps().values().each { it.label }.asList()
}

private void makeRealDevice(Map device) {
    addToKnownIps device
    try {
        //@todo Change device.ip to device.mac
        addChildDevice 'robheyes', device.deviceName, device.ip, null, [group: device.group, label: device.label, location: device.location]
        addToKnownIps device
        updateKnownDevices()
        logInfo "Added device $device.label of type $device.deviceName with ip address $device.ip and MAC address $device.mac"
    } catch (com.hubitat.app.exception.UnknownDeviceTypeException e) {
        logWarn "${e.message} - you need to install the appropriate driver"
        device.error = "No driver installed for $device.deviceName"
        addToKnownIps(device)
    } catch (IllegalArgumentException ignored) {
        // Intentionally ignored. Expected if device already present
    }
    deleteDeviceDefinition device
}

private void addToKnownIps(Map device) {
    def knownIps = getKnownIps()
    knownIps[device.ip as String] = device
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

private static Map rgbToHSV(red = 255, green = 255, blue = 255, resolution = "low") {
    // Takes RGB (0-255) and returns HSV in 0-360, 0-100, 0-100
    // resolution ("low", "high") will return a hue between 0-100, or 0-360, respectively.

    double r = red / 255
    double g = green / 255
    double b = blue / 255

    double h
    double s

    double max = Math.max(Math.max(r, g), b)
    double min = Math.min(Math.min(r, g), b)
    double delta = (max - min)
    double v = (max * 100.0)

    s = (0.0d == max) ? 0 : (delta / max) * 100d

    if (delta == 0.0d) {
        h = 0.0
    } else if (r == max) {
        h = ((g - b) / delta) % 6
    } else if (g == max) {
        h = ((b - r) / delta) + 2
    } else if (b == max) {
        h = ((r - g) / delta) + 4
    } else {
        throw new ArithmeticException('max must be one of r, g or b')
    }


    h *= 60.0d

    if (resolution == "low") {
        h /= 3.6d
    }
    return [h: h, s: s, v: v]
}

private static Map hexToColor(String hex) {
    hex = hex.replace("#", "")
    if (hex.length() != 6) {
        null
    } else {
        [
                r: Integer.valueOf(hex.substring(0, 2), 16),
                g: Integer.valueOf(hex.substring(2, 4), 16),
                b: Integer.valueOf(hex.substring(4, 6), 16)
        ]
    }
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
    List<Map> candidate = null

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
        if (hashKey) {
            storeBuffer hashKey, buffer
        }
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

@SuppressWarnings("GrMethodMayBeStatic")
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

/* Many of these colours are taken from https://encycolorpedia.com/named */
@Field List<Map> colorMap =
        [
                [name: 'Absolute Zero', rgb: '#0048BA'],
                [name: 'Acajou', rgb: '#4C2F27'],
                [name: 'Acid Green', rgb: '#B0BF1A'],
                [name: 'Aero', rgb: '#7CB9E8'],
                [name: 'Aero Blue', rgb: '#C9FFE5'],
                [name: 'African Violet', rgb: '#B284BE'],
                [name: 'Air Superiority Blue', rgb: '#72A0C1'],
                [name: 'Alabama Crimson', rgb: '#AF002A'],
                [name: 'Alabaster', rgb: '#F2F0E6'],
                [name: 'Alice Blue', rgb: '#F0F8FF'],
                [name: 'Alizarin Crimson', rgb: '#E32636'],
                [name: 'Alloy Orange', rgb: '#C46210'],
                [name: 'Almond', rgb: '#EFDECD'],
                [name: 'Aloeswood Brown', rgb: '#5A6457'],
                [name: 'Aloewood Color', rgb: '#6A432D'],
                [name: 'Aluminum', rgb: '#D6D6D6'],
                [name: 'Aluminum Foil', rgb: '#D2D9DB'],
                [name: 'Amaranth', rgb: '#E52B50'],
                [name: 'Amaranth Deep Purple', rgb: '#9F2B68'],
                [name: 'Amaranth Pink', rgb: '#F19CBB'],
                [name: 'Amaranth Purple', rgb: '#AB274F'],
                [name: 'Amaranth Red', rgb: '#D3212D'],
                [name: 'Amazon', rgb: '#3B7A57'],
                [name: 'Amber', rgb: '#FFBF00'],
                [name: 'Amber (Kohaku-iro)', rgb: '#CA6924'],
                [name: 'Amber (SAE/ECE)', rgb: '#FF7E00'],
                [name: 'American Blue', rgb: '#3B3B6D'],
                [name: 'American Blue', rgb: '#3B3B6D'],
                [name: 'American Bronze', rgb: '#391802'],
                [name: 'American Brown', rgb: '#804040'],
                [name: 'American Gold', rgb: '#D3AF37'],
                [name: 'American Green', rgb: '#34B334'],
                [name: 'American Orange', rgb: '#FF8B00'],
                [name: 'American Pink', rgb: '#FF9899'],
                [name: 'American Purple', rgb: '#431C53'],
                [name: 'American Red', rgb: '#B32134'],
                [name: 'American Rose', rgb: '#FF033E'],
                [name: 'American Silver', rgb: '#CFCFCF'],
                [name: 'American Violet', rgb: '#551B8C'],
                [name: 'American Yellow', rgb: '#F2B400'],
                [name: 'Amethyst', rgb: '#9966CC'],
                [name: 'Amur Cork Tree', rgb: '#F3C13A'],
                [name: 'Anti-Flash White', rgb: '#F2F3F4'],
                [name: 'Antique Brass', rgb: '#CD9575'],
                [name: 'Antique Bronze', rgb: '#665D1E'],
                [name: 'Antique Fuchsia', rgb: '#915C83'],
                [name: 'Antique Ruby', rgb: '#841B2D'],
                [name: 'Antique White', rgb: '#FAEBD7'],
                [name: 'Apple', rgb: '#66B447'],
                [name: 'Apple Green', rgb: '#8DB600'],
                [name: 'Apricot', rgb: '#FBCEB1'],
                [name: 'Aqua', rgb: '#00FFFF'],
                [name: 'Aqua Blue', rgb: '#86ABA5'],
                [name: 'Aquamarine', rgb: '#7FFFD4'],
                [name: 'Arctic Lime', rgb: '#D0FF14'],
                [name: 'Argent', rgb: '#C0C0C0'],
                [name: 'Army Green', rgb: '#4B5320'],
                [name: 'Artichoke', rgb: '#8F9779'],
                [name: 'Arylide Yellow', rgb: '#E9D66B'],
                [name: 'Asparagus', rgb: '#87A96B'],
                [name: 'Ateneo Blue', rgb: '#003A6C'],
                [name: 'Atomic Tangerine', rgb: '#FF9966'],
                [name: 'Auburn', rgb: '#A52A2A'],
                [name: 'Aureolin', rgb: '#FDEE00'],
                [name: 'Avocado', rgb: '#568203'],
                [name: 'Awesome', rgb: '#FF2052'],
                [name: 'Axolotl', rgb: '#6E7F80'],
                [name: 'Azure', rgb: '#007FFF'],
                [name: 'Azure Mist', rgb: '#F0FFFF'],
                [name: 'Azureish White', rgb: '#DBE9F4'],

                [name: 'Beige', rgb: '#F5F5DC'],
                [name: 'Bisque', rgb: '#FFE4C4'],
                [name: 'Blanched Almond', rgb: '#FFEBCD'],
                [name: 'Blue', rgb: '#0000FF'],
                [name: 'Blue Violet', rgb: '#8A2BE2'],
                [name: 'Brown', rgb: '#A52A2A'],
                [name: 'Burly Wood', rgb: '#DEB887'],
                [name: 'Cadet Blue', rgb: '#5F9EA0'],
                [name: 'Chartreuse', rgb: '#7FFF00'],
                [name: 'Chocolate', rgb: '#D2691E'],
                [name: 'Cool White', rgb: '#F3F6F7'],
                [name: 'Coral', rgb: '#FF7F50'],
                [name: 'Corn Flower Blue', rgb: '#6495ED'],
                [name: 'Corn Silk', rgb: '#FFF8DC'],
                [name: 'Crimson', rgb: '#DC143C'],
                [name: 'Cyan', rgb: '#00FFFF'],
                [name: 'Dark Blue', rgb: '#00008B'],
                [name: 'Dark Cyan', rgb: '#008B8B'],
                [name: 'Dark Golden Rod', rgb: '#B8860B'],
                [name: 'Dark Gray', rgb: '#A9A9A9'],
                [name: 'Dark Green', rgb: '#006400'],
                [name: 'Dark Khaki', rgb: '#BDB76B'],
                [name: 'Dark Magenta', rgb: '#8B008B'],
                [name: 'Dark Olive Green', rgb: '#556B2F'],
                [name: 'Dark Orange', rgb: '#FF8C00'],
                [name: 'Dark Orchid', rgb: '#9932CC'],
                [name: 'Dark Red', rgb: '#8B0000'],
                [name: 'Dark Salmon', rgb: '#E9967A'],
                [name: 'Dark Sea Green', rgb: '#8FBC8F'],
                [name: 'Dark Slate Blue', rgb: '#483D8B'],
                [name: 'Dark Slate Gray', rgb: '#2F4F4F'],
                [name: 'Dark Turquoise', rgb: '#00CED1'],
                [name: 'Dark Violet', rgb: '#9400D3'],
                [name: 'Daylight White', rgb: '#F2F2F2'],
                [name: 'Deep Pink', rgb: '#FF1493'],
                [name: 'Deep Sky Blue', rgb: '#00BFFF'],
                [name: 'Dim Gray', rgb: '#696969'],
                [name: 'Dodger Blue', rgb: '#1E90FF'],
                [name: 'Fire Brick', rgb: '#B22222'],
                [name: 'Floral White', rgb: '#FFFAF0'],
                [name: 'Forest Green', rgb: '#228B22'],
                [name: 'Fuchsia', rgb: '#FF00FF'],
                [name: 'Gainsboro', rgb: '#DCDCDC'],
                [name: 'Ghost White', rgb: '#F8F8FF'],
                [name: 'Gold', rgb: '#FFD700'],
                [name: 'Golden Rod', rgb: '#DAA520'],
                [name: 'Gray', rgb: '#808080'],
                [name: 'Green', rgb: '#008000'],
                [name: 'Green Yellow', rgb: '#ADFF2F'],
                [name: 'Honeydew', rgb: '#F0FFF0'],
                [name: 'Hot Pink', rgb: '#FF69B4'],
                [name: 'Indian Red', rgb: '#CD5C5C'],
                [name: 'Indigo', rgb: '#4B0082'],
                [name: 'Ivory', rgb: '#FFFFF0'],
                [name: 'Khaki', rgb: '#F0E68C'],
                [name: 'Lavender', rgb: '#E6E6FA'],
                [name: 'Lavender Blush', rgb: '#FFF0F5'],
                [name: 'Lawn Green', rgb: '#7CFC00'],
                [name: 'Lemon Chiffon', rgb: '#FFFACD'],
                [name: 'Light Blue', rgb: '#ADD8E6'],
                [name: 'Light Coral', rgb: '#F08080'],
                [name: 'Light Cyan', rgb: '#E0FFFF'],
                [name: 'Light Golden Rod Yellow', rgb: '#FAFAD2'],
                [name: 'Light Gray', rgb: '#D3D3D3'],
                [name: 'Light Green', rgb: '#90EE90'],
                [name: 'Light Pink', rgb: '#FFB6C1'],
                [name: 'Light Salmon', rgb: '#FFA07A'],
                [name: 'Light Sea Green', rgb: '#20B2AA'],
                [name: 'Light Sky Blue', rgb: '#87CEFA'],
                [name: 'Light Slate Gray', rgb: '#778899'],
                [name: 'Light Steel Blue', rgb: '#B0C4DE'],
                [name: 'Light Yellow', rgb: '#FFFFE0'],
                [name: 'Lime', rgb: '#00FF00'],
                [name: 'Lime Green', rgb: '#32CD32'],
                [name: 'Linen', rgb: '#FAF0E6'],
                [name: 'Maroon', rgb: '#800000'],
                [name: 'Medium Aquamarine', rgb: '#66CDAA'],
                [name: 'Medium Blue', rgb: '#0000CD'],
                [name: 'Medium Orchid', rgb: '#BA55D3'],
                [name: 'Medium Purple', rgb: '#9370DB'],
                [name: 'Medium Sea Green', rgb: '#3CB371'],
                [name: 'Medium Slate Blue', rgb: '#7B68EE'],
                [name: 'Medium Spring Green', rgb: '#00FA9A'],
                [name: 'Medium Turquoise', rgb: '#48D1CC'],
                [name: 'Medium Violet Red', rgb: '#C71585'],
                [name: 'Midnight Blue', rgb: '#191970'],
                [name: 'Mint Cream', rgb: '#F5FFFA'],
                [name: 'Misty Rose', rgb: '#FFE4E1'],
                [name: 'Moccasin', rgb: '#FFE4B5'],
                [name: 'Navajo White', rgb: '#FFDEAD'],
                [name: 'Navy', rgb: '#000080'],
                [name: 'Old Lace', rgb: '#FDF5E6'],
                [name: 'Olive', rgb: '#808000'],
                [name: 'Olive Drab', rgb: '#6B8E23'],
                [name: 'Orange', rgb: '#FFA500'],
                [name: 'Orange Red', rgb: '#FF4500'],
                [name: 'Orchid', rgb: '#DA70D6'],
                [name: 'Pale Golden Rod', rgb: '#EEE8AA'],
                [name: 'Pale Green', rgb: '#98FB98'],
                [name: 'Pale Turquoise', rgb: '#AFEEEE'],
                [name: 'Pale Violet Red', rgb: '#DB7093'],
                [name: 'Papaya Whip', rgb: '#FFEFD5'],
                [name: 'Peach Puff', rgb: '#FFDAB9'],
                [name: 'Peru', rgb: '#CD853F'],
                [name: 'Pink', rgb: '#FFC0CB'],
                [name: 'Plum', rgb: '#DDA0DD'],
                [name: 'Powder Blue', rgb: '#B0E0E6'],
                [name: 'Purple', rgb: '#800080'],
                [name: 'R.A.F. Blue', rgb: '#5D8AA8'],
                [name: 'Red', rgb: '#FF0000'],
                [name: 'Rosy Brown', rgb: '#BC8F8F'],
                [name: 'Royal Blue', rgb: '#4169E1'],
                [name: 'Saddle Brown', rgb: '#8B4513'],
                [name: 'Salmon', rgb: '#FA8072'],
                [name: 'Sandy Brown', rgb: '#F4A460'],
                [name: 'Sea Green', rgb: '#2E8B57'],
                [name: 'Sea Shell', rgb: '#FFF5EE'],
                [name: 'Sienna', rgb: '#A0522D'],
                [name: 'Silver', rgb: '#C0C0C0'],
                [name: 'Sky Blue', rgb: '#87CEEB'],
                [name: 'Slate Blue', rgb: '#6A5ACD'],
                [name: 'Slate Gray', rgb: '#708090'],
                [name: 'Snow', rgb: '#FFFAFA'],
                [name: 'Soft White', rgb: '#B6DA7C'],
                [name: 'Spring Green', rgb: '#00FF7F'],
                [name: 'Steel Blue', rgb: '#4682B4'],
                [name: 'Tan', rgb: '#D2B48C'],
                [name: 'Teal', rgb: '#008080'],
                [name: 'Thistle', rgb: '#D8BFD8'],
                [name: 'Tomato', rgb: '#FF6347'],
                [name: 'Turquoise', rgb: '#40E0D0'],
                [name: 'U.S.A.F. Blue', rgb: '#00308F'],
                [name: 'Violet', rgb: '#EE82EE'],
                [name: 'Warm White', rgb: '#DAF17E'],
                [name: 'Wheat', rgb: '#F5DEB3'],
                [name: 'White', rgb: '#FFFFFF'],
                [name: 'White Smoke', rgb: '#F5F5F5'],
                [name: 'Yellow', rgb: '#FFFF00'],
                [name: 'Yellow Green', rgb: '#9ACD32'],
        ]


void logWarn(String msg) {
    log.warn msg
}

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
