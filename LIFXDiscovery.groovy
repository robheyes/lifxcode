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

@Field Integer extraProbesPerPass = 2

metadata {
    definition(name: 'LIFX Discovery', namespace: 'robheyes', author: 'Robert Alan Heyes') {
        capability "Refresh"

        attribute 'lifxdiscovery', 'string'
        attribute 'progress', 'number'
    }

    preferences {
        input "logEnable", "bool", title: "Enable debug logging", required: false
    }
}

def updated() {
    log.debug "LIFX updating"
}

def installed() {
    log.debug "LIFX Discovery installed"
    initialize()
}

def initialize() {
    def discoveryType = parent.discoveryType()
    logDebug "Dscovery type $discoveryType"
    if (discoveryType == 'discovery') {
        refresh()
    } else if (discoveryType == 'refresh') {
        rescanNetwork()
    } else {
        logDebug "Unexpected discovery type $discoveryType"
    }
}

def refresh() {
    String subnet = parent.getSubnet()
    if (!subnet) {
        return
    }
    parent.clearCachedDescriptors()
    def scanPasses = parent.maxScanPasses()
    1.upto(scanPasses) {
        logDebug "Scanning pass $it of $scanPasses"
        parent.setScanPass(it)
        scanNetwork subnet, it
        logDebug "Pass $it complete"
    }
    parent.setScanPass 'DONE'
    sendEvent name: 'lifxdiscovery', value: 'complete'
}

private scanNetwork(String subnet, int pass) {
    1.upto(254) {
        def ipAddress = subnet + it
        if (!parent.isKnownIp(ipAddress)) {
            1.upto(pass + extraProbesPerPass) {
//            1.upto(2) {
                sendCommand ipAddress, messageTypes().DEVICE.GET_VERSION.type as int, true, 1, it % 128 as Byte
            }
        }
//        sendEvent name: 'progress', value: ((100 * it) / 255).toString()
    }
}

def rescanNetwork() {
    logDebug "Rescan network"
    String subnet = parent.getSubnet()
    if (!subnet) {
        return
    }
    logDebug "Rescanning..."
    1.upto(254) {
        def ipAddress = subnet + it
        if (parent.isKnownIp(ipAddress)) {
            sendCommand ipAddress, messageTypes().DEVICE.GET_GROUP.type as int, true, 1, it % 128 as Byte
        }
    }
    parent.setScanPass 'DONE'
    sendEvent name: 'lifxdiscovery', value: 'complete'
}

private void sendCommand(String ipAddress, int messageType, boolean responseRequired = true, int pass = 1, Byte sequence = 0) {
    def buffer = []
    parent.makePacket buffer, [0, 0, 0, 0, 0, 0] as byte[], messageType, false, responseRequired, [], sequence
    def rawBytes = parent.asByteArray(buffer)
    String stringBytes = hubitat.helper.HexUtils.byteArrayToHexString rawBytes
    sendPacket ipAddress, stringBytes
    pauseExecution(parent.interCommandPauseMilliseconds(pass))
}

def parse(String description) {
    Map<String, List> command = parent.discoveryParse(description)
    sendActions command
//    command != null ? sendCommand(command.ip as String, command.type as int) : null
}

private void sendActions(Map<String, List> actions) {
    actions.commands?.eachWithIndex { item, index -> sendCommand item.ip as String, item.type as int, true, 1, index as Byte }
    actions.events?.each { sendEvent it }
}

private void sendCommand(String ip, String deviceAndType, Map payload = [:], boolean responseRequired = true, boolean ackRequired = false, Byte index = 0) {
//    resendUnacknowledgedCommand()
    def parts = deviceAndType.split(/\./)
    def buffer = []
    byte sequence = parent.makePacket buffer, parts[0], parts[1], payload, responseRequired, ackRequired, index
//    if (ackRequired) {
//        parent.expectAckFor device, sequence, buffer
//    }
    sendPacket buffer
}

private void sendPacket(List buffer) {
    String stringBytes = hubitat.helper.HexUtils.byteArrayToHexString parent.asByteArray(buffer)
    sendHubCommand(
            new hubitat.device.HubAction(
                    stringBytes,
                    hubitat.device.Protocol.LAN,
                    [
                            type              : hubitat.device.HubAction.Type.LAN_TYPE_UDPCLIENT,
                            destinationAddress: myIp() + ":56700",
                            encoding          : hubitat.device.HubAction.Encoding.HEX_STRING
                    ]
            )
    )
}

Map<String, Map<String, Map>> messageTypes() {
    parent.messageTypes()
}

private void sendPacket(String ipAddress, String bytes, boolean wantLog = false) {
    if (wantLog) {
        logDebug "sending bytes: ${stringBytes} to ${ipAddress}"
    }
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
                            encoding          : hubitat.device.HubAction.Encoding.HEX_STRING
                    ]
            )
    )
}

private Integer getTypeFor(String dev, String act) {
    parent.getTypeFor dev, act
}

static byte[] asByteArray(List buffer) {
    (buffer.each { it as byte }) as byte[]
}

private void logDebug(msg) {
    log.debug "DISCOVERY: $msg"
}

private void logInfo(msg) {
    log.info msg
}

private void logWarn(String msg) {
    log.warn msg
}
