# LIFX Lan Protocol app and drivers for Hubitat Elevation
## About
LIFX is quite a complex system to integrate into Hubitat so it's not as simple as just installing a single driver

There are several components
* The LIFX Master App - this is where you discover devices
* The LIFX Discovery device - this does the grunt work of scanning your network for LIFX devices
* Various LIFX device handlers
  * LIFX Color - currently the only driver supplied, others will be added soon
  * LIFXPlus Color (includes support for Night Vision) 
  * LIFX White
  * LIFX Day and Dusk
  * LIFX Tile
  * LIFX Beam
  * LIFX Strip
  
## Installation
### LIFX Master App
On the Apps Code page, press the **New App** button

Paste the code for LIFXMasterApp.groovy and press the **Save** button (or type **Ctrl-S**)
### LIFX Discovery and individual device handlers
On the Devices Code page, press the **New Driver** button 

Paste the code for LIFXDiscovery.groovy and save

Repeat this process for each of the device handlers you want to install

### Create the app
On the Apps page press the **Add User App** button then click on **LIFX Master** in the list of available apps.

### Device discovery
First of all, make sure that all your LIFX devices are powered on, obviously discovery won't find any device that doesn't 
have power.

Open the **LIFX Master** app and press the **Discover devices** button.  Check the logs to see discovery in progress.
You may notice the LIFX Discovery device in your list of devices, but this should disappear after about 
5 minutes from starting the scan.

The LIFX Lan Protocol uses UDP messages, it seems that sometimes these can be missed or lost, so the discovery process 
makes 5 passes over your local network to try to mitigate this.  So far this seems good enough on my network.

## Troubleshooting
### Undiscovered devices
If you find that some devices aren't discovered then you could try altering some of the values in the following functions 
in LIFXMasterApp.groovy
```

Integer interCommandPauseMilliseconds() {
    50
}

Integer maxScanTimeSeconds() {
    300
}

Integer maxScanPasses() {
    5
}
```

My first recommendation would be increasing the value of interCommandPauseMilliseconds.  Try increasing it by 10 at a time.

You can also increase maxScanPasses to give discovery a better chance of reaching all the devices in your network.

If you have a lot of devices, or change either of the other two parameters then you may need to increase maxScanTimeSeconds from
the default of 300 seconds.

### Limitations
I have assumed that you will be running a Class C network (also known as /24), ie that your subnet mask is 255.255.255.0, 
which limits you to a total of 254 IP addresses.
