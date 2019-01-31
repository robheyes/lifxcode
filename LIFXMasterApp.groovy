/**
 *
 * Copyright 2018, 2019 Robert Heyes. All Rights Reserved
 *
 *  This software if free for Private Use. You may use and modify the software without distributing it.
 *  You may not grant a sublicense to modify and distribute this software to third parties.
 *  Software is provided without warranty and your use of it is at your own risk.
 *
 */
import groovy.transform.Field
//import com.hubitat.app.ChildDeviceWrapper

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
    page(name: "mainPage", title: "", install: true, uninstall: true) {
        section('Discovery') {
            input(name: "discoverBtn", type: "button", title: 'Discover devices')
        }
    }
}


Integer interCommandPauseMilliseconds() {
    50
}

Integer maxScanTimeSeconds() {
    300
}

Integer maxScanPasses() {
    5
}

def updated() {
    logDebug 'LIFX updating'
}

def installed() {
    logDebug 'LIFX installed'

}

def uninstalled() {
    logDebug 'LIFX uninstalled'
}

def initialize() {
//    refresh()
}


def appButtonHandler(btn) {
    log.debug "appButtonHandler called with ${btn}"
    state.btnCall = btn
    if (state.btnCall == "discoverBtn") {
        refresh()
    }

}

def refresh() {
//    def lifxDiscovery =
    removeChildren()
    String subnet = getSubnet()
    if (!subnet) {
        return
    }
    clearDevices()
    addChildDevice('robheyes', 'LIFX Discovery', 'LIFX Discovery')
   // now schedule processing of the device list after a delay
    runIn(maxScanTimeSeconds(), removeDiscoveryDevice)
}

void removeDiscoveryDevice() {
    logDebug 'Removing LIFX Discovery device'
    deleteChildDevice('LIFX Discovery')
}

void removeChildren() {
    logDebug "Removing child devices"
    childDevices.each {
        if (it != null) {
            deleteChildDevice(it.deviceNetworkId)
        }
    }
}

Map lookupDeviceAndType(String device, String type) {
    return messageTypes()[device][type]
}

String lookupDescriptorForDeviceAndType(String device, String type) {
    return lookupDeviceAndType(device, type).descriptor
}

Map parseHeader(Map deviceParams) {
    List<Map> headerDescriptor = getDescriptor('size:2l,misc:2l,source:4l,target:8a,frame_reserved:6a,flags:1,sequence:1,protocol_reserved:8a,type:2l,protocol_reserved2:2')
    parseBytes(headerDescriptor, (hubitat.helper.HexUtils.hexStringToIntArray(deviceParams.payload) as List<Long>).each {
        it & 0xff
    })
}

void createDeviceDefinition(Map parsed, String ip, String mac) {
//    logDebug("Creating device descriptor for ip address ${ip} and mac ${mac}")
    List<Map> stateVersionDescriptor = getDescriptor('vendor:4l,product:4l,version:4l')
    def version = parseBytes(stateVersionDescriptor, parsed.remainder as List<Long>)
//            logDebug("Version = ${version}")
    def device = deviceVersion(version)
    device.putAt('ip', ip)
    device.putAt('mac', mac)
//    logDebug("Device descriptor = ${device}")
    saveDeviceDefinition(device)
}

Map getDeviceDefinition(String mac) {
    Map devices = getDevices()

    devices.getAt(mac)
}

private void clearDevices() {
    atomicState.devices = new HashMap()
}

private Map getDevices() {
    atomicState.devices
}

private void saveDevices(Map devices) {
    atomicState.devices = devices
}

void updateDeviceDefinition(String mac, Map properties)
{
    Map device = getDeviceDefinition(mac)
    if (device) {
        properties.each {key, val -> device.putAt(key, val)}
        if (isDeviceComplete(device)) {
//            logDebug("Device definition is complete ${device.label}, good to create child device")
            try {
                addChildDevice('robheyes', device.deviceName, device.ip, null, [group: device.group, label: device.label, location: device.location])
                logDebug "Added device ${device.label} of type ${device.deviceName}"
            } catch (IllegalArgumentException) {
//                logDebug('Device already present')
            }
            deleteDeviceDefinition(device)
        } else {
            saveDeviceDefinition(device)
        }
    }
}

Boolean isDeviceComplete(Map device) {
    def missing = matchKeys(device, ['ip', 'mac', 'group', 'label', 'location'])
    return missing.isEmpty()
}

List matchKeys(Map device, List<String> expected) {
    def result = []
    expected.each {
        if (!device.containsKey(it)) {
            result << it
        }
    }
    result
}

