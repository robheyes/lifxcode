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

definition(name: "LIFX discovery", namespace: "robheyes", author: "Robert Alan Heyes") {
//	capability: "Switch"
    capability "Polling"
    capability "Refresh"
    command "refresh"
    command "removeChildren"
}

preferences {
    input "logEnable", "bool", title: "Enable debug logging", required: false
    //input "refreshBtn", "button", title: "Refresh"
}

//enum States{ INITIALISING, DISCOVERING, POLLING, OPERATING}

@Field List<Map> headerDescriptor = getDescriptor('size:2l,misc:2l,source:4l,target:8a,frame_reserved:6a,flags:1,sequence:1,protocol_reserved:8a,type:2l,protocol_reserved2:2')
@Field List<Map> stateVersionDescriptor = getDescriptor('vendor:4l,product:4l,version:4l')
@Field String currentState = 'DISCOVERING'
@Field List<Map> devicesFound = []

def updated() {
    log.debug "LIFX updating"
    initialize()
}

def installed() {
    log.debug "LIFX installed"
    initialize()
}

def initialize() {
    state.sequence = 1
    state.deviceCount = 0
    def localIP = getHubIP()

//    log.debug "localIP: ${localIP}"
    refresh()
}

def refresh() {
    removeChildren()
    String subnet = getSubnet()
    if (!subnet) {
        return
    }
    currentState = 'DISCOVERING'
    1.upto(254) {
        def packet = makeVersionPacket([0, 0, 0, 0, 0, 0] as byte[])
        def ipAddress = subnet + it
        log.debug "Going to test ${ipAddress}"
        sendPacket(packet.buffer, ipAddress)
    }


    // maybe change the state to OPERATING after a period?
}

def removeChildren() {
    log.debug "Removing child devices"
    childDevices.each {
        if (it != null) {
            deleteChildDevice(it.getDeviceNetworkId())
        }
    }
}

def parse(String description) {
//    logDebug("Description = ${description}")
    Map deviceParams = new HashMap()
    description.findAll(~/(\w+):(\w+)/) {
//        logDebug("Storing ${it[2]} at ${it[1]}")
        deviceParams.putAt(it[1], it[2])
    }
//    logDebug("Params ${deviceParams}")
//    theClass = deviceParams.ip.getClass()
//    logDebug("ip ${deviceParams.ip} of ${theClass}")
    ip = convertIpLong(deviceParams.ip as String)
    mac = hubitat.helper.HexUtils.hexStringToIntArray(deviceParams.mac)
//	logDebug("Mac: ${mac}")
    def parsed = parseHeader(deviceParams)
//    logDebug("looking up message type ${parsed.type}")
//    def theType = lookupMessageType(parsed.type)
//    logDebug("Got message of type ${theType}")
    def descriptor = responseDescriptor()[parsed.type] ?: null
    switch (parsed.type) {
        case messageTypes().DEVICE.STATE_VERSION:
            createBasicDevice(parsed, ip, mac)
            break
        case messageTypes().DEVICE.STATE_LABEL:
            def data = parseBytes(getDescriptor('label:32s'), deviceParams.payload)
            logDebug("data = ${data}")
            def devices = devicesFound as LinkedList<Map>
            def device = devices.find { it.ip == ip }
            logDebug("Device is now ${device}")
            device?.label = data.label
            state.devicesFound[ip] = device
            break
        case messageTypes().LIGHT.STATE:
            if (null != descriptor) {
                def data = parseBytes(getDescriptor(descriptor), deviceParams.payload)
                logDebug("State data: ${data}")
            }
            break
        case messageTypes().DEVICE.STATE_GROUP:
            break
        case messageTypes().DEVICE.STATE_LOCATION:
            break
        case messageTypes().DEVICE.STATE_WIFI_INFO:
            break
        case messageTypes().DEVICE.STATE_INFO:
            break
    }
}

Map lookupMessageType(messageType) {
    def result = [name: "unknown message type ${messageType}"]
    messageTypes().each { key, value ->
        value.each {
            kind, type ->
                if (type == messageType) {
                    result = [name: sprintf('%s.%s', [key, kind]), descriptor: responseDescriptor()[type] ?: 'none']
                }
        }

    }
    return result
}

Map parseHeader(Map deviceParams) {
    parseBytes(headerDescriptor, (hubitat.helper.HexUtils.hexStringToIntArray(deviceParams.payload) as List<Long>).each {
        it & 0xff
    })
}

def requestExtraInfo(Map data) {
    def device = data.device
    logDebug("Trying to send a get state to ${device}")
    def packet = makeGetStatePacket()
    //sendPacket(packet.buffer, device.ip)
}

private void createBasicDevice(Map parsed, String ip, int[] mac) {
//            logDebug("It's a state version message")
    log.debug("Creating device for ip address ${ip} and mac ${mac}")
    def version = parseBytes(stateVersionDescriptor, parsed.remainder as List<Long>)
//            logDebug("Version = ${version}")
    def device = deviceVersion(version)
    device.putAt('ip', ip)
    device.putAt('mac', mac)
    logDebug("Device descriptor = ${device}")
    if (null == getChildDevice(device.ip)) {
        addChildDevice('robheyes', device.deviceName, device.ip)
    }
}


