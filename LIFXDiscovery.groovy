/**
 *
 * Copyright 2018, 2019 Robert Heyes. All Rights Reserved
 *
 *  This software if free for Private Use. You may use and modify the software without distributing it.
 *  You may not grant a sublicense to modify and distribute this software to third parties.
 *  Software is provided without warranty and your use of it is at your own risk.
 *
 */

metadata {
    definition(name: 'LIFX Discovery', namespace: 'robheyes', author: 'Robert Alan Heyes') {
        capability "Refresh"
    }

    preferences {
        input "logEnable", "bool", title: "Enable debug logging", required: false
        //input "refreshBtn", "button", title: "Refresh"
    }
}

def updated() {
    log.debug "LIFX updating"
//    initialize()
}

def installed() {
    log.debug "LIFX Discovery installed"
    initialize()
}

def initialize() {
    refresh()
}

def refresh() {
    String subnet = parent.getSubnet()
    if (!subnet) {
        return
    }
    1.upto(3) {
        logDebug "Scanning pass $it"
        scanNetwork(subnet)
    }
}

private scanNetwork(String subnet) {
    1.upto(254) {
        def ipAddress = subnet + it
        sendCommand(ipAddress, messageTypes().DEVICE.GET_VERSION.type as int)
    }
}


private void sendCommand(String ipAddress, int messageType, List payload = [], boolean responseRequired = true) {
    def buffer = []
    parent.makePacket(buffer, [0, 0, 0, 0, 0, 0] as byte[], messageType, false, responseRequired, payload)
    def rawBytes = parent.asByteArray(buffer)
//    logDebug "raw bytes: ${rawBytes}"
    String stringBytes = hubitat.helper.HexUtils.byteArrayToHexString(rawBytes)
    sendPacket(ipAddress, stringBytes)
    pauseExecution(parent.interCommandPauseMilliseconds())
}

def parse(String description) {
    Map deviceParams = parseDeviceParameters(description)
    def ip = parent.convertIpLong(deviceParams.ip as String)
    def parsed = parent.parseHeader(deviceParams)
    final String payload = deviceParams.payload
    final String mac = deviceParams.mac
    switch (parsed.type) {
        case messageTypes().DEVICE.STATE_VERSION.type:
            def existing = parent.getDeviceDefinition(mac)
            if (!existing) {
                parent.createDeviceDefinition(parsed, ip, mac)
                sendCommand(ip, messageTypes().DEVICE.GET_GROUP.type as int)
            }
            break
//        case messageTypes().LIGHT.STATE.type:
//            def desc = lookupDescriptorForDeviceAndType('LIGHT', 'STATE')
//            def data = parseBytes(desc, parsed.remainder as List<Long>)
//            break
        case messageTypes().DEVICE.STATE_LABEL.type:
            def data = parseBytes(lookupDescriptorForDeviceAndType('DEVICE', 'STATE_LABEL'), parsed.remainder as List<Long>)
            parent.updateDeviceDefinition(mac, [label: data.label])
            break
        case messageTypes().DEVICE.STATE_GROUP.type:
            def data = parseBytes(lookupDescriptorForDeviceAndType('DEVICE', 'STATE_GROUP'), parsed.remainder as List<Long>)
            parent.updateDeviceDefinition(mac, [group: data.label])
            sendCommand(ip, messageTypes().DEVICE.GET_LOCATION.type as int)
            break
        case messageTypes().DEVICE.STATE_LOCATION.type:
            def data = parseBytes(lookupDescriptorForDeviceAndType('DEVICE', 'STATE_LOCATION'), parsed.remainder as List<Long>)
            parent.updateDeviceDefinition(mac, [location: data.label])
            sendCommand(ip, messageTypes().DEVICE.GET_LABEL.type as int)
            break
        case messageTypes().DEVICE.STATE_WIFI_INFO.type:
            break
        case messageTypes().DEVICE.STATE_INFO.type:
            break
    }
}

private Map parseDeviceParameters(String description) {
    Map deviceParams = new HashMap()
    description.findAll(~/(\w+):(\w+)/) {
        deviceParams.putAt(it[1], it[2])
    }
    deviceParams
}

Map lookupDeviceAndType(String device, String type) {
    parent.lookupDeviceAndType(device, type)
}

String lookupDescriptorForDeviceAndType(String device, String type) {
    parent.lookupDescriptorForDeviceAndType(device, type)
}

Map parseHeader(Map deviceParams) {
    parent.parseHeader(deviceParams)
}

Map<String, Map<String, Map>> messageTypes() {
    parent.messageTypes()
}


Map parseBytes(String descriptor, List<Long> bytes) {
    parent.parseBytes(descriptor, bytes)
}

Map parseBytes(List<Map> descriptor, List<Long> bytes) {
    parent.parseBytes(descriptor, bytes)
}

List<Map> getDescriptor(String desc) {
    parent.getDescriptor(desc)
}

def sendPacket(String ipAddress, String bytes, boolean wantLog = false) {
    if (wantLog) {
        logDebug "sending bytes: ${stringBytes} to ${ipAddress}"
    }
    broadcast(bytes, ipAddress)
}

private void broadcast(String stringBytes, String ipAddress) {
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

private Integer getTypeFor(String dev, String act) {
    parent.getTypeFor(dev, act)
}

Map makeGetLabelPacket() {
    parent.makeGetLabelPacket()
}

Map makeGetStatePacket() {
    parent.makeGetStatePacket()
}


static byte[] asByteArray(List buffer) {
    (buffer.each { it as byte }) as byte[]
}

private void logDebug(msg) {
    log.debug("DISCOVERY: $msg")
}

private void logInfo(msg) {
    log.info(msg)
}

private void logWarn(String msg) {
    log.warn(msg)
}
