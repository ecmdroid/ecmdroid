EcmDroid
--------

EcmDroid is an Android application to diagnose and configure Buell
Motorcycles with a DDFI(-2, -3) ECM.

Communication with the ECM is achieved through a Bluetooth-, BLE- or
USB-to-serial adapter connected to the motorcycle's diagnostic plug. The
plug is located underneath the seat (on XB-9/12 "S" models) or behind
the front mask (on XB-9/12 "R" models).

This [Article](https://ecmspy.com/btwireless2.shtml)
explains how to build a Bluetooth Serial Adapter. Pre-built
adapters are also offered by various vendors, e.g.
[buell-parts.com](https://buell-parts.com/Bluetooth-Adapter-Version-2).

Initial pairing of your Android device and the Bluetooth serial adapter
must be done using the Android Settings application (Wireless & Network).
Also, make sure that the Bluetooth serial adapter is set to 9600, 8N1,
No handshake.

Pairing is not required for BLE serial adapters.

Also checkout [ecmsim](https://github.com/ecmdroid/ecmsim) which can
be used for testing/debugging ecmdroid without a real ECM.