String convertIpLong(String ip) {
    sprintf('%d.%d.%d.%d', hubitat.helper.HexUtils.hexStringToIntArray(ip))
}

def poll() {
    logInfo('Polling')
    def packet = makeGetLabelPacket()
    logDebug(packet)
    sendPacket(packet.buffer, "192.168.1.45", true)

    logDebug "Sent packet with sequence ${packet.sequence}"
}


static Map<String, Map<String, Integer>> messageTypes() {
    return [
            DEVICE: [
                    GET_SERVICE        : 2,
                    STATE_SERVICE      : 3,
                    GET_HOST_INFO      : 12,
                    STATE_HOST_INFO    : 13,
                    GET_HOST_FIRMWARE  : 14,
                    STATE_HOST_FIRMWARE: 15,
                    GET_WIFI_INFO      : 16,
                    STATE_WIFI_INFO    : 17,
                    GET_WIFI_FIRMWARE  : 18,
                    STATE_WIFI_FIRMWARE: 19,
                    GET_POWER          : 20,
                    SET_POWER          : 21,
                    STATE_POWER        : 22,
                    GET_LABEL          : 23,
                    SET_LABEL          : 24,
                    STATE_LABEL        : 25,
                    GET_VERSION        : 32,
                    STATE_VERSION      : 33,
                    GET_INFO           : 34,
                    STATE_INFO         : 35,
                    ACKNOWLEDGEMENT    : 45,
                    GET_LOCATION       : 48,
                    SET_LOCATION       : 49,
                    STATE_LOCATION     : 50,
                    GET_GROUP          : 51,
                    SET_GROUP          : 52,
                    STATE_GROUP        : 53,
                    ECHO_REQUEST       : 58,
                    ECHO_RESPONSE      : 59
            ],
            LIGHT : [
                    GET_STATE            : 101,
                    SET_COLOR            : 102,
                    SET_WAVEFORM         : 103,
                    SET_WAVEFORM_OPTIONAL: 119,
                    STATE                : 107,
                    GET_POWER            : 116,
                    SET_POWER            : 117,
                    STATE_POWER          : 118,
                    GET_INFRARED         : 120,
                    STATE_INFRARED       : 121,
                    SET_INFRARED         : 122,
            ]
    ]
}

static Map<Integer, String> responseDescriptor() {
    def hsbk = 'hue:2l,saturation:2l,brightness:2l,kelvin:2l'
    return [
            3  : 'service:1;port:4l',                                               // DEVICE.STATE_SERVICE
            13 : 'signal:4l;tx:4l;rx:4l,reservedHost:2l',                           // DEVICE.STATE_HOST_INFO
            15 : 'build:8l;reservedFirmware:8l;version:4l',                         // DEVICE.STATE_HOST_FIRMWARE
            17 : 'signal:4l;tx:4l;rx:4l,reservedWifi:2l',                           // DEVICE.STATE_WIFI_INFO
            19 : 'build:8l;reservedFirmware:8l;version:4l',                         // DEVICE.STATE_WIFI_FIRMWARE
            22 : 'level:2l',                                                        // DEVICE.STATE_POWER
            25 : 'label:32s',                                                       // DEVICE.STATE_LABEL
            33 : 'vendor:4l;product:4l;version:4l',                                 // DEVICE.STATE_VERSION
            35 : 'time:8l;uptime:8l;downtime:8l',                                   // DEVICE.STATE_INFO
            50 : 'location:16a;label:32s;updated_at:8l',                            // DEVICE.STATE_LOCATION
            53 : 'group:16a;label:32s;updated_at:8l',                               // DEVICE.STATE_GROUP
            59 : 'payload:64a',                                                     // DEVICE._ECHO_RESPONSE
            107: "${hsbk};reserved1State:2l;power:2l;label:32s;reserved2state:8l",  // DEVICE.STATE
            118: 'level:2l',                                                        // DEVICE.STATE_POWER
            121: 'brightness:2l',                                                   // DEVICE.STATE_INFRARED
    ]
}

