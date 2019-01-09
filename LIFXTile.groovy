/**
 *
 * Copyright 2018, 2019 Robert Heyes. All Rights Reserved
 *
 *  This software if free for Private Use. You may use and modify the software without distributing it.
 *  You may not grant a sublicense to modify and distribute this software to third parties.
 *  Software is provided without warranty and your use of it is at your own risk.
 *
 */
definition(name: "LIFX Tile", namespace: "robheyes", author: "Robert Alan Heyes") {
	capability "Light"
//	capability "LightEffect"
    capability "ColorControl"
    capability "ColorTemperature"
    capability "HealthCheck"
    capability "Polling"
    capability "Initialize"
    attribute "Label", "string"
    attribute "Group", "string"
    attribute "Location", "string"
}


preferences {
    input "logEnable", "bool", title: "Enable debug logging", required: false
}

def initialize() {

}

def refresh() {

}

def poll() {

}

def on() {

}

def off() {

}

def setColor(Map colorMap) {

}

def setHue(number) {

}

def setSaturation(number) {

}

def setColorTemperature(temperature) {

}

