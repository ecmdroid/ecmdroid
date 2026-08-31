# EcmDroid User Guide

EcmDroid is an Android app for diagnosing and tuning Buell motorcycles that run a
Zeeltronic/ecmspy-derived DDFI(-1, -2, -3) Engine Control Module (ECM). It talks
directly to the ECM over the motorcycle's factory diagnostic connector, using a
Bluetooth, Bluetooth LE, or USB-to-serial adapter.

This guide covers everything an owner/tuner needs: what hardware to buy, how to
pair it, and how to use every screen in the app. For source-level and build
documentation, see [`DEVELOPER_GUIDE.md`](DEVELOPER_GUIDE.md).

## Table of Contents

1. [What EcmDroid Can Do](#1-what-ecmdroid-can-do)
2. [Supported Motorcycles / ECMs](#2-supported-motorcycles--ecms)
3. [Hardware You Need](#3-hardware-you-need)
4. [Installing EcmDroid](#4-installing-ecmdroid)
5. [Connecting to the ECM](#5-connecting-to-the-ecm)
6. [App Settings](#6-app-settings)
7. [Screen-by-Screen Guide](#7-screen-by-screen-guide)
8. [Working with EEPROM Files](#8-working-with-eeprom-files)
9. [Recording and Converting Logs](#9-recording-and-converting-logs)
10. [Safety: Reading vs. Writing (Burning)](#10-safety-reading-vs-writing-burning)
11. [Troubleshooting](#11-troubleshooting)
12. [Privacy](#12-privacy)

---

## 1. What EcmDroid Can Do

- Identify the connected ECM (type, protocol, serial number, manufacturing date,
  calibration ID).
- Read live ("runtime") sensor data: RPM, throttle position, coolant
  temperature, air/fuel value, battery voltage, etc.
- Read current, recent, and stored diagnostic trouble codes, and clear them.
- Trigger "active tests" — manually fire the fuel pump, ignition coils,
  injectors, cooling fan, exhaust valve, shift light, or reset the TPS
  (throttle position sensor) baseline.
- Read and edit the ECM's EEPROM: named configuration parameters (via the
  Setup screen) or raw hex bytes (via the EEPROM screen).
- Save/load EEPROM dumps to/from a file (`.xpr`/legacy `.epr` format) so you
  can back up a tune or share it.
- Record live runtime data to a binary log file and convert it to
  MegaLogViewer (`.msl`) format for graphing/analysis.
- Look up factory torque specifications for Buell XB fasteners.

EcmDroid does **not** contain any tuning tables/maps editor beyond what the ECM
exposes as named EEPROM variables — it is a diagnostic and calibration tool,
not a stand-alone fuel/ignition map editor.

## 2. Supported Motorcycles / ECMs

The app recognizes three ECM generations, auto-detected from the ECM's version
string once connected:

| App label | Motorcycles                              |
|-----------|-------------------------------------------|
| DDFI      | Buell "Tuber" models (early DDFI-1)        |
| DDFI-2    | Buell XB models through 2007               |
| DDFI-3    | Buell XB 2008+, 1125R, 1125CR               |

Each ECM also runs one of two **protocols**, selectable in the app (Main
screen) and identifiable by the marking on the ECM casing:

- **Stock / P&A** — the vast majority of ECMs. Serial adapter must be set to
  **9600 baud, 8N1, no handshake**.
- **Factory Race** — identified by an ***engraved*** "RACE USE ONLY" marking.
  Serial adapter must be set to **19200 baud, 8N1, no handshake**.

(A ***printed*** "RACE USE ONLY" marking indicates a P&A Race ECM, which still
uses the Stock protocol/baud rate.)

If you pick the wrong protocol, the app will fail to identify the ECM (version
string won't parse) — switch the protocol and reconnect.

## 3. Hardware You Need

You need an adapter that plugs into the motorcycle's diagnostic connector
(under the seat on "S" models, behind the front mask on "R" models) and
exposes a serial link to your phone/tablet. EcmDroid supports four transport
types:

| Type | Notes |
|---|---|
| **Classic Bluetooth (SPP)** | Most common. Any Bluetooth-serial adapter (e.g. based on HC-05/06) that presents a Serial Port Profile. [Build guide](https://ecmspy.com/btwireless2.shtml) or buy pre-built from vendors such as [buell-parts.com](https://buell-parts.com/Bluetooth-Adapter-Version-2). |
| **Bluetooth LE** | Adapters built around Nordic nRF51822, Texas Instruments CC254x, Microchip RN4870/RN4871, or Telit Bluemod TIO 2.0 chipsets are auto-detected. No OS-level pairing required. |
| **USB-to-serial adapter** | Plug an FTDI, Silicon Labs CP210x, Prolific PL2303, or QinHeng CH340/CH341/CH9102 USB-serial cable into your Android device (via a USB-OTG cable) wired to the diagnostic connector. Requires Android USB host support. |
| **TCP/IP** | Connect to a Bluetooth-to-WiFi or serial-to-network bridge by host/port (advanced/legacy option). |

For **Classic Bluetooth**, pair the adapter first using Android's own
**Settings → Bluetooth** app — EcmDroid does not do OS-level pairing itself.
For **BLE**, no separate pairing step is needed; the app scans for and
connects to the adapter directly.

Also check out [ecmsim](https://github.com/ecmdroid/ecmsim), a standalone ECM
simulator you can use to try EcmDroid without a real motorcycle.

## 4. Installing EcmDroid

- Requires **Android 8.0 (API 26) or later**.
- Install the APK from your preferred source (F-Droid/GitHub releases/Play
  Store, depending on distribution) or build it yourself — see
  [`DEVELOPER_GUIDE.md`](DEVELOPER_GUIDE.md#building).
- On first launch the app extracts its bundled ECM-definitions database; you
  may briefly see an "Updating database…" toast.

### Permissions

EcmDroid will ask for, or use, the following at runtime:

- **Nearby devices (Bluetooth Scan/Connect)** — required to list and connect
  to Bluetooth/BLE adapters (Android 12+).
- **Location** — required by Android itself for classic BLE scanning on
  Android 6–11 (`ACCESS_FINE_LOCATION`/`ACCESS_COARSE_LOCATION`). EcmDroid
  does not use your location for anything else; if location services are
  turned off it automatically falls back to classic Bluetooth discovery for
  BLE devices instead.
- **Notifications** — shown while a log recording is in progress, so you can
  see recording status from the notification shade (Android 13+ requires you
  to grant this explicitly).
- **Storage folder access** — you choose a folder (via the system file
  picker) where EEPROM dumps and log files are saved; EcmDroid never gets
  broad storage access.

## 5. Connecting to the ECM

1. Tap the round connect button (bottom right, colored **red** when
   disconnected).
2. Depending on the connection type configured in **Settings**:
   - **Classic Bluetooth**: pick your already-paired adapter from the list.
   - **Bluetooth LE**: the app scans for nearby BLE devices; tap yours in the
     list. Use the menu's "Scan"/"Stop scan" and "Bluetooth settings"
     shortcuts if needed.
   - **USB-to-serial**: plug in the adapter (or it may already be attached);
     grant the USB permission prompt when Android shows it.
   - **TCP/IP**: connects automatically to the host/port set in Settings.
3. The button turns **gray** while connecting, then **green** once EcmDroid
   has successfully identified the ECM. Tap it again anytime to disconnect.

If the connect button stays red or you get a "Could not open COM port" /
"connection failed" message, see [Troubleshooting](#11-troubleshooting).

Once connected, the **ECM Information** screen (the app's home screen) shows
the ECM ID, type, protocol, serial number, manufacturing date, layout
revision, country ID and calibration ID (the latter group only appears after
an EEPROM read).

## 6. App Settings

Open the navigation drawer (swipe from the left edge, or tap the hamburger
icon) → **Settings**:

| Setting | Purpose |
|---|---|
| Storage location | Folder (chosen via system file picker) where EEPROM dumps and log files are written/read. Set this before saving/loading/logging. |
| ECM connection type | Classic Bluetooth / Bluetooth LE / TCP/IP / USB-to-serial adapter. |
| TCP host / TCP port | Only used when connection type is TCP/IP. |
| Hide non-existent EEPROM variables | On the Setup screen, hides parameters that don't apply to your detected ECM model. |
| Enable EEPROM burning | **Off by default.** Must be turned on before you can write (burn) any changes back to the ECM. Read this twice before enabling — see [§10](#10-safety-reading-vs-writing-burning). |
| Optimized burning | Only available once burning is enabled. When on, only EEPROM pages you actually changed are re-written, which is faster and reduces write-cycle wear. |
| Keep screen on | Prevents the screen from locking while EcmDroid is in the foreground (handy for long logging sessions or a phone mounted on the bike). |

The **ECM Protocol** selector (Stock/P&A vs. Factory Race) lives on the main
ECM Information screen, not in Settings — see [§2](#2-supported-motorcycles--ecms).

## 7. Screen-by-Screen Guide

Navigate between screens using the left-hand drawer.

### ECM Information (home screen)

Shows ECM identity fields described in [§5](#5-connecting-to-the-ecm) and lets
you pick the protocol (Stock/P&A vs. Factory Race) before connecting.

### Trouble Codes

- **Read Errors** fetches and displays two lists: *Current* errors (active
  right now) and *Stored* errors (persisted in the ECM even after they clear).
  Each line shows the code and its description.
- **Clear Errors** asks for confirmation, then tells the ECM to clear its
  stored codes and re-reads the error lists.

### Active Tests

- Pick a test from the list (front/rear ignition coil, tachometer, fuel pump,
  front/rear injector, cooling fan, exhaust valve, active intake, shift
  light) and tap **Start Test** to fire it once. Useful for confirming a
  component (e.g. the fuel pump relay or an injector) is wired and
  responding, without starting the engine.
- **TPS Reset** re-learns the throttle position sensor's closed-throttle
  baseline. Follow the on-screen prompt (procedure differs slightly for
  DDFI-3 ECMs) — typically: key on, don't touch the throttle, confirm.
- Tests require an active connection; buttons are disabled until you connect.

### Data Channels

- A live gauge list showing up to 5 configurable channels at once (defaults:
  battery voltage, RPM, throttle position, air/fuel value, coolant
  temperature).
- Tap a channel row's dropdown to swap in a different runtime variable.
- Toggle the switch to start/stop live streaming; values refresh continuously
  while it's on (values come from the ECM roughly 4x/second by default).
- Your channel selections are remembered per-ECM.

### Setup (named parameters)

- A categorized settings-style list of ECM configuration parameters — system
  options, noise-reduction/retard settings, airbox/baro-pressure sensor
  configuration, shifter/shift-light configuration, error-reporting masks,
  RPM/temperature limits, fan on/off setpoints, AFV calibration, O2 sensor
  setup, etc.
- Checkboxes toggle individual configuration bits; text fields hold scaled
  numeric values (e.g. RPM limits, temperatures, voltages) with their real
  units — you type the human value, not a raw byte.
- Edits are staged locally. An **Apply Changes / Save** button appears once
  you have unsaved edits and are connected; use it (or the EEPROM screen's
  **Burn** action) to actually write changes to the ECM — see
  [§10](#10-safety-reading-vs-writing-burning).

### EEPROM (raw byte editor)

- A hex grid of every byte in the ECM's EEPROM, addressed by offset.
- Tap a cell to see details in the info panel: offset (hex/decimal), byte
  value (hex/decimal), the 16-bit word formed with the adjacent byte
  (hi/lo, hex/decimal), and the nearest known variable name, if any.
- **Long-press** a cell to open the byte editor dialog: type a value 0–255,
  or use the quick buttons (`0`, `255`, `-16`, `+16`, `÷2`, `×2`) then **Set**.
- Menu actions: **Fetch** (read all EEPROM pages from the ECM), **Burn**
  (write changed pages back), **Save** (export the current EEPROM to a file),
  **Load** (import an EEPROM file). See [§8](#8-working-with-eeprom-files).

### Log Recorder

- Choose a sample interval (No Delay up through 5 seconds) from the dropdown.
- Tap **Start** to begin recording; live TPS/RPM/coolant-temp readouts and a
  running byte/record counter are shown while recording.
- Tap **Stop** to end the session. The binary log is written into your
  configured storage folder as `yyyyMMdd_hhmmss.bin`.
- Enable the **Convert to MSL** switch before stopping to automatically
  produce a companion MegaLogViewer-compatible `.msl` text file you can graph
  in MegaLogViewer or open in a spreadsheet. See [§9](#9-recording-and-converting-logs).

### Torque Values

A static reference of factory torque specifications (Nm) for Buell XB
chassis/engine fasteners, sourced from the factory service manual and the
xborgforum community wiki. No connection required; purely informational.

## 8. Working with EEPROM Files

- **Save**: EEPROM screen menu → **Save**. Writes the full current EEPROM
  image (whatever is currently loaded/fetched) to a file named
  `<ECM-ID>_yyyyMMdd-hhmmss.xpr` in your chosen storage folder. This is the
  same `.xpr` format used by ECMSpy, so files are interchangeable with that
  tool. Legacy `.epr` dumps can also be loaded (not saved).
- **Load**: EEPROM screen menu → **Load**, pick a file. EcmDroid detects the
  ECM type from the file size; if more than one ECM type shares that size,
  you'll be prompted to pick the correct one. If you're connected to a real
  ECM, the loaded file's ID must match the connected ECM's ID.
- **Fetch**: reads all EEPROM pages fresh from the connected ECM, overwriting
  any unsaved local edits (you'll be asked to confirm if you have unsaved
  changes).
- **Burn**: writes local edits back to the connected ECM — see next section.

## 9. Recording and Converting Logs

- Logs are recorded in a compact binary format: a small ECM-type header
  followed by timestamped runtime-data snapshots, each with a checksum.
- Recording interval options range from "No Delay" (as fast as the ECM will
  respond) to a fixed 5-second period — shorter intervals produce denser data
  but larger files and use more phone/bike-adapter bandwidth.
- Converting to `.msl` (MegaLogViewer format) turns the binary log into a
  tab-separated text file with one row per sample and one column per
  parameter (RPM, TPS, AFV, coolant temp, EGO correction, etc.), plus derived
  columns like closed/open-loop EGO correction ("Gego") and an engine-state
  byte. This is done automatically at Stop time if you enabled the convert
  switch, or can be triggered from a saved `.bin` file.
- Corrupted samples (failed checksum) are silently dropped during conversion
  rather than aborting the whole log.

## 10. Safety: Reading vs. Writing (Burning)

Reading (Fetch) never modifies your ECM and is always safe. **Writing
("burning") changes to the EEPROM can leave your ECM in a bad state if
interrupted or done with mismatched data** — treat it with respect:

- Burning is **disabled by default**. You must explicitly enable it under
  **Settings → Enable EEPROM burning**, and the app shows an extra warning
  dialog every time you burn.
- Before writing, EcmDroid re-reads the ECM's ID/version and refuses to burn
  if it doesn't match the EEPROM data you're about to write (protects against
  writing one motorcycle's tune to a different ECM).
- Don't disconnect, lose Bluetooth/BLE range, or let your phone sleep during
  a burn. Enable **Keep screen on** in Settings for long sessions.
- Keep a saved (`.xpr`) backup of the EEPROM *before* you start editing, so
  you can always restore a known-good state.
- **Optimized burning** (only write touched pages) is faster but only writes
  pages you've actually changed; if you're unsure exactly what changed,
  leaving it off (full-page burn) is the more conservative choice.

## 11. Troubleshooting

| Symptom | Likely cause / fix |
|---|---|
| Connect button never turns green | Wrong protocol selected (Stock vs. Factory Race) — try the other one. Wrong baud rate on a DIY adapter (9600 for Stock, 19200 for Factory Race). |
| "No USB COM Devices available" | USB-serial adapter not recognized/plugged in, or your phone/OS doesn't support USB host mode. Check the adapter uses one of the supported chipsets (FTDI, CP210x, PL2303, CH340/CH341/CH9102, or a CDC-ACM device such as Arduino). |
| "Give USB Permission" / connection fails after that prompt | Re-plug the adapter and accept the Android USB permission dialog when it appears. |
| BLE device doesn't show up while scanning | On Android 6–11, BLE scanning requires system Location Services to be turned on. Turn on Location, or let the app fall back to classic discovery. Also confirm Bluetooth is enabled. |
| Classic Bluetooth device not in the list | Pair it first in Android's own Settings → Bluetooth app, then reopen EcmDroid. |
| Connected, but ECM Information fields stay blank/N/A | ECM took the version request but didn't answer as expected — usually a protocol/baud mismatch, or a loose diagnostic-connector cable. Reseat the connector and retry. |
| Trouble reading/writing EEPROM mid-operation | Stay in range and keep the screen awake; retry the Fetch/Burn. If a Burn is refused with an ID-mismatch error, you loaded/prepared data for a different ECM than the one currently connected. |
| Log file location not writable | Set/repick the storage folder in Settings before recording — the picker grants EcmDroid persistent access to that folder only. |

## 12. Privacy

See [`privacy-policy.md`](../privacy-policy.md) for the full policy. In short:
EcmDroid does not collect or transmit any personal data; all ECM data,
EEPROM dumps, and logs stay on your device and in the storage folder you
choose.