private def static deviceVersion(Map device) {
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
                    deviceName: 'LIFX+ Color',
                    features  : [color: true, infrared: true, multizone: false, temperature_range: [min: 2500, max: 9000], chain: false]
            ]
        case 30:
        case 46:
            return [
                    name      : 'LIFX+ BR30',
                    deviceName: 'LIFX+ Color',
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

static Map parseBytes(List<Map> descriptor, List<Long> bytes) {
    Map result = new HashMap();
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

@Field Map<String, List<Map>> cachedDescriptors

List<Map> getDescriptor(String desc) {
    if (null == cachedDescriptors) {
        cachedDescriptors = new HashMap<String, List<Map>>()
    }
    List<Map> candidate = cachedDescriptors.get(desc)
    if (candidate) {
        logDebug('Found candidate')
    } else {
        candidate = makeDescriptor(desc)
        cachedDescriptors[desc] = (candidate)
    }
    candidate
}

private static List<Map> makeDescriptor(String desc) {
    desc.findAll(~/(\w+):(\d+)([aAbBlLsS]?)/) {
        full ->
            [
                    endian: full[3].toUpperCase(),
                    bytes : full[2],
                    name  : full[1],
            ]
    }
}

private String getSubnet() {
    def ip = getHubIP()
    def m = ip =~ /^(\d{1,3}\.\d{1,3}\.\d{1,3}\.)\d{1,3}/
    if (!m) {
        logWarn('ip does not match pattern')
        return null
    }
    return m.group(1)
}

private static Long makeTarget(List macAddress) {
    return macAddress.inject(0L) { Long current, Long val -> current * 256 + val }
}

def sendPacket(List buffer, String ipAddress, boolean wantLog = false) {
    def rawBytes = asByteArray(buffer)
//    logDebug "raw bytes: ${rawBytes}"
    String stringBytes = hubitat.helper.HexUtils.byteArrayToHexString(rawBytes)
    if (wantLog) {
        logDebug "sending bytes: ${stringBytes} to ${ipAddress}"
    }
    sendHubCommand(
            new hubitat.device.HubAction(
                    stringBytes,
                    hubitat.device.Protocol.LAN,
                    [
                            type              : hubitat.device.HubAction.Type.LAN_TYPE_UDPCLIENT,
                            destinationAddress: ipAddress + ":56700",
                            encoding          : hubitat.device.HubAction.Encoding.HEX_STRING
                    ]
            )
    )
}

private Map makeGetDevicePacket() {
    def buffer = []
    def getServiceSequence = makePacket(buffer, [0, 0, 0, 0, 0, 0] as byte[], messageTypes().DEVICE.GET_SERVICE, false, true, [])
    return [sequence: getServiceSequence, buffer: buffer]
}

private Map makeEchoPacket(byte[] target) {
    def payload = []
    fill(payload, 0xAA as byte, 64)
    def buffer = []
    def echoSequence = makePacket(buffer, target, messageTypes().DEVICE.ECHO_REQUEST, false, false, payload)
    return [sequence: echoSequence, buffer: buffer]
}

private Map makeVersionPacket(byte[] target) {
    def buffer = []
    def echoSequence = makePacket(buffer, target, messageTypes().DEVICE.GET_VERSION)
    return [sequence: echoSequence, buffer: buffer]
}

Map makeGetLabelPacket() {
    def buffer = []
    def labelSequence = makePacket(buffer, [0, 0, 0, 0, 0, 0] as byte[], messageTypes().DEVICE.GET_LABEL)
    return [sequence: labelSequence, buffer: buffer]
}

Map makeGetStatePacket() {
    def buffer = []
    def stateSequence = makePacket(buffer, [0, 0, 0, 0, 0, 0] as byte[], messageTypes().LIGHT.GET_STATE)
    return [sequence: stateSequence, buffer: buffer]
}

private String getHubIP() {
    def hub = location.hubs[0]

    hub.localIP
}

// fills the buffer with the LIFX packet
byte makePacket(List buffer, byte[] targetAddress, int messageType, Boolean ackRequired = false, Boolean responseRequired = false, List payload = []) {
    def lastSequence = sequenceNumber()
    createFrame(buffer, targetAddress.every { it == 0 })
    createFrameAddress(buffer, targetAddress, ackRequired, responseRequired, lastSequence)
    createProtocolHeader(buffer, messageType as short)
    createPayload(buffer, payload as byte[])

    put(buffer, 0, buffer.size() as short)
    return lastSequence
}

private byte sequenceNumber() {
    state.sequence = (state.sequence + 1) % 128
}

private static def createFrame(List buffer, boolean tagged) {
    add(buffer, 0 as short)
    add(buffer, 0x00 as byte)
    add(buffer, (tagged ? 0x34 : 0x14) as byte)
    add(buffer, lifxSource())
}

private static int lifxSource() {
    0x48454C44 // = HELD: Hubitat Elevation LIFX Device
}

private static def createFrameAddress(List buffer, byte[] target, boolean ackRequired, boolean responseRequired, byte sequenceNumber) {
    add(buffer, target)
    add(buffer, 0 as short)
    fill(buffer, 0 as byte, 6)
    add(buffer, ((ackRequired ? 0x02 : 0) | (responseRequired ? 0x01 : 0)) as byte)
    add(buffer, sequenceNumber)
}

private static def createProtocolHeader(List buffer, short messageType) {
    fill(buffer, 0 as byte, 8)
    add(buffer, messageType)
    add(buffer, 0 as short)
}

private static def createPayload(List buffer, byte[] payload) {
    add(buffer, payload)
}

static byte[] asByteArray(List buffer) {
    (buffer.each { it as byte }) as byte[]
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

private void logDebug(msg) {
    log.debug(msg)
}

private void logInfo(msg) {
    log.info(msg)
}

private void logWarn(String msg) {
    log.warn(msg)
}