void saveDeviceDefinition(Map device) {
//    logDebug("Saving device $device")
    Map devices = getDevices()

    devices[device.mac] = device

    saveDevices(devices)
}

void deleteDeviceDefinition(Map device) {
    Map devices = getDevices()

    devices.remove(device.mac)

    saveDevices(devices)
}


String convertIpLong(String ip) {
    sprintf('%d.%d.%d.%d', hubitat.helper.HexUtils.hexStringToIntArray(ip))
}

Map<String, Map<String, Map>> messageTypes() {
    final def color = 'hue:2l,saturation:2l,level:2l,kelvin:2l'
    final def types = [
            DEVICE: [
                    GET_SERVICE        : [type: 2, descriptor: ''],
                    STATE_SERVICE      : [type: 3, descriptor: 'service:1;port:4l'],
                    GET_HOST_INFO      : [type: 12, descriptor: ''],
                    STATE_HOST_INFO    : [type: 13, descriptor: 'signal:4l;tx:4l;rx:4l,reservedHost:2l'],
                    GET_HOST_FIRMWARE  : [type: 14, descriptor: ''],
                    STATE_HOST_FIRMWARE: [type: 15, descriptor: 'build:8l;reservedFirmware:8l;version:4l'],
                    GET_WIFI_INFO      : [type: 16, descriptor: ''],
                    STATE_WIFI_INFO    : [type: 17, descriptor: 'signal:4l;tx:4l;rx:4l,reservedWifi:2l'],
                    GET_WIFI_FIRMWARE  : [type: 18, descriptor: ''],
                    STATE_WIFI_FIRMWARE: [type: 19, descriptor: 'build:8l;reservedFirmware:8l;version:4l'],
                    GET_POWER          : [type: 20, descriptor: ''],
                    SET_POWER          : [type: 21, descriptor: 'level:2l'],
                    STATE_POWER        : [type: 22, descriptor: 'level:2l'],
                    GET_LABEL          : [type: 23, descriptor: ''],
                    SET_LABEL          : [type: 24, descriptor: 'label:32s'],
                    STATE_LABEL        : [type: 25, descriptor: 'label:32s'],
                    GET_VERSION        : [type: 32, descriptor: ''],
                    STATE_VERSION      : [type: 33, descriptor: 'vendor:4l;product:4l;version:4l'],
                    GET_INFO           : [type: 34, descriptor: ''],
                    STATE_INFO         : [type: 35, descriptor: 'time:8l;uptime:8l;downtime:8l'],
                    ACKNOWLEDGEMENT    : [type: 45, descriptor: ''],
                    GET_LOCATION       : [type: 48, descriptor: ''],
                    SET_LOCATION       : [type: 49, descriptor: 'location:16a;label:32s;updated_at:8l'],
                    STATE_LOCATION     : [type: 50, descriptor: 'location:16a;label:32s;updated_at:8l'],
                    GET_GROUP          : [type: 51, descriptor: ''],
                    SET_GROUP          : [type: 52, descriptor: 'group:16a;label:32s;updated_at:8l'],
                    STATE_GROUP        : [type: 53, descriptor: 'group:16a;label:32s;updated_at:8l'],
                    ECHO_REQUEST       : [type: 58, descriptor: 'payload:64a'],
                    ECHO_RESPONSE      : [type: 59, descriptor: 'payload:64a'],
            ],
            LIGHT : [
                    GET_STATE            : [type: 101, descriptor: ''],
                    SET_COLOR            : [type: 102, descriptor: "reservedColor:1;${color};duration:4l"],
                    SET_WAVEFORM         : [type: 103, descriptor: "reservedWaveform:1;transient:1;${color};period:4l;cycles:4l;skew_ratio:2l;waveform:1"],
                    SET_WAVEFORM_OPTIONAL: [type: 119, descriptor: "reservedWaveform:1;transient:1;${color};period:4l;cycles:4l;skew_ratio:2l;waveform:1;set_hue:1;set_saturation:1;set_brightness:1;set_kelvin:1"],
                    STATE                : [type: 107, descriptor: "${color};reserved1State:2l;power:2l;label:32s;reserved2state:8l"],
                    GET_POWER            : [type: 116, descriptor: ''],
                    SET_POWER            : [type: 117, descriptor: 'level:2l;duration:4l'],
                    STATE_POWER          : [type: 118, descriptor: 'level:2l'],
                    GET_INFRARED         : [type: 120, descriptor: ''],
                    STATE_INFRARED       : [type: 121, descriptor: 'brightness:2l'],
                    SET_INFRARED         : [type: 122, descriptor: 'brightness:2l'],
            ]
    ]
    return types
}

