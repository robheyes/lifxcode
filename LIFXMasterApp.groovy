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

@Field Integer extraProbesPerPass = 0
@Field Boolean wantBufferCaching = false // should probably remove this?

definition(
        name: 'LIFX Master',
        namespace: 'robheyes',
        author: 'Robert Alan Heyes',
        description: 'Provides for discovery and control of LIFX devices',
        category: 'Discovery',
        iconUrl: '',
        iconX2Url: '',
        iconX3Url: ''
)

preferences {
    page(name: 'mainPage')
    page(name: 'discoveryPage')
    page(name: 'namedColorsPage')
    page(name: 'testBedPage')
}


@SuppressWarnings("unused")
def mainPage() {
    dynamicPage(name: "mainPage", title: "Options", install: true, uninstall: true) {
        section {
            input 'interCommandPause', 'number', defaultValue: 50, title: 'Time between commands (milliseconds)', submitOnChange: true
            input 'maxPasses', 'number', title: 'Maximum number of passes', defaultValue: 2, submitOnChange: true
            input 'refreshInterval', 'number', title: 'Discovery page refresh interval (seconds).<br><strong>WARNING</strong>: high refresh rates may interfere with discovery.', defaultValue: 6, submitOnChange: true
            input 'namePrefix', 'text', title: 'Device name prefix', description: 'If you specify a prefix then all your device names will be preceded by this value', submitOnChange: true
            input 'baseIpSegment', 'text', title: 'IP subnet(s)', description: 'e.g. 192.168.0 or 192.168.1, separate multiple subnets with commas', submitOnChange: true
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

@SuppressWarnings("unused")
def discoveryPage() {
    dynamicPage(name: 'discoveryPage', title: 'Discovery', refreshInterval: refreshInterval()) {
        section {
            paragraph "<strong>RECOMMENDATION</strong>: The device network id (DNI) for a LIFX device is based on its IP address. It is, therefore, advisable to configure your router's DHCP settings to use fixed IP addresses for all LIFX devices"
            paragraph '<strong>ADVICE</strong>: I would suggest that it\'s a good idea to create groups for all your ' +
                    'devices, and not just LIFX ones. This will make your rules and other automations dependent only ' +
                    'on the groups and not the actual hardware, making it easier to replace devices at a later date ' +
                    'with minimal disruption.<br>If you do this, then you may want to set the device prefix on the ' +
                    'settings page to provide a way of clearly distinguishing between the group name and the device name.'
            input 'discoverBtn', 'button', title: 'Discover devices'
            paragraph 'If you have added a new device, or not all of your devices are discovered the first time around, try the <strong>Discover only new devices</strong> button below'
            paragraph(
                    null == atomicState.scanPass ?
                            '' :
                            (
                                    ('DONE' == atomicState.scanPass) ?
                                            'Scanning complete' :
                                            "Scanning your network for devices from subnets [${describeSubnets()}]" +
                                                    "<div class='meter'>" +
                                                    "<span style='width:${getProgressPercentage()}'><strong>${getProgressPercentage()}</strong></span>" +
                                                    "</div>"
                            )

            )
            paragraph(
                    discoveryTextKnownDevices()
            )

            input 'discoverNewBtn', 'button', title: discoverNewText()
            input 'clearCachesBtn', 'button', title: 'Clear caches'

        }
        mainPageLink()
        colorsPageLink()
        includeStyles()
    }
}

private String discoverNewText() {
    'Discover only new devices'
}

@SuppressWarnings("unused")
def namedColorsPage() {
    dynamicPage(name: 'namedColorsPage', title: 'Named Colors') {
        mainPageLink()
        section {
            input 'sortOrder', 'enum', title: 'Sort by: ', options: ['0': 'Alphabetical', '1': 'By Hue', '2': 'By Level', '3': 'By RGB'], submitOnChange: true
            paragraph(
                    colorListHTML(settings.sortOrder as String)
            )
        }
        discoveryPageLink()
        includeStyles()
    }
}


@SuppressWarnings("unused")
def testBedPage() {
    dynamicPage(name: 'testBedPage', title: 'Testing stuff') {
        section {
            input 'multizone', 'capability.light', title: 'Multizone Light', submitOnChange: true
            input 'colors', 'text', title: 'Colors', defaultValue: '{"a": "Red", "b": "#FFDDAA", "c": "random", "d": {"h":20,"s":50,"v":100}}'
            input 'colors2', 'color_map', title: 'Colors', defaultValue: '[a: [color: Red], b: [color:"#FFDDAA"], c: [color: random], d: [h:20,s:50,v:100]]'
            input 'pattern', 'text', title: 'Descriptor', defaultValue: 'a:1,b:2,c:3,d:1'
            input 'testBtn', 'button', title: 'Test pattern'
            input 'fetchBtn', 'button', title: 'Load from device'
        }
        section {
            paragraph("Flattened = ${flattenedDescriptors()}")
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

private String styles() {
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
    
    button.hrefElem span.state-incomplete-text {
        display: block 
    }
    
    button.hrefElem span {
        display: none
    }
    
    button.hrefElem br {
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

String deviceNamePrefix() {
    settings.namePrefix ? settings.namePrefix + ' ' : ""
}

String ipSegment() {
    settings.baseIpSegment
}

@SuppressWarnings("unused")
def updated() {
    logDebug 'LIFX updating'
    atomicState.subnets = null
    initialize()
}

@SuppressWarnings("unused")
def installed() {
    logDebug 'LIFX installed'
    initialize()
}

@SuppressWarnings("unused")
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

@SuppressWarnings("unused")
def appButtonHandler(btn) {
    if (btn == "discoverBtn") {
        clearKnownIps()
        clearDeviceDefinitions()
        atomicState.packets = null
        removeChildren()
        discover()
    } else if (btn == 'discoverNewBtn') {
        clearKnownIpsWithErrors()
        discoverNew()
    } else if (btn == 'refreshExistingBtn') {
        refreshExisting()
    } else if (btn == 'clearCachesBtn') {
        clearCachedDescriptors()
        clearDeviceDefinitions()
        clearBufferCache()
    } else if (btn == 'testBtn') {
        testColorMapBuilder()
    } else if (btn == 'fetchBtn') {
        loadFromMultizone()
    }
}

def loadFromMultizone() {
    if (!multizone) {
        logDebug 'No multizone device'
        return
    }
}


def testColorMapBuilder() {
    Map<String, Map> map = buildColorMaps(settings.colors)
    def hsbkMaps = makeColorMaps map, settings.pattern as String
}

def setScanPass(pass) {
    atomicState.scanPass = pass ?: null
}

def refresh() {
    removeChildren()
    discovery('discovery')
}

def discoverNew() {
    endDiscovery()
    discovery('discovery')

}

def refreshExisting() {
    endDiscovery()
}

private void discover() {
    logInfo("Discovery started")
    String[] subnets = getSubnets()
    if (0 == subnets.size()) {
        log.warn "Can't discover the hub's subnet!"
        return
    }

    clearCachedDescriptors()
    int scanPasses = maxScanPasses()
    Map queue = prepareQueue(makeVersionPacket())
    subnets.each {
        String subnet = it
        1.upto(scanPasses) {
            setScanPass(it)
            scanNetwork queue, subnet, it
        }
    }
//    sendEvent name: 'progress', value: 0
    queue.size = queue.ipAddresses.size()
    runInMillis(50, 'processQueue', [data: queue])
}

private String makeVersionPacket() {
    makeDiscoveryPacketString typeOfMessage('DEVICE.GET_VERSION')
}

private void scanNetwork(Map queue, String subnet, Number pass) {
    1.upto(pass + extraProbesPerPass) {
        1.upto(254) {
            def ipAddress = subnet + it
            queue.ipAddresses << ipAddress
        }
    }
}

def handleOutstandingDevices(Map outstandingDevices, Map queue) {
    logDebug("Processing outstanding devices")
    queue.attempts++
    if (queue.attempts > 5) {
        return
    }
    outstandingDevices.each {
        mac, data ->
            queue.ipAddresses << data.ip
    }

    queue.size = queue.ipAddresses.size()
    runInMillis(50, 'processQueue', [data: queue])
}


private Map prepareQueue(String packet, int delay = 20) {
    [packet: packet, ipAddresses: [], delay: delay, attempts: 0]
}

@SuppressWarnings("unused")
private processQueue(Map queue) {
    def oldPercent = calculateQueuePercentage(queue)
    if (isQueueEmpty(queue)) {
        endDiscovery()
        return
    }
    def data = getNext(queue)
    sendPacket data.ipAddress, data.packet
    def newPercent = calculateQueuePercentage(queue)
    if (oldPercent != newPercent) {
        showProgress(newPercent)
    }
    runInMillis(queue.delay, 'processQueue', [data: queue])
}

private Map<String, Object> getNext(Map queue) {
    String first = queue.ipAddresses.first()
    queue.ipAddresses = queue.ipAddresses.tail()
    [ipAddress: first, packet: queue.packet]
}

private isQueueEmpty(Map queue) {
    queue.ipAddresses.isEmpty()
}

private int calculateQueuePercentage(Map queue) {
    100 - (int) ((queue.ipAddresses.size() * 100) / queue.size as Long)
}

private void sendPacket(String ipAddress, String bytes) {
    broadcast bytes, ipAddress
}

private void broadcast(String stringBytes, String ipAddress) {
    sendHubCommand(
            new hubitat.device.HubAction(
                    stringBytes,
                    hubitat.device.Protocol.LAN,
                    [
                            type              : hubitat.device.HubAction.Type.LAN_TYPE_UDPCLIENT,
                            destinationAddress: ipAddress + ":56700",
                            encoding          : hubitat.device.HubAction.Encoding.HEX_STRING,
                            ignoreWarning     : true,
                            callback          : "discoveryParse"
                    ]
            )
    )
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
//    def discoveryDevice = addChildDevice 'robheyes', 'LIFX Discovery', 'LIFX Discovery'
//    subscribe discoveryDevice, 'lifxdiscovery.complete', removeDiscoveryDevice
//    subscribe discoveryDevice, 'lifxdiscovery.outstanding', handleOutstandingDevices
//    subscribe discoveryDevice, 'progress', progress
    discover()
}

@SuppressWarnings("unused")
def progress(evt) {
    def percent = evt.getIntegerValue()
    showProgress(percent)
}

private void showProgress(int percent) {
    Integer delta = percent - (atomicState.progressPercent ?: 0)
    if (delta.abs() > 10) {
        atomicState.progressPercent = percent
    }
}

def getProgressPercentage() {
    def percent = atomicState.progressPercent ?: 0
    "$percent%"
}

void endDiscovery() {
    logInfo 'Discovery complete'
//    unsubscribe()
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


def enableLevelChange(com.hubitat.app.DeviceWrapper device) {
    sendEvent(device, [name: "cancelLevelChange", value: 'no', displayed: false])
}

// Common device commands
//def deviceDoLevelChange(Map params) {
//    logDebug "Params $params"
//    String deviceId = params.dni
//    com.hubitat.app.DeviceWrapper device = getChildDevice(deviceId)
//    logDebug "Device for $deviceId is $device"
//    def direction = params.direction
//    def changeLevelStep = params.step
//    def changeLevelEvery = params.every
//    def useActivityLog = params.logIt as Boolean
////    if (0 == direction) {
////        return
////    }
//    def cancelling = device.currentValue('cancelLevelChange') ?: 'no'
//    if (cancelling == 'yes') {
//        runInMillis 2 * (changeLevelEvery as Integer), "enableLevelChange", [data: device]
//        return;
//    }
//    def newLevel = device.currentValue('level') + ((direction as Float) * (changeLevelStep as Float))
//    def lastStep = false
//    if (newLevel < 0) {
//        newLevel = 0
//        lastStep = true
//    } else if (newLevel > 100) {
//        newLevel = 100
//        lastStep = true
//    }
//    sendActions device, deviceId, deviceSetLevel(device, newLevel, useActivityLog, (changeLevelEvery - 1) / 1000)
//    if (!lastStep) {
//        runInMillis(
//                changeLevelEvery as Integer,
//                "deviceDoLevelChange",
//                [
//                        data: [
//                                direction: direction,
//                                dni      : deviceId,
//                                step     : changeLevelStep,
//                                every    : changeLevelEvery,
//                                logIt    : useActivityLog
//                        ]
//                ]
//        )
//    }
//}
//
//private void sendActions(com.hubitat.app.DeviceWrapper device, String ipAddress, Map<String, List> actions) {
//    actions.commands?.each { item -> lifxCommand(device, item.cmd, item.payload) { List buffer -> sendPacket ipAddress, buffer, true } }
//    actions.events?.each { sendEvent it }
//}
//
//def parser(value) {
//    logDebug("Value is $value")
//}
//
//private void sendPacket(String ipAddress, List buffer, boolean noResponseExpected = false) {
//    String stringBytes = hubitat.helper.HexUtils.byteArrayToHexString asByteArray(buffer)
//    logDebug("Sending $stringBytes to $ipAddress")
//    sendHubCommand(
//            new hubitat.device.HubAction(
//                    stringBytes,
//                    hubitat.device.Protocol.LAN,
//                    ipAddress,
//                    [
//                            callback: 'parser',
//                            type              : hubitat.device.HubAction.Type.LAN_TYPE_UDPCLIENT,
//                            destinationAddress: ipAddress + ":56700",
//                            encoding          : hubitat.device.HubAction.Encoding.HEX_STRING,
//                            ignoreResponse    : noResponseExpected
//                    ]
//            )
//    )
//}

Map<String, List> deviceOnOff(String value, Boolean displayed, duration = 0) {
    def actions = makeActions()
    actions.commands << makeCommand('LIGHT.SET_POWER', [powerLevel: value == 'on' ? 65535 : 0, duration: duration * 1000])
    actions.events << [name: "switch", value: value, displayed: displayed, data: [syncing: "false"]]
    actions
}

Map<String, List> deviceSetZones(com.hubitat.app.DeviceWrapper device, Map zoneMap, Boolean displayed = true) {
    def actions = makeActions()
    actions.commands << makeCommand('MULTIZONE.SET_EXTENDED_COLOR_ZONES', zoneMap)
    actions
}

Map<String, List> deviceSetColor(com.hubitat.app.DeviceWrapper device, Map colorMap, Boolean displayed, duration = 0) {
    def hsbkMap = getCurrentHSBK device
    hsbkMap << getScaledColorMap(colorMap)
    hsbkMap.duration = (colorMap.duration ?: duration)
    deviceSetHSBKAndPower(device, duration, hsbkMap, displayed)
}

Map<String, List> deviceSetColor(com.hubitat.app.DeviceWrapper device, String colorMap, Boolean displayed, duration = 0) {
    deviceSetColor(device, stringToMap(colorMap), displayed, duration)
}

Map<String, List> deviceSetHue(com.hubitat.app.DeviceWrapper device, Number hue, Boolean displayed, duration = 0) {
    def hsbkMap = getCurrentHSBK device
    hsbkMap.hue = scaleUp100 hue
    hsbkMap.duration = duration

    deviceSetHSBKAndPower(device, duration, hsbkMap, displayed)
}

Map<String, List> deviceSetSaturation(com.hubitat.app.DeviceWrapper device, Number saturation, Boolean displayed, duration = 0) {
    def hsbkMap = getCurrentHSBK device
    hsbkMap.saturation = scaleUp100 saturation
    hsbkMap.duration = duration

    deviceSetHSBKAndPower(device, duration, hsbkMap, displayed)
}

Map<String, List> deviceSetColorTemperature(com.hubitat.app.DeviceWrapper device, Number temperature, Boolean displayed, duration = 0) {
    def hsbkMap = [kelvin: temperature, duration: duration, brightness: scaleUp(device.currentLevel as Long, 100)]

    deviceSetHSBKAndPower(device, duration, hsbkMap, displayed)
}

Map<String, List> deviceSetIRLevel(com.hubitat.app.DeviceWrapper device, Number level, Boolean displayed, duration = 0) {
    def actions = makeActions()
    actions.commands << makeCommand('LIGHT.SET_INFRARED', [irLevel: scaleUp100(level)])
    actions.events << [name: "IRLevel", value: level, displayed: displayed, data: [syncing: "false"]]
    actions
}

//@TODO change duration to be a float to allow for fractional durations
Map<String, List> deviceSetLevel(com.hubitat.app.DeviceWrapper device, Number level, Boolean displayed, Number duration = 0.0) {
    if ((null == level || level <= 0) && 0 == duration) {
        return deviceOnOff('off', displayed)
    }
    if (level > 100) {
        level = 100
    }
    def hsbkMap = getCurrentHSBK(device)
    if (device.hasCapability('ColorMode') && (device.currentValue('colorMode') == 'CT') || hsbkMap.saturation == 0) {
        hsbkMap.brightness = scaleUp100 level
        hsbkMap.hue = 0
        hsbkMap.saturation = 0
        hsbkMap.duration = duration
    } else {
        hsbkMap = [
                hue       : scaleUp100(device.currentHue),
                saturation: scaleUp100(device.currentSaturation),
                brightness: scaleUp100(level),
                kelvin    : device.currentColorTemperature,
                duration  : duration,
        ]
    }

    deviceSetHSBKAndPower(device, duration, hsbkMap, displayed)
}

Map<String, List> deviceSetState(com.hubitat.app.DeviceWrapper device, Map myStateMap, Boolean displayed, duration = 0) {
    String power = myStateMap.power
    Number level = (myStateMap.level ?: myStateMap.brightness) as Number
    def kelvin = myStateMap.kelvin ?: myStateMap.temperature
    String color = myStateMap.color ?: myStateMap.colour
    duration = (myStateMap.duration ?: duration) as Integer

    if (color) {
        Map myColor
        myColor = (null == color) ? null : lookupColor(color.replace('_', ' '))
        Map<String, Object> realColor = [
                hue       : scaleUp(myColor.h ?: 0, 360),
                saturation: scaleUp100(myColor.s ?: 0),
                brightness: scaleUp100(level ?: (myColor.v ?: 50)),
                kelvin    : kelvin ?: device.currentColorTemperature, // not sure this makes any sense
                duration  : duration
        ]
        if (myColor.name) {
            realColor.name = myColor.name
        }
        return deviceSetHSBKAndPower(device, duration, realColor, displayed, power)
    }
    if (kelvin) {
        Map<String, Object> realColor = [
                hue       : 0, // does this make sense? Yes, because of Groovy truth
                saturation: 0,
                kelvin    : kelvin,
                brightness: scaleUp100(level ?: 100),
                duration  : duration,
                name      : null
        ]
        return deviceSetHSBKAndPower(device, duration, realColor, displayed, power)
    }
    if (level) {
        return deviceSetLevel(device, level, displayed, duration)
    }
    return [:] // do nothing
}

List<Map> parseForDevice(device, String description, Boolean displayed, Boolean updateDevice = false) {
    Map header = parseHeaderFromDescription description
    switch (header.type) {
        case messageType["DEVICE.STATE_VERSION"]:
            log.warn("STATE_VERSION type ignored")
            return []
        case messageType['DEVICE.STATE_LABEL']:
            if (updateDevice) {
                def data = parsePayload 'DEVICE.STATE_LABEL', header
                device.setLabel(officialDeviceName(data.label.trim()))
            }
            return []
        case messageType['DEVICE.STATE_GROUP']:
            def data = parsePayload 'DEVICE.STATE_GROUP', header
            String group = data.label
            return [[name: 'group', value: group]]
        case messageType['DEVICE.STATE_LOCATION']:
            def data = parsePayload 'DEVICE.STATE_LOCATION', header
            String location = data.label
            return [[name: 'location', value: location]]
        case messageType['DEVICE.STATE_HOST_INFO']:
            def data = parsePayload 'DEVICE.STATE_HOST_INFO', header
            break
        case messageType['DEVICE.STATE_INFO']:
            def data = parsePayload 'DEVICE.STATE_INFO', header
            break
        case messageType['LIGHT.STATE']:
            def data = parsePayload 'LIGHT.STATE', header
            if (updateDevice) {
                def label = data.label.trim()
                def deviceName = officialDeviceName(label)
                device.setName deviceName
                device.setLabel deviceName
            }
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
        case messageType['LIGHT.STATE_INFRARED']:
            def data = parsePayload 'LIGHT.STATE_INFRARED', header
            return [[name: 'IRLevel', value: scaleDown100(data.irLevel), displayed: displayed]]
        case messageType['DEVICE.STATE_POWER']:
            Map data = parsePayload 'DEVICE.STATE_POWER', header
            return [[name: "switch", value: (data.powerLevel as Integer == 0 ? "off" : "on"), displayed: displayed, data: [syncing: "false"]],]
        case messageType['LIGHT.STATE_POWER']:
            Map data = parsePayload 'LIGHT.STATE_POWER', header
            return [
                    [name: "switch", value: (data.powerLevel as Integer == 0 ? "off" : "on"), displayed: displayed, data: [syncing: "false"]],
                    [name: "level", value: (data.powerLevel as Integer == 0 ? 0 : 100), displayed: displayed, data: [syncing: "false"]]
            ]
/*
        case messageType['DEVICE.ACKNOWLEDGEMENT']:
            Byte sequence = header.sequence
            clearExpectedAckFor device, sequence
            return []
*/
        case messageType['MULTIZONE.STATE_EXTENDED_COLOR_ZONES']:
            Map data = parsePayload 'MULTIZONE.STATE_EXTENDED_COLOR_ZONES', header
//            String compressed = compressMultizoneData data
            def multizoneHtml = renderMultizone(data)
            return [
                    [name: 'multizone', value: multizoneHtml, data: data, displayed: true],
            ]
        default:
            logWarn "Unhandled response for ${header.type}"
            return []
    }
    return []
}

@SuppressWarnings("unused")
void discoveryParse(response) {
    def description = response.description
    def actions = makeActions()
    Map deviceParams = parseDeviceParameters description
    String ip = convertIpLong(deviceParams.ip as String)
    Map parsed = parseHeader deviceParams

    final String mac = deviceParams.mac
    switch (parsed.type) {
        case messageType['DEVICE.STATE_VERSION']:
            if (isKnownIp(ip)) {
                break
            }
            def existing = getDeviceDefinition mac
            if (!existing) {
                createDeviceDefinition parsed, ip, mac
            }
            actions.commands << [ip: ip, type: messageType['DEVICE.GET_GROUP']]
            break
        case messageType['DEVICE.STATE_LABEL']:
            def data = parsePayload 'DEVICE.STATE_LABEL', parsed
            def device = updateDeviceDefinition mac, ip, [label: officialDeviceName(data.label as String)]
            if (device) {
                sendEvent ip, [name: 'label', value: officialDeviceName(data.label as String)]
                sendEvent ip, [name: 'deviceName', value: officialDeviceName(data.label as String)]
            }
            break
        case messageType['DEVICE.STATE_GROUP']:
            def data = parsePayload 'DEVICE.STATE_GROUP', parsed
            def device = updateDeviceDefinition mac, ip, [group: data.label]
            if (device) {
                sendEvent ip, [name: 'group', value: data.label]
            }
            actions.commands << [ip: ip, type: messageType['DEVICE.GET_LOCATION']]
            break
        case messageType['DEVICE.STATE_LOCATION']:
            def data = parsePayload 'DEVICE.STATE_LOCATION', parsed
            def device = updateDeviceDefinition mac, ip, [location: data.label]
            if (device) {
                sendEvent ip, [name: 'location', value: data.label]
            }
            actions.commands << [ip: ip, type: messageType['DEVICE.GET_LABEL']]
            break
        case messageType['DEVICE.STATE_WIFI_INFO']:
            break
        case messageType['DEVICE.STATE_INFO']:
            break
    }
    sendDiscoveryActions actions
}

private void sendDiscoveryActions(Map<String, List> actions) {
    actions.commands?.eachWithIndex { item, index -> sendDiscoveryCommand item.ip as String, item.type as int, 1 }
    actions.events?.each { sendEvent it }
}

private void sendCommand(String deviceAndType, Map payload = [:], boolean responseRequired, Closure<List> sender) {
    def buffer = []
    sender makePacket(buffer, deviceAndType, payload, responseRequired)
}

private void sendDiscoveryCommand(String ipAddress, int messageType, int pass = 1) {
    String stringBytes = makeDiscoveryPacketString messageType
    sendPacket ipAddress, stringBytes
    pauseExecution(interCommandPauseMilliseconds(pass))
}

private Map<String, Object> getPacketStringCache() {
    if (null == atomicState.packets) {
        atomicState.packets = new HashMap<String, String>()
    }
    atomicState.packets
}

private storePacket(String messageType, Object bytes) {
    packets = getPacketStringCache()
    packets[messageType] = bytes
    atomicState.packets = packets
}

private Object getCachedPacket(String messageType) {
    def cache = getPacketStringCache()
    def bytes = cache.get(messageType)
    bytes
}

private String makeDiscoveryPacketString(int messageType) {
    def bytes = getCachedPacket(messageType as String)
    if (bytes) {
        return bytes as String
    }
    def buffer = []
    simpleMakePacket buffer, messageType, true, []
    def rawBytes = asByteArray(buffer)
    String stringBytes = hubitat.helper.HexUtils.byteArrayToHexString rawBytes
    storePacket(messageType as String, stringBytes)
    stringBytes
}

byte[] asByteArray(List buffer) {
    (buffer.each { it as byte }) as byte[]
}

@SuppressWarnings("unused")
void lifxQuery(com.hubitat.app.DeviceWrapper device, String deviceAndType, Closure<List> sender) {
    sendCommand deviceAndType, [:], true, sender
}

@SuppressWarnings("unused")
void lifxQuery(com.hubitat.app.DeviceWrapper device, List<String> deviceAndType, Closure<List> sender) {
    deviceAndType.each { sendCommand it, [:], true, sender }
}

@SuppressWarnings("unused")
void lifxCommand(com.hubitat.app.DeviceWrapper device, String deviceAndType, Map payload, Closure<List> sender) {
    sendCommand deviceAndType, payload, false, sender
}

List makePacket(List buffer, String deviceAndType, Map payload, Boolean responseRequired = true) {
    def tryCache = responseRequired && payload.isEmpty()

    if (tryCache) {
        def bytes = getCachedPacket(deviceAndType)
        if (bytes) {
            return bytes as List<Byte>
        }
    }

    def listPayload = makePayload(deviceAndType, payload)
    int messageType = messageType[deviceAndType]
    simpleMakePacket(buffer, messageType, responseRequired, listPayload)
    storePacket(deviceAndType, buffer)
    buffer
}

private List simpleMakePacket(List buffer, int messageType, Boolean responseRequired = false, List payload = []) {
    byte[] targetAddress = [0, 0, 0, 0, 0, 0]
    createFrame buffer, targetAddress.every { it == 0 }
    createFrameAddress buffer, targetAddress, false, responseRequired, 0 as byte
    createProtocolHeader buffer, messageType as short
    createPayload buffer, payload as byte[]

    put buffer, 0, buffer.size() as short

    return buffer
}

Boolean isKnownIp(String ip) {
    def knownIps = getKnownIps()
    null != knownIps[ip]
}

private void expectAckFor(com.hubitat.app.DeviceWrapper device, Byte sequence, List buffer) {
    def expected = atomicState.expectedAckFor ?: [:]
    expected[device.getDeviceNetworkId() as String] = [sequence: sequence, buffer: buffer]
    atomicState.expectedAckFor = expected
}

private Byte ackWasExpected(com.hubitat.app.DeviceWrapper device) {
    def expected = atomicState.expectedAckFor ?: [:]
    expected[device.getDeviceNetworkId() as String]?.sequence as Byte
}

private void clearExpectedAckFor(com.hubitat.app.DeviceWrapper device, Byte sequence) {
    def expected = atomicState.expectedAckFor ?: [:]
    expected.remove(device.getDeviceNetworkId())
    atomicState.expectedAckFor = expected
}

private List getBufferToResend(com.hubitat.app.DeviceWrapper device, Byte sequence) {
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

int typeOfMessage(String deviceAndType) { messageType[deviceAndType] }

void clearCachedDescriptors() { atomicState.cachedDescriptors = null }

String getSubnet() {
    if (null != settings.baseIpSegment) {
        def baseIp = parseIPSegment(settings.baseIpSegment)
        if (baseIp != null) {
            return baseIp
        }
    }
    def ip = getHubIP()
    def m = ip =~ /^(\d{1,3}\.\d{1,3}\.\d{1,3}\.)\d{1,3}/
    if (!m) {
        logWarn('ip does not match pattern')
        return null
    }
    return m.group(1)
}

String describeSubnets() {
    def subnets = getSubnets()
    subnets.join ','
}

String[] getSubnets() {
    if (atomicState.subnets != null) {
        return atomicState.subnets
    }
    def baseIps = getBaseIps()
    if (baseIps?.size() != 0) {
        atomicState.subnets = baseIps
        return baseIps
    }
    def ip = getHubIP()
    def m = ip =~ /^(\d{1,3}\.\d{1,3}\.\d{1,3}\.)\d{1,3}/
    if (!m) {
        logWarn "ip $ip does not match pattern"
        return null
    }
    atomicState.subnets = [m.group(1)]
    return atomicState.subnets
}

String[] getBaseIps() {
    String ipSegment = settings.baseIpSegment
    return ipSegment == null ? [] : ipSegment.split(/,/)?.collect { return parseIPSegment(it) }
}

private String parseIPSegment(String ipSegment) {
    def m = ipSegment =~ /^(\d{1,3}\.\d{1,3}\.\d{1,3})/
    if (!m) {
        logDebug 'null segment'
        return null
    }
    def segment = m.group(1)
    (segment.endsWith('.')) ? segment : segment + '.'
}

// returns [h, s, v, name]
private Map lookupColor(String color) {
    Map foundColor
    if (color?.startsWith('#')) {
        foundColor = getHexColor(color)
        foundColor.name = color
        return foundColor
    }

    if (color == "random") {
        foundColor = pickRandomColor()
    } else {
        foundColor = fullColorMap.find { (it.name as String).equalsIgnoreCase(color) }
        if (!foundColor) {
            throw new RuntimeException("No color found for $color")
        }
    }

    return transformNamedColor(foundColor)
}

private Map transformNamedColor(Map foundColor) {
    Map theColor = foundColor.hsv as Map
    theColor.name = foundColor.name
    theColor
}

private Map getHexColor(String color) {
    Map rgb = hexToColor color
    rgbToHSV rgb.r, rgb.g, rgb.b, 'high'
}

private Map expandRgb(Map colorDef) {
    Map rgb = hexToColor(colorDef.rgb)
    Map hsv = rgbToHSV rgb.r, rgb.g, rgb.b, 'high'
    [name: colorDef.name, rgb: colorDef.rgb, rgbMap: rgb, hsv: hsv]
}

private Map pickRandomColor() {
    def colors = fullColorMap
    def tempRandom = Math.abs(new Random().nextInt() % colors.size())
    colors[tempRandom]
}

private List<Map> colorList(String sortOrder) {
    if (!(!sortOrder || '0' == sortOrder)) {
        switch (sortOrder) {
            case '1':
                List<Map> colorMapHSV = fullColorMap
                colorMapHSV.sort { a, b -> compareHSV(a.hsv, b.hsv) }
                return colorMapHSV
            case '2':
                List<Map> colorMapHSV = fullColorMap
                colorMapHSV.sort { a, b -> compareVHS(a.hsv, b.hsv) }
                return colorMapHSV
            case '3':
                List<Map> colorMapRGB = fullColorMap
                colorMapRGB.sort { a, b -> compareRGB(b.rgbMap, a.rgbMap) }
                return colorMapRGB
        }
    }
    colorMap
}

private int compareRGB(Map a, Map b) {
    def result = (a.r as short).compareTo(b.r as short)
    if (0 == result) {
        result = (a.g as short).compareTo(b.g as short)
    }
    if (0 == result) {
        result = (a.b as short).compareTo(b.b as short)
    }
    result
}

private int compareHSV(Map a, Map b) {
    def result = (a.h as float).compareTo(b.h as float)
    if (0 == result) {
        result = (a.s as float).compareTo(b.s as float)
    }
    if (0 == result) {
        result = (a.v as float).compareTo(b.v as float)
    }
    result
}

private int compareVHS(Map a, Map b) {
    def result = (a.v as float).compareTo(b.v as float)
    if (0 == result) {
        result = (a.h as float).compareTo(b.h as float)
    }
    if (0 == result) {
        result = (a.s as float).compareTo(b.s as float)
    }
    result
}

private Map buildColorMaps(String jsonString) {
    def slurper = new JsonSlurper()
    Map map = slurper.parseText jsonString
    Map<String, Map> result = [:]
    map.each {
        key, value ->
            result[key] = getScaledColorMap transformColorValue(value)
    }
    result
}

private Map buildColorMaps(Map map) {
    Map result = [:]
    map.each {
        key, value ->
            result[key] = getScaledColorMap transformColorValue(value)
    }
    result
}

private Map transformColorValue(String value) {
    transformColorValue(lookupColor(value))
}

private Map transformColorValue(Map hsv) {
    [hue: hsv.h, saturation: hsv.s, brightness: hsv.v]
}

private List<Map> makeColorMaps(Map<String, Map> namedColors, String descriptor) {
    List<Map> result = []
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

private Map<String, List> deviceSetHSBKAndPower(com.hubitat.app.DeviceWrapper device, Number duration, Map<String, Object> hsbkMap, boolean displayed, String power = 'on') {
    def actions = makeActions()
    if (hsbkMap) {
        actions.commands << makeCommand('LIGHT.SET_COLOR', [color: hsbkMap, duration: (hsbkMap.duration ?: 0) * 1000])
        actions.events = actions.events + makeColorMapEvents(hsbkMap, displayed)
    }

    if (null != power && device.currentSwitch != power) {
        def powerLevel = 'on' == power ? 65535 : 0
        actions.commands << makeCommand('LIGHT.SET_POWER', [powerLevel: powerLevel, duration: duration * 1000])
        actions.events << [name: "switch", value: power, displayed: displayed, data: [syncing: "false"]]
    }

    actions
}

private List makeColorMapEvents(Map hsbkMap, Boolean displayed) {
    List<Map> events = []
    if (hsbkMap.hue || hsbkMap.saturation) {
        events << [name: 'colorMode', value: 'RGB', displayed: displayed]
        hsbkMap.hue ? events << [name: 'hue', value: scaleDown100(hsbkMap.hue), displayed: displayed] : null
        hsbkMap.saturation ? events << [name: 'saturation', value: scaleDown100(hsbkMap.saturation), displayed: displayed] : null
        hsbkMap.brightness ? events << [name: 'level', value: scaleDown100(hsbkMap.brightness), displayed: displayed] : null
        events << [name: 'RGB', value: hsbkMap.RGB ?: hsvToRgbString(scaleDown100(hsbkMap.hue), scaleDown100(hsbkMap.saturation), scaleDown100(hsbkMap.brightness)), displayed: displayed]
    } else if (hsbkMap.kelvin) {
        events << [name: 'colorMode', value: 'CT', displayed: displayed]
        events << [name: 'colorTemperature', value: hsbkMap.kelvin as Integer, displayed: displayed]
        hsbkMap.brightness ? events << [name: 'level', value: scaleDown100(hsbkMap.brightness), displayed: displayed] : null
    }

    events << [name: 'colorName', value: hsbkMap.name ?: 'Unknown', displayed: displayed]
    events
}

private Map getScaledColorMap(Map colorMap) {
    def result = [:]
    def brightness = colorMap.level ?: colorMap.brightness

    colorMap.hue instanceof Integer ? result.hue = scaleUp100(colorMap.hue) as Integer : null
    colorMap.saturation instanceof Integer ? result.saturation = scaleUp100(colorMap.saturation) as Integer : null
    brightness instanceof Integer ? result.brightness = scaleUp100(brightness) as Integer : null
    colorMap.kelvin instanceof Integer ? result.kelvin = colorMap.kelvin : null
    result
}

private Map makeCommand(String command, Map payload) {
    [cmd: command, payload: payload]
}

private Map<String, List> makeActions() {
    [commands: [], events: []]
}

private Map<String, Object> getCurrentHSBK(com.hubitat.app.DeviceWrapper theDevice) {
    [
            hue       : scaleUp(theDevice.currentHue ?: 0, 100),
            saturation: scaleUp(theDevice.currentSaturation ?: 0, 100),
            brightness: scaleUp(theDevice.currentLevel as Long, 100),
            kelvin    : theDevice.currentcolorTemperature
    ]
}

/** Scaling */
private Float scaleDown100(value) {
    scaleDown(value, 100)
}

private Long scaleUp100(value) {
    scaleUp(value, 100)
}

private Float scaleDown(value, maxValue) {
    Float result = ((value * maxValue) / 65535)
    result.round(2)
}

private Long scaleUp(value, maxValue) {
    (value * 65535) / maxValue
}

private Map parseHeader(Map deviceParams) {
    List<Map> headerDescriptor = makeDescriptor('size:w,misc:w,source:i,target:ba8,frame_reserved:ba6,flags:b,sequence:b,protocol_reserved:ba8,type:w,protocol_reserved2:w')
    parseBytes(headerDescriptor, (hubitat.helper.HexUtils.hexStringToIntArray(deviceParams.payload) as List<Long>).each {
        it & 0xff
    })
}

private void createDeviceDefinition(Map parsed, String ip, String mac) {
    List<Map> stateVersionDescriptor = makeDescriptor('vendor:i,product:i,version:i')
    def version = parseBytes stateVersionDescriptor, parsed.remainder as List<Long>
    def device = deviceVersion version
    device['ip'] = ip
    device['mac'] = mac
    saveDeviceDefinition device
}

private Map getDeviceDefinition(String mac) {
    Map devices = getDeviceDefinitions()

    devices[mac]
}

private void clearDeviceDefinitions() {
    atomicState.devices = [:]
}

public Map getDeviceDefinitions() {
    if (atomicState.devices == null) {
        atomicState.devices = [:]
    }

    atomicState.devices
}

private void saveDeviceDefinitions(Map devices) {
    atomicState.devices = devices
}

private void saveDeviceDefinition(Map device) {
    Map devices = getDeviceDefinitions()

    devices[device.mac] = device

    saveDeviceDefinitions devices
}

private void deleteDeviceDefinition(Map device) {
    Map devices = getDeviceDefinitions()

    devices.remove device.mac

    saveDeviceDefinitions devices
}

private String updateDeviceDefinition(String mac, String ip, Map properties) {
    Map device = getDeviceDefinition mac
    if (!device) {
        // perhaps it's a real device?
        return getChildDevice(ip)
    }
    properties.each { key, val -> (device[key] = val) }

    isDeviceComplete(device) ? makeRealDevice(device) : saveDeviceDefinition(device)
    null
}

private List knownDeviceLabels() {
    getKnownIps().values().each { officialDeviceName(it.label) }.asList()
}

private void makeRealDevice(Map device) {
    addToKnownIps device
    try {
        addChildDevice(
                'robheyes',
                device.deviceName,
                device.ip,
                null,
                [
                        group   : device.group,
                        label   : device.label,
                        location: device.location
                ]
        )
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

private String officialDeviceName(String name) {
    return deviceNamePrefix() + name
}

private void addToKnownIps(Map device) {
    def knownIps = getKnownIps()
    knownIps[device.ip as String] = device
    atomicState.knownIps = knownIps
}

private void clearKnownIps() {
    atomicState.knownIps = [:]
}

private void clearKnownIpsWithErrors() {
    Map<String, Map> ips = atomicState.knownIps
    ips = ips.findAll {
        k, v ->
            !v.containsKey('error')
    }
    atomicState.knownIps = ips
}

private Map<String, Map> getKnownIps() {
    atomicState.knownIps ?: [:]
}

private Boolean isDeviceComplete(Map device) {
    List missing = matchKeys device, ['ip', 'mac', 'group', 'label', 'location']
    missing.isEmpty()
}

private List matchKeys(Map device, List<String> expected) {
    def result = []
    expected.each {
        if (!device.containsKey(it)) {
            result << it
        }
    }
    result
}

private String convertIpLong(String ip) {
    sprintf '%d.%d.%d.%d', hubitat.helper.HexUtils.hexStringToIntArray(ip)
}

private String applySubscript(String descriptor, Number subscript) {
    descriptor.replace('!', subscript.toString())
}


private Map deviceVersion(Map device) {
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
        case 62:
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
        case 63:
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

/** Color related */
private Map rgbToHSV(red = 255, green = 255, blue = 255, resolution = "low") {
    // Takes RGB (0-255) and returns HSV in 0-360, 0-100, 0-100
    // resolution ("low", "high") will return a hue between 0-100, or 0-360, respectively.
    List<Float> hsv = hubitat.helper.ColorUtils.rgbToHSV([red, green, blue])
    def hsvMap = [h: hsv[0] * (resolution == 'high' ? 3.6d : 1d), s: hsv[1], v: hsv[2]]
    return hsvMap
}

private String hsvToRgbString(hue, saturation, level) {
    def rgb = hubitat.helper.ColorUtils.hsvToRGB([hue, saturation, level])
    return hubitat.helper.ColorUtils.rgbToHEX(rgb)
}

private Map hexToColor(String hex) {
    List<Integer> rgbList = hubitat.helper.ColorUtils.hexToRGB(hex)
    return [r: rgbList[0], g: rgbList[1], b: rgbList[2]]
}

/** Device parsing */
private Map parseDeviceParameters(String description) {
    def deviceParams = [:]
    description.findAll(~/(\w+):(\w+)/) {
        (deviceParams[it[1]] = it[2])
    }
    deviceParams
}

private Map parseHeaderFromDescription(String description) {
    parseHeader parseDeviceParameters(description)
}

private Map parsePayload(String deviceAndType, Map header) {
    parseBytes descriptors[deviceAndType], getRemainder(header)
}

private Map parseBytes(String descriptor, List<Long> bytes) {
    parseBytes makeDescriptor(descriptor), bytes
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
        assert (data.size() <= totalLength)

        if (item.isArray) {
            def itemSize = item.size as Number
            def numItems = data.size().intdiv(itemSize)
            nextOffset = offset + numItems * item.size // NB this only works if the variable length array is at the end
            def subMap = [:]
            for (int i = 0; i < numItems; i++) {
                def startOffset = i * itemSize
                def endOffset = (i + 1) * itemSize
                processSegment subMap, data.subList(startOffset, endOffset), item, i, true
            }
            result.put item.name, subMap
        } else {
            processSegment result, data, item, item.name
        }
        offset = nextOffset
    }
    if (offset < bytes.size()) {
        result.put 'remainder', bytes[offset..-1]
    }
    return result
}

private void processSegment(Map result, List<Long> data, Map item, name, boolean logIt = false) {

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

private List makePayload(String deviceAndType, Map payload) {
    def descriptor = makeDescriptor(descriptors[deviceAndType])
    def result = []
    descriptor.each {
        Map item ->
            if ('H' == item.kind) {
                if (item.isArray) {
                    for (int i = 0; i < item.count; i++) {
                        Map hsbk = payload.colors[i] as Map
                        add result, (hsbk['hue'] ?: 0) as short
                        add result, (hsbk['saturation'] ?: 0) as short
                        add result, (hsbk['brightness'] ?: 0) as short
                        add result, (hsbk['kelvin'] ?: 0) as short
                    }
                } else {
                    add result, (payload.color['hue'] ?: 0) as short
                    add result, (payload.color['saturation'] ?: 0) as short
                    add result, (payload.color['brightness'] ?: 0) as short
                    add result, (payload.color['kelvin'] ?: 0) as short
                }
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

private List<Long> getRemainder(header) { header.remainder as List<Long> }

private Number itemLength(String kind) {
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

private List<Map> makeDescriptor(String desc) {
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

private String getHubIP() {
    def hub = location.hubs[0]

    hub.localIP
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

private Byte sequenceNumber() {
    atomicState.sequence = ((atomicState.sequence ?: 0) + 1) % 128
}

/** Protocol packet building */

private def createFrame(List buffer, boolean tagged) {
    add buffer, 0 as short
    add buffer, 0x00 as byte
    add buffer, (tagged ? 0x34 : 0x14) as byte
    add buffer, lifxSource()
}

private int lifxSource() {
    0x48454C44 // = HELD: Hubitat Elevation LIFX Device :)
}

private def createFrameAddress(List buffer, byte[] target, boolean ackRequired, boolean responseRequired, Byte sequenceNumber) {
    add buffer, target
    add buffer, 0 as short
    fill buffer, 0 as byte, 6
    add buffer, ((ackRequired ? 0x02 : 0) | (responseRequired ? 0x01 : 0)) as byte
    add buffer, sequenceNumber as byte
}

private def createProtocolHeader(List buffer, short messageType) {
    fill buffer, 0 as byte, 8
    add buffer, messageType
    add buffer, 0 as short
}

private def createPayload(List buffer, byte[] payload) {
    add buffer, payload
}

/** LOW LEVEL BUFFER FILLING */
private void add(List buffer, byte value) {
    buffer.add Byte.toUnsignedInt(value)
}

private void add(List buffer, short value) {
    def lower = value & 0xff
    add buffer, lower as byte
    add buffer, ((value - lower) >>> 8) as byte
}

private void add(List buffer, int value) {
    def lower = value & 0xffff
    add buffer, lower as short
    add buffer, Integer.divideUnsigned(value - lower, 0x10000) as short
}

private void add(List buffer, long value) {
    def lower = value & 0xffffffff
    add buffer, lower as int
    add buffer, Long.divideUnsigned(value - lower, 0x100000000) as int
}

private void add(List buffer, byte[] values) {
    for (value in values) {
        add buffer, value
    }
}

private void add(List buffer, List other) {
    for (value in other) {
        add buffer, value
    }
}

private void fill(List buffer, byte value, int count) {
    for (int i = 0; i < count; i++) {
        add buffer, value
    }
}

private void put(List buffer, int index, byte value) {
    buffer.set index, Byte.toUnsignedInt(value)
}

private void put(List buffer, int index, short value) {
    def lower = value & 0xff
    put buffer, index, lower as byte
    put buffer, index + 1, ((value - lower) >>> 8) as byte
}

/** LOGGING **/
private void logDebug(msg) {
    log.debug msg
}

private void logInfo(msg) {
    log.info msg
}

private void logWarn(String msg) {
    log.warn msg
}

private List<Map> getFullColorMap() {
    colorMap.collect { expandRgb it }
}

@Lazy @Field List<Map> fullColorMap = getFullColorMap()

/** Many of these colours are taken from https://encycolorpedia.com/named */
@Field static final List<Map> colorMap =
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
                [name: "B'dazzled Blue", rgb: '#2E5894'],
                [name: 'Baby Blue', rgb: '#89CFF0'],
                [name: 'Baby Blue Eyes', rgb: '#A1CAF1'],
                [name: 'Baby Pink', rgb: '#F4C2C2'],
                [name: 'Baby Powder', rgb: '#FEFEFA'],
                [name: 'Baiko Brown', rgb: '#857C55'],
                [name: 'Baker-Miller Pink', rgb: '#FF91AF'],
                [name: 'Ball Blue', rgb: '#21ABCD'],
                [name: 'Banana Mania', rgb: '#FAE7B5'],
                [name: 'Banana Yellow', rgb: '#FFE135'],
                [name: 'Bangladesh Green', rgb: '#006A4E'],
                [name: 'Barbie Pink', rgb: '#E94196'],
                /* omitted lots of other Barbie Pink shades*/
                [name: 'Barn Red', rgb: '#7C0A02'],
                [name: 'Battery Charged Blue', rgb: '#1DACD6'],
                [name: 'Battleship Grey', rgb: '#848482'],
                [name: 'Bayside', rgb: '#5FC9BF'],

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
                [name: 'Warm White', rgb: '#B0B893'],
                [name: 'Wheat', rgb: '#F5DEB3'],
                [name: 'White', rgb: '#FFFFFF'],
                [name: 'White Smoke', rgb: '#F5F5F5'],
                [name: 'Yellow', rgb: '#FFFF00'],
                [name: 'Yellow Green', rgb: '#9ACD32'],
        ]

/** Create a map of types by the name */
private Map flattenMessageTypes() {
    def result = [:]
    msgTypes.each {
        k, v ->
            v.each {
                k2, v2 ->
                    result["$k.$k2"] = v2
            }
    }

    result
}

private Map flattenedTypes() {
    flattenMessageTypes().collectEntries { k, v -> [(k): v.type] }
}

private Map flattenedDescriptors() {
    flattenMessageTypes().collectEntries { k, v -> [(k): v.descriptor] }
}

@Lazy @Field Map<String, Integer> messageType = flattenedTypes()
@Lazy @Field Map<String, String> descriptors = flattenedDescriptors()


@Field static final Map msgTypes = [
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
                SET_EXTENDED_COLOR_ZONES  : [type: 510, descriptor: 'duration:i;apply:b;index:w;colors_count:b;colors:ha82'],
                GET_EXTENDED_COLOR_ZONES  : [type: 511, descriptor: ''],
                STATE_EXTENDED_COLOR_ZONES: [type: 512, descriptor: 'zone_count:w;index:w;colors_count:b;colors:ha82'],
        ]
]

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
    def rgb = hsvToRgbString(
            scaleDown100(item.hue as Long),
            scaleDown100(item.saturation as Long),
            scaleDown100(item.brightness as Long)
    )
    "$rgb"
}


Map stringToHsbk(String data) {
    def m = data =~ /^(\p{XDigit}{4})(\p{XDigit}{4})(\p{XDigit}{4})(\p{XDigit}{4})(\p{XDigit}{0,2})$/
    if (m) {
        def hue = Long.parseLong(m.group(1), 16)
        def sat = Long.parseLong(m.group(2), 16)
        def bri = Long.parseLong(m.group(3), 16)
        def kel = Long.parseLong(m.group(4), 16)
        def count = 1
        if (m.group(5)) {
            count = Integer.parseInt(m.group(5), 16)
        }
        [hue: hue, saturation: sat, brightness: bri, kelvin: kel, count: count]
    }
}

List<String> unpack(String packed) {
    def matcher = packed =~ /\p{XDigit}{16}/
    List<Map> result = matcher[0..-1].collect() {
        it as String
    }
    result
}

List<String> unRle(String compressed) {
    def matcher = compressed =~ /\p{XDigit}{18}/
    List<String> temp = matcher[0..-1].collect() {
        it as String
    }
    List<String> result = []
    temp.each {
        def value = it.substring(0, 16)
        def count = Integer.parseInt(it.substring(16), 16)
        for (int i = 0; i < count; i++) {
            result << value
        }
    }
    result
}

List<String> decompress(String compressed) {
    if (compressed.startsWith('@')) {
        unpack(compressed.substring(1))
    } else if (compressed.startsWith('*')) {
        unRle(compressed.substring(1))
    } else {
        []
    }
}

Map getZones(String compressed) {
    List<Map> colors = decompress(compressed).collect { stringToHsbk(it) }
    def numZones = colors.size()
    while (colors.size() < 82) {
        colors << [hue: 0, saturation: 0, brightness: 0, kelvin: 0]
    }
    def realColors = [:]
    colors.eachWithIndex { v, k -> realColors[k] = v }
    [index: 0, zone_count: numZones, colors_count: numZones, colors: realColors]
}


