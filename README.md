# LIFX Lan Protocol app and drivers for Hubitat Elevation
## About
LIFX is quite a complex system to integrate into Hubitat so it's not as simple as just installing a single driver

There are several components
* The LIFX Master App - this is where you discover devices
* Various LIFX device handlers
  * LIFX Color
  * LIFXPlus Color 
  * LIFX White
  * LIFX Day and Dusk
  * LIFX Tile - dummy driver, only supports on/off
  * LIFX Multizone - for Beam and Strip
  
# Installation
## Using Hubitat Package Manager
This is now the preferred installation method since it provides an easy path to install updates and beta releases.

See https://github.com/dcmeglio/hubitat-packagemanager if you don't already have Hubitat Package Manager installed.

### New installation using HPM
* Open the HPM app and choose __Install__
* Pick `Browse by Tags`
* Select `Lights & Switches`
* Scroll down to `LIFX Master app and drivers` and select it
* Press the __Next__ button

HPM should then do some magic and install everything for you.
You'll probably still need to follow the `Create the app` step below.
### HPM installation when you have previously manually installed
Simply use the __Match Up__ option in HPM. I'd recommend that you leave the `Assume that packages are up-to-date` switch 
turned off. 

Then do an __Update__ to ensure that you have the latest version.

## Manual installation 
### LIFX Master App
On the Apps Code page, press the **New App** button

Paste the code for `LIFXMasterApp.groovy` and press the **Save** button (or type **Ctrl-S**)
### Individual device handlers
On the Devices Code page, press the **New Driver** button 

Paste the code for `LIFXColor.groovy` and save

Repeat this process for each of the device handlers you want to install. It's probably best
to add all of them even if you don't have a corresponding device at the moment, I've found that buying
LIFX devices is a bit addictive.

## Create the app
On the Apps page press the **Add User App** button then click on **LIFX Master** in the list of available apps.

####IMPORTANT: don't forget to press the __Done__ button to make sure the the **LIFX Master** app sticks around.

## Device discovery
First of all, make sure that all your LIFX devices are powered on, obviously discovery won't find any device that doesn't 
have power.

Again __IMPORTANT__ if you didn't press the __Done__ button when you created the **LIFX Master** app then do it now.

Open the **LIFX Master** app and press the **Discover devices** button. 
You may notice the **LIFX Discovery** device in your list of devices, but this should disappear at the end of the scan. 

### Updating
#### Always make a backup.

#### Hubitat Package Manager update
* Open the HPM app
* Select the `Update` option
* If there's an available update then you should see the new version in the list of updated packages.

#### Manual update
When the drivers/app have been updated I'll post a message on the Hubitat forum.  

To update a device or the app just open the corresponding code, 
1. click the `Import` button at the top of the window
2. a dialog should open with the URL prefilled - click its `Import` button
3. click `OK` on the override dialog that should have appeared
4. click the `Save` button

I normally open the code for each app and device driver in a new tab, then for each tab I'll just do the import (steps 1-3)
then once the code has been updated I'll go through the tabs one at a time clicking save and then closing that tab.  

While this is happening, you may find some errors appearing in the log if I've made significant changes.  Only be
concerned if you see errors after all the drivers and the app have been updated.

### Implementation information
The LIFX Lan Protocol uses UDP messages, it seems that sometimes these can be missed or lost, so the discovery process 
makes 5 passes over your local network to try to mitigate this.  So far this seems good enough on my network.

Further to this, when a command is sent to change a LIFX setting it requests an acknowledgement and 
the sequence number is tracked with the LAN Protocol packet. When a subsequent command or query is sent
it checks to see whether the acknowledgement was received. If it wasn't received the packet is sent 
one more time.  The reason for this is that UDP messages are not guaranteed to arrive, so it's pretty much
a small safety net.  You can expect that this will happen at least when the device is next polled (under a minute).

### Using the Multizone capabilities
The multizone driver provides a generic `setZones` command.  This accepts an input in the following format:
```0:"[hue: 30, brightness: 100, saturation: 100, kelvin: 3500]", 1:"[hue: 0, brightness: 100, saturation: 0, kelvin:3500"]```
an entry can be submitted for each zone you want to update, and can contain any combination of parameters - 
e.g. if you only want to update hue and saturation, you can specify only those values, and the brighness and kelvin
values for that zone will remain the same.  Likewise, you can omit any zones that you do not want to update.
As an alternate to specific HBSK values, you can specify any of the defined `namedColors` or an RGB hex value using
```0:"[color: 'Red']", 1:"[color: '#0000FF']"```

An additional capability is the creation of child devices for each zone.  The corresponding child device can be
updated/set like a typical RGBW bulb, and it will update/set the zone in the parent multizone device.  If the parent
device is updated directly, it will update its children on the next polling interval (1 min).

NOTES: 
* After creating the child devices, I've found you have to toggle a few things before they start updating
the parent properly - e.g. flip the "switch" on, off, and on again.  Will see if this can be addressed in a
future update.

## Troubleshooting
### Undiscovered devices
If you find that some devices aren't discovered
* try the **Discover new only new devices** button
* make sure that the missing devices still have power - someone may have turned off a switch :) 
* try altering some of the options as described below. 

With recent improvements I've found that the default settings should be more than enough to discover all your devices in two 
passes, but your mileage may vary.

My first recommendation would be increasing the value of __`Time between commands (milliseconds)`__.  
Try increasing it by 10 at a time.

You can also increase `Maximum number of passes` to give discovery a better chance of reaching all the devices in your network. 
I've found that all my devices are found in the first pass at least 95% of the time.

### Errors in the log
Let me know if you see any errors in the log, and I'll do my best to fix the issue as soon as possible

### Limitations
I have assumed that you will be running a Class C network (also known as /24), ie that your subnet mask is 255.255.255.0, 
which limits you to a total of 254 IP addresses.

Note that this uses the IP address as the device network id - if you change the id you may break things. 
If you don't assign fixed IP addresses to your LIFX devices then you may be able to use this to avoid having 
to scan for new devices.