Map deviceVersion(Map device) {
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
                    deviceName: 'LIFX Z',
                    features  : [color: true, infrared: false, multizone: true, temperature_range: [min: 2500, max: 9000], chain: false]
            ]
        case 32:
            return [
                    name      : 'LIFX Z 2',
                    deviceName: 'LIFX Z',
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
                    deviceName: 'LIFX Beam',
                    features  : [color: true, infrared: false, multizone: true, temperature_range: [min: 2500, max: 9000], chain: true]
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
                    features  : [color: false, infrared: false, multizone: false, temperature_range: [min: 1500, max: 4000], chain: true]
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
                    features  : [color: true, infrared: false, multizone: false, temperature_range: [min: 2500, max: 9000], chain: true]
            ]
        default:
            return [name: "Unknown LIFX device with product id ${device.product}"]
    }
}

Map parseBytes(String descriptor, List<Long> bytes) {
//    logDebug("Looking for descriptor for ${descriptor}")
    def realDescriptor = getDescriptor(descriptor)
    return parseBytes(realDescriptor, bytes)
}

Map parseBytes(List<Map> descriptor, List<Long> bytes) {
    Map result = new HashMap()
    int offset = 0
    descriptor.each { item ->
        int nextOffset = offset + (item.bytes as int)

        List<Long> data = bytes.subList(offset, nextOffset)
        assert (data.size() == item.bytes as int)
        offset = nextOffset
        // assume big endian for now
        if ('A' == item.endian) {
            result.put(item.name, data)
            return result
        }
        if ('S' == item.endian) {
            result.put(item.name, new String((data.findAll { it != 0 }) as byte[]))
            return result
        }
        if ('B' != item.endian) {
            data = data.reverse()
        }

        BigInteger value = 0
        data.each { value = (value * 256) + it }
        switch (item.bytes) {
            case 1:
                result.put(item.name, (value & 0xFF) as byte)
                break
            case 2:
                result.put(item.name, (value & 0xFFFF) as short)
                break
            case 3: case 4:
                result.put(item.name, (value & 0xFFFFFFFF) as int)
                break
            default: // this should complain if longer than 8 bytes
                result.put(item.name, (value & 0xFFFFFFFFFFFFFFFF) as long)
        }
    }
    if (offset < bytes.size()) {
        result.put('remainder', bytes[offset..-1])
    }
    return result
}

List makePayload(String device, String type, Map payload)
{
    def descriptorString = lookupDescriptorForDeviceAndType(device, type)
    def descriptor = getDescriptor(descriptorString)
    def result = []
    descriptor.each {
        Map item ->
            def value = payload[item.name] ?: 0
            switch (item.bytes as int) {
                case 1:
                    logDebug('length 1')
                    add(result, value as byte)
//                    result.add((value & 0xFF) as byte)
                    break
                case 2:
                    add(result, value as short)
//                    result.add((value & 0xFFFF) as short)
                    break
                case 3: case 4:
                    add(result, value as int)
//                    result.add((value & 0xFFFFFFFF) as int)
                    break
                default: // this should complain if longer than 8 bytes
                    add (result, value as long)
//                    result.add((value & 0xFFFFFFFFFFFFFFFF) as long)
            }
            def bytesResult = result as List<Byte>
    }
    result as List<Byte>
}

List<Long> getRemainder(header) {
    header.remainder as List<Long>
}

List<Map> getDescriptor(String desc) {
    def cachedDescriptors = atomicState.cachedDescriptors
    if (null == cachedDescriptors) {
        cachedDescriptors = new HashMap<String, List<Map>>()
    }
    List<Map> candidate = cachedDescriptors.get(desc)
    if (!candidate) {
        candidate = makeDescriptor(desc)
        cachedDescriptors[desc] = (candidate)
        atomicState.cachedDescriptors = cachedDescriptors
    }
    candidate
}

