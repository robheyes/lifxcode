/** THIS IS DEPRECATED */

//
//import groovy.transform.Field
//
///**
// *
// *  Copyright 2019 Robert Heyes. All Rights Reserved
// *
// *  This software is free for Private Use. You may use and modify the software without distributing it.
// *  If you make a fork, and add new code, then you should create a pull request to add value, there is no
// *  guarantee that your pull request will be merged.
// *
// *  You may not grant a sublicense to modify and distribute this software to third parties without permission
// *  from the copyright holder
// *  Software is provided without warranty and your use of it is at your own risk.
// *
// */
//
//@Field Integer extraProbesPerPass = 2
//
//metadata {
//    definition(name: 'LIFX Discovery', namespace: 'robheyes', author: 'Robert Alan Heyes', importUrl: 'https://raw.githubusercontent.com/robheyes/lifxcode/master/LIFXDiscovery.groovy') {
//        capability "Refresh"
//
//        attribute 'lifxdiscovery', 'string'
//        attribute 'progress', 'number'
//    }
//
//    preferences {
//        input "logEnable", "bool", title: "Enable debug logging", required: false
//    }
//}
//
//@SuppressWarnings("unused")
//def updated() {
//    log.info "LIFX updating"
//}
//
//@SuppressWarnings("unused")
//def installed() {
//    log.info "LIFX Discovery installed"
//    initialize()
//}
//
//def initialize() {
//    def discoveryType = parent.discoveryType()
//    logDebug "Discovery type $discoveryType"
//    if (discoveryType == 'discovery') {
//        refresh()
//    } else if (discoveryType == 'refresh') {
//        rescanNetwork()
//    } else {
//        logDebug "Unexpected discovery type $discoveryType"
//    }
//}
//
//def refresh() {
//    String[] subnets = parent.getSubnets()
//    if (0 == subnets.size()) {
//        log.warn "Can't discover the hub's subnet!"
//        return
//    }
//
//    parent.clearCachedDescriptors()
//    int scanPasses = parent.maxScanPasses()
//    String packet = makePacketString parent.typeOfMessage('DEVICE.GET_VERSION'), true, 1 as byte
//    Map queue = prepareQueue(packet)
//    subnets.each {
//        String subnet = it
//        1.upto(scanPasses) {
//            parent.setScanPass(it)
//            scanNetwork queue, subnet, it
//        }
//    }
//    sendEvent name: 'progress', value: 0
//    queue.size = queue.ipAddresses.size()
//    runInMillis(50, 'processQueue', [data: queue])
//}
//
//private void scanNetwork(Map queue, String subnet, Number pass) {
//    1.upto(pass + extraProbesPerPass) {
//        1.upto(254) {
//            def ipAddress = subnet + it
//            queue.ipAddresses << ipAddress
//        }
//    }
//}
//
//private static Map prepareQueue(String packet, int delay = 20) {
//    [packet: packet, ipAddresses: [], delay: delay]
//}
//
//@SuppressWarnings("unused")
//private processQueue(Map queue) {
//    def oldPercent = calculateQueuePercentage(queue)
//    if (isQueueEmpty(queue)) {
//        def outstandingDevices = parent.getDeviceDefinitions()
//        sendEvent name: 'lifxdiscovery', value: outstandingDevices.size() == 0 ? 'complete': 'outstanding'
//        return
//    }
//    def data = getNext(queue)
//    sendPacket data.ipAddress, data.packet
//    def newPercent = calculateQueuePercentage(queue)
//    if (oldPercent != newPercent) {
//        sendEvent name: 'progress', value: newPercent
//    }
//    runInMillis(queue.delay, 'processQueue', [data: queue])
//}
//
//private static Map<String, String> getNext(Map queue) {
//    String first = queue.ipAddresses.first()
//    queue.ipAddresses = queue.ipAddresses.tail()
//    [ipAddress: first, packet: queue.packet]
//}
//
//private static isQueueEmpty(Map queue) {
//    queue.ipAddresses.isEmpty()
//}
//
//private static int calculateQueuePercentage(Map queue) {
//    100 - (int) ((queue.ipAddresses.size() * 100) / queue.size as Long)
//}
//
//def rescanNetwork() {
//    logDebug "Rescan network"
//    String subnet = parent.getSubnet()
//    if (!subnet) {
//        return
//    }
//    logDebug "Rescanning..."
//    1.upto(254) {
//        def ipAddress = subnet + it
//        if (parent.isKnownIp(ipAddress)) {
//            sendCommand ipAddress, parent.typeOfMessage('DEVICE.GET_GROUP'), true, 1, it % 128 as Byte
//        }
//    }
//    parent.setScanPass 'DONE'
//    sendEvent name: 'lifxdiscovery', value: 'complete'
//}
//
//private void sendCommand(String ipAddress, int messageType, boolean responseRequired = true, int pass = 1, Byte sequence = 0) {
//    String stringBytes = makePacketString(messageType, responseRequired, sequence)
//    sendPacket ipAddress, stringBytes
//    pauseExecution(parent.interCommandPauseMilliseconds(pass))
//}
//
//private String makePacketString(int messageType, boolean responseRequired, byte sequence) {
//    def buffer = []
//    parent.makePacket buffer, [0, 0, 0, 0, 0, 0] as byte[], messageType, false, responseRequired, [], sequence
//    def rawBytes = parent.asByteArray(buffer)
//    String stringBytes = hubitat.helper.HexUtils.byteArrayToHexString rawBytes
//    stringBytes
//}
//
//def parse(String description) {
//    Map<String, List> command = parent.discoveryParse(description)
//    sendActions command
//}
//
//private void sendActions(Map<String, List> actions) {
//    actions.commands?.eachWithIndex { item, index -> sendCommand item.ip as String, item.type as int, true, 1, index as Byte }
//    actions.events?.each { sendEvent it }
//}
//
//private void sendPacket(String ipAddress, String bytes) {
//    broadcast bytes, ipAddress
//}
//
//private void broadcast(String stringBytes, String ipAddress) {
//    sendHubCommand(
//            new hubitat.device.HubAction(
//                    stringBytes,
//                    hubitat.device.Protocol.LAN,
//                    [
//                            type              : hubitat.device.HubAction.Type.LAN_TYPE_UDPCLIENT,
//                            destinationAddress: ipAddress + ":56700",
//                            encoding          : hubitat.device.HubAction.Encoding.HEX_STRING,
//                            ignoreWarning    : true
//                    ]
//            )
//    )
//}
//
//static byte[] asByteArray(List buffer) {
//    (buffer.each { it as byte }) as byte[]
//}
//
//private void logDebug(msg) {
//    log.debug "DISCOVERY: $msg"
//}
//
//private void logInfo(msg) {
//    log.info msg
//}
//
//private void logWarn(String msg) {
//    log.warn msg
//}
//
//
///** ========= BELOW THIS LINE IS EXPERIMENTAL CODE THAT MAY PROVE USEFUL EVENTUALLY */
////List<Map> discoverMacs(String subnet) {
////    def result = []
////    1.upto(254) {
////        def ipAddress = subnet + it
////        def mac = getMACFromIP(ipAddress)
////        if (mac) {
////            result << [mac: mac, ip: ipAddress]
////        }
////    }
////    result
////}
////
////private scanNetwork(List<Map> macs, int pass) {
////
////    macs.each {
////        String ipAddress = it.ip
////        if (!parent.isKnownIp(ipAddress)) {
////            logDebug "Scanning $ipAddress"
////            1.upto(pass + extraProbesPerPass) {
////                sendCommand ipAddress, messageTypes().DEVICE.GET_VERSION.type as int, true, 1, it % 128 as Byte
////            }
////        }
////    }
////}
