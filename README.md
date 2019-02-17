# LIFX Lan Protocol app and drivers for Hubitat Elevation
## About
LIFX is quite a complex system to integrate into Hubitat so it's not as simple as just installing a single driver

There are several components
* The LIFX Master App - this is where you discover devices
* The LIFX Discovery device - this does the grunt work of scanning your network for LIFX devices
* Various LIFX device handlers
  * LIFX Color
  * LIFXPlus Color (does not currently include support for Infrared/Night Vision) 
  * LIFX White
  * LIFX Day and Dusk
  * LIFX Tile - dummy driver, only supports on/off
  * LIFX Multizone - for Beam and Strip, currently only supports on/off
  
## Installation
### LIFX Master App
On the Apps Code page, press the **New App** button

Paste the code for LIFXMasterApp.groovy and press the **Save** button (or type **Ctrl-S**)
### LIFX Discovery and individual device handlers
On the Devices Code page, press the **New Driver** button 

Paste the code for LIFXDiscovery.groovy and save

Repeat this process for each of the device handlers you want to install. It's probably best
to add all of them

### Create the app
On the Apps page press the **Add User App** button then click on **LIFX Master** in the list of available apps.

####IMPORTANT: don't forget to press the __Done__ button to make sure the the **LIFX Master** app sticks around.

### Device discovery
First of all, make sure that all your LIFX devices are powered on, obviously discovery won't find any device that doesn't 
have power.

Again __IMPORTANT__ if you didn't press the __Done__ button when you created the **LIFX Master** app then do it now.

Open the **LIFX Master** app and press the **Discover devices** button. 
You may notice the LIFX Discovery device in your list of devices, but this should disappear at the end of the scan. 

#### Implementation information
The LIFX Lan Protocol uses UDP messages, it seems that sometimes these can be missed or lost, so the discovery process 
makes 5 passes over your local network to try to mitigate this.  So far this seems good enough on my network.

Further to this, when a command is sent to change a LIFX setting it requests an acknowledgement and 
the sequence number is tracked with the LAN Protocol packet. When a subsequent command or query is sent
it checks to see whether the acknowledgement was received. If it wasn't received the packet is sent 
one more time.  The reason for this is that UDP messages are not guaranteed to arrive, so it's pretty much
a small safety net.  You can expect that this will happen at least when the device is next polled (under a minute).
A warning should show up in the log if this happens.
  
## Troubleshooting
### Undiscovered devices
If you find that some devices aren't discovered then you could try altering some of the options

My first recommendation would be increasing the value of __`Time between commands for first pass (milliseconds)`__.  Try increasing it by 10 at a time.

You can also increase `Maximum number of passes` to give discovery a better chance of reaching all the devices in your network.

### Errors in the log
Let me know if you see any errors in the log, and I'll do my best to fix the issue as soon as possible

### Limitations
I have assumed that you will be running a Class C network (also known as /24), ie that your subnet mask is 255.255.255.0, 
which limits you to a total of 254 IP addresses.

Note that this uses the IP address as the device network id - if you change the id you may break things. 
If you don't assign fixed IP addresses to your LIFX devices then you may be able to use this to avoid having 
to scan for new devices.