private static List<Map> makeDescriptor(String desc) {
    desc.findAll(~/(\w+):(\d+)([aAbBlLsS]?)/) {
        full ->
            [
                    endian: full[3].toUpperCase(),
                    bytes : full[2].toInteger(),
                    name  : full[1],
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

def /* Hub */ getHub() {
    location.hubs[0]
}

static Integer getTypeFor(String dev, String act) {
    lookupDeviceAndType(dev, act).type as Integer
}

private Map makeEchoPacket(byte[] target) {
    def payload = []
    fill(payload, 0xAA as byte, 64)
    def buffer = []
    def echoSequence = makePacket(buffer, target, getTypeFor('DEVICE', 'ECHO_REQUEST'), false, false, payload)
    return [sequence: echoSequence, buffer: buffer]
}

private Map makeVersionPacket(byte[] target) {
    def buffer = []
    def echoSequence = makePacket(buffer, target, getTypeFor('DEVICE', 'GET_VERSION'))
    return [sequence: echoSequence, buffer: buffer]
}

Map makeGetLabelPacket() {
    def buffer = []
    def labelSequence = makePacket(buffer, getTypeFor('DEVICE', 'GET_LABEL'))
    return [sequence: labelSequence, buffer: buffer]
}

Map makeGetStatePacket() {
    def buffer = []
    def stateSequence = makePacket(buffer, getTypeFor('LIGHT', 'GET_STATE'))
    return [sequence: stateSequence, buffer: buffer]
}

String getHubIP() {
    def hub = location.hubs[0]

    hub.localIP
}

byte makePacket(List buffer, int messageType, Boolean responseRequired = false, List payload = []) {
    makePacket(buffer, [0, 0, 0, 0, 0, 0] as byte[], messageType, false, responseRequired, payload)
}

byte makePacket(List buffer, String device, String type, Map payload, Boolean responseRequired = true) {
    def listPayload = makePayload(device, type, payload)
    int messageType = lookupDeviceAndType(device, type).type
    logDebug("List payload is $listPayload")
    makePacket(buffer, [0, 0, 0, 0, 0, 0] as byte[], messageType, false, responseRequired, listPayload)
}

// fills the buffer with the LIFX packet and returns the sequence number
byte makePacket(List buffer, byte[] targetAddress, int messageType, Boolean ackRequired = false, Boolean responseRequired = false, List payload = []) {
    def lastSequence = sequenceNumber()
    createFrame(buffer, targetAddress.every { it == 0 })
    createFrameAddress(buffer, targetAddress, ackRequired, responseRequired, lastSequence)
    createProtocolHeader(buffer, messageType as short)
    createPayload(buffer, payload as byte[])

    put(buffer, 0, buffer.size() as short)
    return lastSequence
}



byte[] asByteArray(List buffer) {
    (buffer.each { it as byte }) as byte[]
}

byte sequenceNumber() {
    atomicState.sequence = ((atomicState.sequence ?: 0) + 1) % 128
}

static def createFrame(List buffer, boolean tagged) {
    add(buffer, 0 as short)
    add(buffer, 0x00 as byte)
    add(buffer, (tagged ? 0x34 : 0x14) as byte)
    add(buffer, lifxSource())
}

static int lifxSource() {
    0x48454C44 // = HELD: Hubitat Elevation LIFX Device :)
}

static def createFrameAddress(List buffer, byte[] target, boolean ackRequired, boolean responseRequired, byte sequenceNumber) {
    add(buffer, target)
    add(buffer, 0 as short)
    fill(buffer, 0 as byte, 6)
    add(buffer, ((ackRequired ? 0x02 : 0) | (responseRequired ? 0x01 : 0)) as byte)
    add(buffer, sequenceNumber)
}

static def createProtocolHeader(List buffer, short messageType) {
    fill(buffer, 0 as byte, 8)
    add(buffer, messageType)
    add(buffer, 0 as short)
}

static def createPayload(List buffer, byte[] payload) {
    add(buffer, payload)
}

static void add(List buffer, byte value) {
    buffer.add(Byte.toUnsignedInt(value))
}

static void put(List buffer, int index, byte value) {
    buffer.set(index, Byte.toUnsignedInt(value))
}

static void add(List buffer, short value) {
    def lower = value & 0xff
    add(buffer, lower as byte)
    add(buffer, ((value - lower) >>> 8) as byte)
}

static void put(List buffer, int index, short value) {
    def lower = value & 0xff
    put(buffer, index, lower as byte)
    put(buffer, index + 1, ((value - lower) >>> 8) as byte)
}

static void add(List buffer, int value) {
    def lower = value & 0xffff
    add(buffer, lower as short)
    add(buffer, Integer.divideUnsigned(value - lower, 0x10000) as short)
}

static void add(List buffer, long value) {
    def lower = value & 0xffffffff
    add(buffer, lower as int)
    add(buffer, Long.divideUnsigned(value - lower, 0x100000000) as int)
}

static void add(List buffer, byte[] values) {
    for (value in values) {
        add(buffer, value)
    }
}

static void fill(List buffer, byte value, int count) {
    for (int i = 0; i < count; i++) {
        add(buffer, value)
    }
}

void logDebug(msg) {
    log.debug(msg)
}

void logInfo(msg) {
    log.info(msg)
}

void logWarn(String msg) {
    log.warn(msg)
}
