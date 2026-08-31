# EcmDroid Developer Guide

Technical/architecture reference for working on the EcmDroid codebase: a
native Android (plain Java, no DI/Compose/coroutines) diagnostic and tuning
app for Buell DDFI(-1,-2,-3) motorcycle ECMs.

For coding-agent-oriented quick reference (conventions, command cheat sheet),
see [`AGENTS.md`](../AGENTS.md) at the repo root — this document goes deeper
into *why* things are built this way, the wire protocol, and the data model,
with file/line references for the parts most people need when making changes.
For end-user documentation, see [`USER_GUIDE.md`](USER_GUIDE.md).

## Table of Contents

1. [Project Layout](#1-project-layout)
2. [Building and Running](#2-building-and-running)
3. [High-Level Architecture](#3-high-level-architecture)
4. [The Wire Protocol (PDU)](#4-the-wire-protocol-pdu)
5. [Transport Layer](#5-transport-layer)
6. [ECM: the Central Facade](#6-ecm-the-central-facade)
7. [EEPROM / Variable / BitSet Data Model](#7-eeprom--variable--bitset-data-model)
8. [Bundled ECM-Definitions Database](#8-bundled-ecm-definitions-database)
9. [Background Service and Threading](#9-background-service-and-threading)
10. [UI Layer](#10-ui-layer)
11. [Async Task Pattern](#11-async-task-pattern)
12. [Binary Log Format and MSL Conversion](#12-binary-log-format-and-msl-conversion)
13. [Testing](#13-testing)
14. [Code Conventions](#14-code-conventions)
15. [Common Pitfalls](#15-common-pitfalls)

---

## 1. Project Layout

```
ecmdroid/
├── app/
│   ├── build.gradle.kts              # module config, versionCode/versionName, signing
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── assets/ecmdroid.db.gz # bundled gzip'd SQLite ECM-definitions DB
│       │   ├── assets/about.html     # About screen content
│       │   ├── resources/            # runtime1.tab/runtime2.tab/runtime3.tab (Bin2Msl offset tables)
│       │   ├── res/                  # layouts, menus, xml preference trees, strings
│       │   └── java/
│       │       ├── org/ecmdroid/                              # core domain
│       │       ├── org/ecmdroid/activities/                   # MainActivity, PrefsActivity, AboutActivity
│       │       ├── org/ecmdroid/fragments/                    # one fragment per drawer screen
│       │       ├── org/ecmdroid/task/                         # AsyncTask I/O helpers
│       │       ├── org/ecmdroid/util/                         # Bin2MslConverter
│       │       └── de/kai_morich/simple_bluetooth_le_terminal/ # vendored BLE UART transport
│       └── androidTest/
│           ├── java/org/ecmdroid/         # instrumented JUnit4 tests
│           └── resources/                 # .eeprom/.bin/.msl fixtures used by tests
├── gradle/libs.versions.toml          # central dependency/plugin version catalog
├── scripts/mysql2sqlite.sh            # ecmspy.com MySQL dump → SQLite converter
├── scripts/mklocalversion             # git-derived build version string generator
├── README.md, README.db, CHANGES, privacy-policy.md, LICENSE
└── docs/USER_GUIDE.md, docs/DEVELOPER_GUIDE.md   (this file)
```

## 2. Building and Running

Gradle wrapper only — do not rely on a system-installed Gradle (pinned in
`gradle/wrapper/gradle-wrapper.properties`). Android Gradle Plugin 9.2.1,
`compileSdk 34`, `targetSdk 33`, `minSdk 26` (Android 8.0+). No Kotlin, no
Jetpack Compose, no Room, no Hilt/Dagger — keep new code consistent with the
existing plain-Java/AndroidX-appcompat stack.

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK (unsigned unless keystore.properties present)
./gradlew installDebug           # Build + install debug APK on a connected device
./gradlew clean                  # Clean build outputs
./gradlew lint                   # Android Lint (no enforced formatter — match surrounding style)
./gradlew connectedAndroidTest   # Instrumented tests — REQUIRES a connected device/emulator
```

APK outputs land in `app/build/outputs/apk/{debug,release}/`.

- **Signing**: create a git-ignored `keystore.properties` at repo root with
  `keyAlias`, `keyPassword`, `storeFile`, `storePassword` (see
  `app/build.gradle.kts`); without it, release builds are unsigned.
- **Version string**: `scripts/mklocalversion` runs at build time and
  generates `org.ecmdroid.VCS.LOCAL_VERSION` from git (`-g<hash>` suffix if
  untagged HEAD, `-dirty` if the working tree has uncommitted changes). Bump
  the shipped version by editing `versionCode`/`versionName` in
  `app/build.gradle.kts`.
- **Dependencies** are centralized in `gradle/libs.versions.toml`; add/bump
  there and reference via `libs.*` aliases in `app/build.gradle.kts` — never
  hardcode Maven coordinates directly in the module build file.
- **Hardware-free testing**: use [ecmsim](https://github.com/ecmdroid/ecmsim)
  (a standalone ECM protocol simulator) to exercise the app without a real
  motorcycle.

## 3. High-Level Architecture

Legacy-style Android app: one singleton hardware facade (`ECM`), one
background `Service` for continuous polling/logging, and a fragment-based UI
hung off a single `MainActivity` navigation drawer. No dependency-injection
framework — components reach each other via `getInstance(Context)` statics or
`getActivity()`.

```mermaid
flowchart LR
    subgraph UI["UI Layer (org.ecmdroid.activities / .fragments)"]
        MA[MainActivity\ndrawer + connect UI]
        FRAG[Fragments\nMain/Setup/EEPROM/DataChannel/\nLog/TroubleCode/ActiveTests]
    end
    subgraph SVC["EcmDroidService"]
        RT[ReaderThread\npolls ECM.readRTData()]
    end
    subgraph CORE["Core Domain (org.ecmdroid)"]
        ECMc[ECM\nsingleton facade]
        PDU[PDU\nwire framing]
        EEPROM[EEPROM / Page]
        VAR[Variable / BitSet / Bit]
        PROV[DatabaseVariableProvider\nDatabaseBitSetProvider]
    end
    subgraph DB["SQLite (ecmdroid.db, from assets/ecmdroid.db.gz)"]
    end
    subgraph XPORT["Transport"]
        BT[BluetoothSocket\nClassic SPP]
        BLE[SerialSocket\nBLE GATT]
        USB[UsbSerialPort]
        TCP[java.net.Socket]
    end

    MA -->|connect()| ECMc
    FRAG -->|bindService| SVC
    FRAG -->|read/write vars| ECMc
    RT -->|readRTData| ECMc
    ECMc --> PDU
    ECMc --> EEPROM
    ECMc --> VAR
    VAR --> PROV
    PROV --> DB
    EEPROM --> DB
    ECMc --> BT & BLE & USB & TCP
```

**Connect → Setup EEPROM → Poll runtime data → Log / Act** is the core data
flow:

1. **Connect** — `MainActivity` (`app/src/main/java/org/ecmdroid/activities/MainActivity.java`)
   drives device selection (paired classic Bluetooth device, BLE scan result
   via `DevicesFragment`, or a probed `UsbSerialPort`) and calls one of
   `ECM.connect(...)`.
2. **`ECM`** (`app/src/main/java/org/ecmdroid/ECM.java`) is a singleton
   facade abstracting the transport (`BluetoothSocket` / BLE `SerialSocket` /
   `UsbSerialPort` / TCP `Socket`) and the protocol variant (`STOCK` vs.
   `FACTORY_RACE`, which changes the PDU ECM address byte between `0x42` and
   `0x55`). It exposes `setupEEPROM()`, `readRTData()`, `getErrors()`,
   `runTest()`, `readEEPromPage()`, `writeEEPromPage()`.
3. **`PDU`** (`app/src/main/java/org/ecmdroid/PDU.java`) encodes/decodes the
   wire protocol: `SOH`/`EOH`/`SOT`/`EOT` framing, XOR checksum, static
   factories `getRequest()`/`setRequest()`/`commandRequest()`.
4. **EEPROM read/write** — `FetchTask`/`BurnTask` (`app/src/main/java/org/ecmdroid/task/`,
   `AsyncTask` subclasses of `ProgressDialogTask`) page through the EEPROM via
   `ECM.readEEPromPage()`/`writeEEPromPage()`; `EEPROM` (`EEPROM.java`) is a
   paginated byte array with per-page dirty tracking, so burns can be
   optimized to only write changed pages.
5. **Continuous polling** — `EcmDroidService` runs a `ReaderThread` that
   loops on `ECM.readRTData()` at a configurable interval (50–5000 ms),
   broadcasts a `REALTIME_DATA` intent every cycle, and (when recording)
   streams `[timestamp][ECM header][data]` records to a log file.
6. **Variable definitions & scaling** — `Variable` (`Variable.java`) models a
   single runtime/EEPROM parameter (type, offset, scale/translate, format,
   unit) and converts raw bytes → display value (`refreshValue()`) and back
   (`updateValue()`). Definitions come from `VariableProvider`/`BitSetProvider`,
   backed by `DatabaseVariableProvider`/`DatabaseBitSetProvider`, both of
   which cache lookups in an in-memory `HashMap` (the SQLite DB is hit
   frequently during live polling, so uncached lookups would be too slow).
7. **Logging** — recorded binary logs convert to MegaLogViewer text format
   via `Bin2MslConverter` (`app/src/main/java/org/ecmdroid/util/`).

## 4. The Wire Protocol (PDU)

Source of truth: `app/src/main/java/org/ecmdroid/PDU.java`.

### Frame layout

```
 byte:   0    1      2         3       4     5     6..N      N+1   N+2
       +----+------+---------+-------+-----+-----+---------+-----+----------+
       |SOH |sender|recipient|length |EOH  |SOT  | payload |EOT  |checksum  |
       |0x01|      |         |       |0xFF |0x02 |         |0x03 |          |
       +----+------+---------+-------+-----+-----+---------+-----+----------+
```

- `length` = `payload.length + 1` (the constructor adds 1; see
  `PDU(byte sender, byte recipient, byte[] payload)`, `PDU.java:195-208`).
- `checksum` = XOR of every byte from `sender` through `EOT` inclusive
  (`PDU.checksum()`, `PDU.java:279-285`).
- A frame is a **request** if its recipient equals the currently configured
  ECM address (`isRequest()`); a **response** if its sender equals the ECM
  address (`isResponse()`). The parser (`validate()`, `PDU.java:167-193`)
  rejects short packets, missing markers, length/size mismatches, and bad
  checksums by throwing `java.text.ParseException`.

### Addressing

| Constant | Value | Meaning |
|---|---|---|
| `DROID_ID` | `0x00` | the app (always the sender for requests) |
| `STOCK_ECM_ID` | `0x42` | ECM address for the Stock/P&A protocol |
| `RACE_ECM_ID` | `0x55` | ECM address for the Factory Race protocol |

`PDU.setProtocol(ECM.Protocol)` swaps a static `ECM_ID` field between these
two values and regenerates the cached `GET_VERSION`/`GET_RT`/`GET_CSTATE`
frames — call it (via `ECM.connect(...)`, which does this automatically)
whenever the user changes the protocol, or every subsequent PDU will be
addressed to the wrong ECM.

### Commands

| Constant | Value | Purpose | Built by |
|---|---|---|---|
| `CMD_VERSION` | `0x56` (`'V'`) | Identify the ECM (returns a version string like `BUEIB310 12-11-03`) | `PDU.getVersion()` |
| `CMD_RTDATA` | `0x43` (`'C'`) | Fetch one snapshot of runtime data | `PDU.getRuntimeData()` |
| `CMD_GET` | `0x52` (`'R'`) | Read `len` bytes from EEPROM `pageno` at `offset` | `PDU.getRequest(pageno, offset, len)` |
| `CMD_SET` | `0x57` (`'W'`) | Write `len` bytes to EEPROM `pageno` at `offset` | `PDU.setRequest(pageno, offset, data, pos, len)` |
| `ACK` | `0x06` | Acknowledgement byte seen in some responses | `PDU.isACK()` |

`CMD_GET`/`CMD_SET` payloads are `[cmd, offset, pageno, len|data...]` — note
offset comes **before** page number in the payload (`PDU.java:113-138`).

**Active test / clear-codes commands** are sent as a `CMD_SET` to page
`0x20`, offset `0`, with the payload's 4th byte selecting the function
(`PDU.commandRequest(Function)`, `PDU.java:145-147`):

| `Function` | code | `Function` | code |
|---|---|---|---|
| `ClearCodes` | `1` | `Rear_Inj` | `7` |
| `FrontCoil` | `2` | `TPS_Reset` | `8` |
| `RearCoil` | `3` | `Fan` | `9` |
| `Tachometer` | `4` | `Exh_Valve` | `0x0a` |
| `FuelPump` | `5` | `Active_Intake` | `0x0b` |
| `FrontInj` | `6` | `Shift_Light` | `0x0c` |

### Payload extraction helpers

- `getPayload()` strips the 6-byte header, returning everything up to (not
  including) `EOT`/checksum.
- `getEEPromData()` additionally strips the command/offset/page bytes: 4
  bytes for a request, 2 for a response (`PDU.java:228-232`) — this asymmetry
  exists because a `CMD_GET` **request** payload is `[cmd, offset, page,
  len]` (4 bytes before the "data", which is empty in a request) while a
  `CMD_GET`/`CMD_SET` **response** payload is `[cmd, status, data...]`.
- `getPageNr()`/`getPageOffset()` decode page/offset from a `CMD_GET`/`CMD_SET`
  **request** PDU (used when logging/debugging what was asked for).

### Request/response cycle (`ECM.sendPDU`/`receivePDU`)

`ECM.sendPDU(PDU)` writes the frame's bytes to the transport `OutputStream`
then calls `receivePDU()`, which reads the fixed 6-byte header first (to
learn the payload length), then reads exactly that many payload bytes plus
`EOT` and the checksum byte, and finally constructs a `PDU` (which
self-validates in its constructor). A read timeout (`DEFAULT_TIMEOUT` =
1000 ms) bounds each request; TCP connects use a longer 5000 ms connect
timeout (`ECM.java:113-114`).

## 5. Transport Layer

`ECM` exposes four `connect(...)` overloads (`ECM.java:153-311`), each
producing a plain `InputStream`/`OutputStream` pair that the protocol layer
above is transport-agnostic about:

| Overload | Transport | Notes |
|---|---|---|
| `connect(BluetoothDevice, Protocol)` | Classic Bluetooth SPP | `createRfcommSocketToServiceRecord(UUID 00001101-0000-1000-8000-00805F9B34FB)`; blocking socket streams used directly. |
| `connect(UsbSerialPort, Protocol)` | USB-to-serial | Wraps [usb-serial-for-android](https://github.com/mik3y/usb-serial-for-android)'s `SerialInputOutputManager`; incoming bytes are forwarded into a `PipedOutputStream`/`PipedInputStream` pair so the rest of `ECM` can treat it like a normal blocking stream. Baud rate/framing (9600 or 19200 8N1) is set by the caller — see `MainActivity.findCOMDevice()`. |
| `connect(Context, BluetoothDevice, Protocol)` | Bluetooth LE | Delegates to `de.kai_morich.simple_bluetooth_le_terminal.SerialSocket`; blocks on a monitor object until `SerialListener.onSerialConnect()`/`onSerialConnectError()` fires, then bridges `onSerialRead()` callbacks into a `PipedInputStream` the same way as USB. |
| `connect(String host, int port, Protocol)` | TCP/IP | Plain `java.net.Socket`, 5 s connect timeout. Mainly for bridges/simulators (e.g. `ecmsim`). |

`disconnect()` (`ECM.java:319-350`) branches on the concrete socket type
(`BluetoothSocket`/`java.net.Socket`/`SerialSocket`) or stops the USB
`SerialInputOutputManager` if there was no `socket` object.

### BLE transport internals (vendored library)

`de.kai_morich.simple_bluetooth_le_terminal` (kept in-tree, treat as
external/vendored — avoid unrelated edits) wraps Android's `BluetoothGatt`
API into a socket-like interface and auto-detects the peripheral's UART
profile:

| Chipset | Service UUID | Delegate class |
|---|---|---|
| TI CC254x | `0000ffe0-...-00805f9b34fb` | `Cc245XDelegate` |
| Nordic nRF51822 | `6e400001-b5a3-f393-e0a9-e50e24dcca9e` | `NrfDelegate` |
| Microchip RN4870/RN4871 | `49535343-FE7D-4AE5-8FA9-9FAFD205E455` | `Rn4870Delegate` |
| Telit Bluemod TIO 2.0 | `0000FEFB-...-00805F9B34FB` | `TelitDelegate` (credit-based flow control: separate RX/TX-credit characteristics, `minReadCredits=16`/`maxReadCredits=64`) |

`SerialSocket` negotiates MTU (up to 512 bytes, `payloadSize = MTU - 3`),
enables notifications/indications via the CCCD descriptor
(`00002902-0000-1000-8000-00805f9b34fb`), and chunks/queues large writes.
Device discovery/pairing UI lives in
`de.kai_morich.simple_bluetooth_le_terminal.DevicesFragment`, which falls
back from `BluetoothAdapter.startLeScan()` to classic
`BluetoothAdapter.startDiscovery()` when system Location Services are
disabled (required by Android 6–11 for BLE scans).

### USB device whitelist

`app/src/main/res/xml/device_filter.xml` lists the USB VID/PIDs EcmDroid
declares support for via the `USB_DEVICE_ATTACHED` intent filter (FTDI,
Silicon Labs CP210x, Prolific PL2303x, QinHeng CH34x/CH9102F, and several
CDC-ACM devices such as Arduino/Teensyduino/Pi Pico). `MainActivity.findCOMDevice()`
also independently probes via `UsbSerialProber` at connect time, so a device
not in the manifest whitelist can still be used if selected manually and
matched by the prober — the whitelist mainly controls which devices
auto-launch EcmDroid when plugged in.

## 6. ECM: the Central Facade

`ECM` (`app/src/main/java/org/ecmdroid/ECM.java`) is a per-process singleton
(`ECM.getInstance(Context)`) holding all live state: connection, protocol,
the current `EEPROM` object, and the last runtime-data snapshot (`rtData`).

Key enums:

```java
public enum Type { DDFI1, DDFI2, DDFI3; }       // Tuber / XB≤2007 / XB2008+,1125
public enum Protocol { STOCK, FACTORY_RACE; }   // ECM address 0x42 vs 0x55
```

Key methods (see the class javadoc at `ECM.java:53-74` for the intended call
order — `getInstance()` → a `connect()` overload → `setupEEPROM()` before
touching EEPROM data):

- `setupEEPROM()` — sends `PDU.getVersion()`, parses the returned version
  string (e.g. `BUEIB310 12-11-03`) to get the 5-character ECM ID, and calls
  `EEPROM.get(id, context)` to load page/size metadata from the SQLite DB.
- `readRTData()` — sends `PDU.getRuntimeData()`, stores the raw payload in
  `rtData` for `Variable`/`BitSet` lookups.
- `readEEPromPage(Page)` / `writeEEPromPage(Page)` — page through EEPROM data
  in 16-byte chunks via `CMD_GET`/`CMD_SET`. Page 0 is special-cased: it's
  addressed as page `0xFF` and, on write, only two variables
  (`Variables.LFuel1`, `Variables.KBaro` — see `PAGE_ZERO_VARS_TO_WRITE`,
  `ECM.java:121-124`) are ever written back, to avoid clobbering
  factory-calibration bytes that share page 0 with user-editable ones.
- `getErrors(ErrorType)` — decodes current (`CDiag0`-`CDiag9`), recent
  (`EDiag0`-`EDiag10`), or stored (`HDiag0`-`HDiag9`) diagnostic bitsets into
  `Error` objects (code + description).
- `runTest(Function)` — sends `PDU.commandRequest(function)` for active
  tests/TPS reset/clear-codes.
- `getEEPROMValue(name)` / `getRuntimeValue(name)` — look up a `Variable` by
  name via `VariableProvider`, then refresh it from `eeprom.getBytes()` or
  `rtData` respectively.
- `setEEPROMValue(Variable)` / `setEEPROMBits(BitSet)` — write a new value
  into the in-memory `EEPROM` byte array and mark the containing `Page` as
  **touched** (dirty) via `EEPROM.Page.touch(offset, length)`. Nothing is
  sent to the ECM until a `BurnTask` runs.

## 7. EEPROM / Variable / BitSet Data Model

### `EEPROM` (`EEPROM.java`)

A paginated byte array plus metadata:

- `EEPROM.get(name, Context)` — queries the `eeprom`/`pages` tables for the
  ECM identified by `name` (first 5 chars of the version string), builds an
  `EEPROM` with an `ArrayList<Page>` and an empty `byte[length]` buffer.
  Page 0 is placed at the **end** of the buffer (`pg.start = eeprom.length -
  pg.length`), all other pages are laid out sequentially from offset 0 —
  mirroring how the physical ECM addresses page 0 as `0xFF`.
- `EEPROM.load(Context, id, InputStream)` — builds an EEPROM template via
  `get()`, overwrites its bytes with a loaded file's contents, and marks
  every page touched (so a freshly-loaded file can be burned in full).
- `EEPROM.size2id(Context, length)` — maps a raw file's byte length back to
  one or more candidate ECM IDs (some ECM types share a EEPROM size, hence
  an array return and a disambiguation prompt in `EEPROMFragment`).
- Inner class `Page`: `nr`, `length`, `start` (byte offset into the
  `EEPROM`'s buffer), `touched`. `getBytes(offset, length, buffer,
  bufferPos)` copies from `start + offset`. `touch()`/`touch(offset, length)`
  mark it dirty; `BurnTask` only re-writes touched pages when "optimized
  burning" is enabled.
- `isTouched()` (any page dirty) gates the Setup/EEPROM screens' Save/Apply
  buttons and the "overwrite unsaved changes?" confirmation before a Fetch.
- `isEepromRead()` distinguishes "we have real page bytes from a Fetch/Load"
  from "we only have page/size metadata" — used to decide whether to show
  serial/mfg-date/calibration fields on the Main screen.

### `Variable` (`Variable.java`)

Represents one named runtime or EEPROM parameter:

- `DataType`: `SCALAR`, `VALUE`, `BITS`, `BITFIELD`, `ARRAY`, `AXIS`, `TABLE`,
  `MAP`, `STRING`.
- `refreshValue(byte[] tmp)` reads a little-endian multi-byte integer at
  `offset` (width = element size), and for numeric types applies
  `scale * raw + translate` before formatting via `DecimalFormat` (bit
  types are kept as raw shorts and formatted as binary strings instead).
- `updateValue(byte[] bytes)` is the inverse: `(displayValue - translate) /
  scale`, written back little-endian at `offset`.
- `parseValue(String)`/`parseValueAt(...)` parse user text input, throwing
  `NumberFormatException` on bad input (caught by the calling fragment and
  surfaced as a toast).

### `BitSet`/`Bit` (`BitSet.java`, `Bit.java`)

A `BitSet` is up to 8 `Bit`s packed into one byte (`bits[bitNr]`, 0-7). Each
`Bit` carries a diagnostic `code` (DTC, e.g. `P0001`) and a human `remark`
where applicable. `BitSet.updateValue(byte[])` masks out only the bits it
owns and ORs in new values, so multiple `BitSet`s can safely share a byte.
Negative offsets throughout this layer (`Bit`, `BitSet`, `Variable`) mean
"relative to the end of the buffer", i.e. page 0.

### `Error` (`Error.java`)

Simple `{code, description, ErrorType}` tuple; `ErrorType` is `CURRENT`
(from `CDiag*` runtime bitsets), `RECENT` (`EDiag*`), or `STORED` (`HDiag*`
EEPROM bitsets). Built exclusively by `ECM.getErrors()`.

## 8. Bundled ECM-Definitions Database

The app ships a gzip-compressed SQLite database,
`app/src/main/assets/ecmdroid.db.gz`, sourced from ecmspy.com MySQL dumps.
Despite the `.gz` suffix, code opens it as `assets.open("ecmdroid.db")`
(`DBHelper.setupDB()`, `DBHelper.java:65-66`) — Android's `AssetManager`
transparently gzip-decompresses any packaged asset stored under a `.gz`
name when it's opened without that suffix, so no explicit decompression
code is needed.

`DBHelper` (`SQLiteOpenHelper`) copies the asset into the app's private DB
directory on first launch or whenever `DB_VERSION` (`DBHelper.java:40`)
changes; `EcmDroidApp.onCreate()` calls `setupDB()` before any other
component runs.

### Schema

| Table | Purpose |
|---|---|
| `eeprom` | One row per ECM ID: `name`, `category`, `type` (DDFI/-2/-3 string), `size`, `xsize` (total EEPROM length incl. page 0). |
| `pages` | Page layout per `category`: `page` number, `size` in bytes. |
| `rtoffsets` | Runtime-data variable offsets: `category`, `varname`, `type`, `offset`, `size`, `scale`, `translate`, `format`, `secret`. |
| `eeoffsets` | EEPROM variable offsets: `category`, `varname`, `type`, `offset`, `elemsize`, `cols`, `rows`, `scale`, `translate`, `format`, `secret`. |
| `names` | Display metadata per `varname`: `origname`, `name`, `secret`, `description`, `units`, `remark`. |
| `bits` | Per-`varname` bit layout: `byte`, and 8 columns each of `bitname{1-8}`, `bit{1-8}`, `dtc{1-8}`. |

`secret=1` rows are internal/debug parameters filtered out of the app's
variable pickers (`... AND secret=0` in the `DatabaseVariableProvider`/
`DatabaseBitSetProvider` queries).

`DatabaseVariableProvider`/`DatabaseBitSetProvider` (`app/src/main/java/org/ecmdroid/`)
run raw SQL via `SQLiteDatabase.rawQuery()` (no ORM) and cache every lookup
in a `HashMap` keyed by `rt#<name>`/`ee#<name>` (variables) or `<name>`
(bitsets); the cache is cleared whenever the connected ECM's ID changes.
**Follow this pattern for any new DB-backed lookup** — the DB is queried on
every live-poll cycle, so uncached queries would be a real perf problem.

### Regenerating the bundled database

See [`README.db`](../README.db) for the exact steps; summary:

1. Import an ecmspy.com MySQL backup into a scratch MySQL DB.
2. `sh scripts/mysql2sqlite.sh -u<user> -p<password> <db> > ecmdroid.sq3`
   (awk-based MySQL-dump → SQLite statement converter).
3. Strip all column `COMMENT`s from the generated SQL (unsupported by
   SQLite3).
4. `sqlite3 assets/ecmdroid.db < ecmdroid.sq3` to build the file.
5. `gzip -f assets/ecmdroid.db` to replace `assets/ecmdroid.db.gz`.
6. **Bump `DB_VERSION` in `DBHelper.java`** so installed apps re-extract the
   new database on next launch — forgetting this step means the new data is
   bundled but never actually installed on any upgraded device.

## 9. Background Service and Threading

No coroutines/RxJava/Executors anywhere in the app. Three threading patterns
are used, consistently:

1. **`AsyncTask` subclasses of `ProgressDialogTask`** (`app/src/main/java/org/ecmdroid/task/`)
   for one-shot ECM I/O with a modal progress dialog:
   - `ProgressDialogTask` (base): freezes screen orientation
     (`Utils.freezeOrientation`) for the duration, shows a non-cancelable
     `ProgressDialog`, restores orientation and displays an error `Toast` if
     `doInBackground()` returns a non-null `Exception`.
   - `FetchTask`: confirms overwrite of unsaved changes, then
     `setupEEPROM()` + `readRTData()` + `readEEPromPage()` for every page.
   - `BurnTask`: shows a "burn at your own risk" confirmation, verifies the
     live ECM's version matches the EEPROM's ID before writing (refuses via
     `cancel(true)` on mismatch), writes touched-only or all pages depending
     on the `enable_fast_burning` preference, and clears dirty flags
     (`eeprom.saved()`... — actually pages' `touched` flags) on success.
   - `MainActivity.ConnectTask` extends `FetchTask` to combine
     connect-then-initial-fetch into one progress dialog.

2. **`EcmDroidService.ReaderThread`** (`EcmDroidService.java`) — a plain
   `Thread` polling `ecm.readRTData()` at a configurable interval (250 ms
   default, 50 ms minimum), using `synchronized`/`wait()`/`notify()` to
   sleep when neither reading nor recording is active rather than
   busy-looping. Each cycle:
   - On success: broadcasts `EcmDroidService.REALTIME_DATA`; if recording,
     appends `[4-byte centisecond timestamp][raw rtData bytes]` to the open
     log `FileOutputStream`.
   - On failure: increments a `readFailures` counter and logs, but keeps
     running (no crash/backoff-to-disconnect logic — a flaky link just shows
     up as gaps in the log/UI).
   - `startRecording(FileOutputStream, interval, ECM)`/`stopRecording()`
     toggle the recording flag and post `RECORDING_STARTED`/`RECORDING_STOPPED`
     broadcasts; a persistent notification (channel `ecmdroid_logrecorder`)
     is shown while recording, deep-linking back to `LogFragment`.

3. **`ServiceConnection`/`BroadcastReceiver`** — `MainActivity`/`DataChannelFragment`/`LogFragment`
   bind to `EcmDroidService` via `ServiceConnection`, and register a
   `BroadcastReceiver` for `REALTIME_DATA`/`RECORDING_STARTED`/`RECORDING_STOPPED`
   in `onResume()`, unregistering in `onPause()`. UI never talks to the
   `ReaderThread` directly — always through the service's public
   start/stop/accessor methods, which are what stay thread-safe
   (`synchronized`) against the reader thread.

## 10. UI Layer

`MainActivity` (`app/src/main/java/org/ecmdroid/activities/MainActivity.java`)
is the single navigation-drawer host; `switchToFragment(int id)` swaps the
`R.id.content_frame` fragment. A `isTransactionSafe` flag defers fragment
switches that arrive while the activity is mid-transition (e.g. during
rotation), executing them once `onPostResume()` marks it safe again — needed
because `FragmentTransaction`s during `onSaveInstanceState()` throw.

| Drawer item | Fragment | Purpose |
|---|---|---|
| ECM Information | `MainFragment` | identity fields, protocol picker |
| Trouble Codes | `TroubleCodeFragment` | read/clear current & stored DTCs |
| Active Tests | `ActiveTestsFragment` | fire `PDU.Function` tests, TPS reset |
| Data Channels | `DataChannelFragment` | 5-slot live gauge list, bound to `EcmDroidService` |
| Setup | `SetupFragment` | `PreferenceFragment` rendering `res/xml/ecm_setup.xml`, bidirectionally bound to `Variable`/`BitSet` |
| Log Recorder | `LogFragment` | start/stop binary logging, optional `.msl` conversion |
| EEPROM | `EEPROMFragment` | raw hex `GridView` + `CellEditorDialogFragment` byte editor, file save/load, Fetch/Burn |
| Torque Values | `TorqueValuesFragment` (legacy `PreferenceActivity`) | static reference data from `res/xml/torque_values.xml` |
| Settings | `PrefsActivity` | connection type/host/port, storage location, burn toggles, keep-screen-on |
| About | `AboutActivity` | WebView of `assets/about.html` |

`SetupFragment` is worth understanding as the canonical example of the
Setup-editing pattern: it recursively walks the `PreferenceScreen` tree from
`res/xml/ecm_setup.xml`, resolves each `Preference`'s `key` against either a
`BitSet` (key syntax `varname:bit[,bit...]`, matched by `Constants.BIT_PATTERN`)
or a plain `Variable`, sets `pref.setPersistent(false)` (Android's own
preference storage is bypassed entirely — the `ECM`/`EEPROM` singleton *is*
the persistence layer), and on `onPreferenceChange()` calls
`ecm.setEEPROMValue(Variable)`/`ecm.setEEPROMBits(BitSet)`, revealing a
Save/Apply button once `ecm.isConnected() && ecm.getEEPROM().isTouched()`.

## 11. Async Task Pattern

Every long-running ECM/file operation follows the same shape — a two-phase
`start()`/`doInBackground()` split so any needed confirmation dialog runs
*before* the background work begins:

```java
class SomeTask extends ProgressDialogTask {
    SomeTask(Activity ctx) { super(ctx, ctx.getString(R.string.some_title)); }

    void start() {
        if (/* needs confirmation, e.g. unsaved changes */) {
            new AlertDialog.Builder(context)
                .setPositiveButton(..., (d, w) -> execute())
                .setNegativeButton(..., (d, w) -> d.cancel())
                .show();
        } else {
            execute();
        }
    }

    @Override protected Exception doInBackground(Void... v) {
        try {
            publishProgress("...");   // updates the ProgressDialog message
            // ECM I/O here
            return null;               // success
        } catch (Exception e) {
            return e;                  // ProgressDialogTask shows it as a Toast
        }
    }
}
```

Exceptions are **returned**, not thrown, from `doInBackground()` — this is
the app-wide error-reporting convention for background work; `onPostExecute()`
in the base class turns a non-null result into a `Toast`.

## 12. Binary Log Format and MSL Conversion

### Binary log format (written by `EcmDroidService.ReaderThread`)

```
[5-byte ECM ID header]
repeat:
  [4-byte int timestamp, centiseconds since recording start]
  [rtLen-byte runtime buffer]     // last byte of the buffer is an XOR checksum
```

`rtLen` (and therefore the on-wire runtime buffer size) depends on ECM
category:

| Category | ECM IDs (prefix) | `rtLen` |
|---|---|---|
| 1 (DDFI-1/Tuber) | `BUEKA`, `BUEJA` | 99 |
| 2 (DDFI-2) | `BUECB`, `BUEGB`, `BUEIB`, `BUEIC`, `B2RIB` | 103–107 |
| 3 (DDFI-3) | `BUEOD`, `BUEWD`, `BUEYD`, `BUEZD`, `BUE1D`, `BUE2D`, `BUE3D`, `B3R1D`, `B3R3D` | 135 |

### Conversion (`Bin2MslConverter`, `app/src/main/java/org/ecmdroid/util/Bin2MslConverter.java`)

`convert(InputStream, PrintWriter)` (an `Observable`, so callers such as
`LogFragment.StopTask` can `addObserver()` for progress-string updates every
~500 ms and support cancellation via `cancel()`):

1. Reads the 5-byte ECM-ID header, maps it to category and loads the
   matching **offset table** resource — `runtime1.tab`/`runtime2.tab`/`runtime3.tab`
   (`app/src/main/resources/`), a tab-separated file with one row per
   exported parameter (`export name`, byte `offset`, `size` 1 or 2, `scale`,
   `translate`, integer/float `format`).
2. Per record: validates the trailing XOR checksum (byte-for-byte against
   `rtBuffer[1..rtLen-2]`); silently discards the record on mismatch rather
   than aborting the whole conversion.
3. Computes derived columns not present verbatim in the offset table:
   `Gego`/`Gego1` (closed-loop EGO correction, or open-loop AFV fallback,
   selected by a bit in the `Flags2` byte) and a composite `Engine` state
   byte assembled from `Flags1` bits plus warmup/acceleration thresholds.
4. Writes a MegaLogViewer-compatible `.msl`: header line
   `"EcmDroid/Bin2Msl <ECM_TYPE>"`, a tab-separated column-name header, then
   one tab-separated row per valid record (`Number`, `Time` in seconds,
   `Gego`[, `Gego1` for category 3], `Engine`, then every exported
   parameter), CRLF line endings for MegaLogViewer compatibility.

If you add a new exportable runtime parameter, it goes in the SQLite
`rtoffsets`/`names` tables (for live display) **and** in the matching
`runtime{1,2,3}.tab` resource (for log conversion) — the two are
independent data sources that happen to describe the same underlying bytes.

## 13. Testing

Only **instrumented** tests exist, under `app/src/androidTest/java/org/ecmdroid/`
(JUnit 4 + `AndroidJUnit4` runner; Espresso is a declared dependency but
unused by current tests). There is no `app/src/test/` JVM unit-test source
set and no CI pipeline configured in this repo — tests are run locally.

| Test class | Covers |
|---|---|
| `TestPDU` | Frame construction/parsing, checksum validation |
| `TestECM` | ECM identification / EEPROM setup flow |
| `TestEEPROM` | Page layout, load/size2id |
| `TestVariableProvider` | DB-backed variable lookups and scaling |
| `TestBitSetProvider` | DB-backed bitset/DTC lookups |
| `TestBin2Msl` | Binary log → `.msl` conversion, using fixture logs |
| `TestUtils` | Misc helpers (hexdump, string checks, etc.) |

Fixtures live in `app/src/androidTest/resources/` — real captured `.eeprom`
dumps (`BUEIB.eeprom`, `BUE2D.eeprom`), binary runtime snapshots
(`RT_BUEIB.bin`, `RT_BUEIB242.bin`), binary logs (`BUEIB_log.bin`,
`BUE2D_log.bin`) and a reference `.msl` conversion output (`BUE2D_log.msl`).
**Reuse these fixtures** for new protocol/converter tests rather than
inventing new binary formats or mocking — there is no Robolectric/mocking
framework configured, and the project's existing convention is to test
against real captured data.

```bash
./gradlew connectedAndroidTest         # all instrumented tests
./gradlew connectedDebugAndroidTest    # debug variant only
```

Naming convention for new tests: `Test<Component>.java`, methods prefixed
`test`, placed alongside the existing classes under
`app/src/androidTest/java/org/ecmdroid/`.

## 14. Code Conventions

- **Language**: plain Java 8 (`sourceCompatibility`/`targetCompatibility` =
  `1.8`). Gradle build files are Kotlin DSL (`.kts`), but there is no Kotlin
  *application* code — don't introduce Kotlin without discussing it first.
- **Naming**: classes `PascalCase`; methods `camelCase`; constants
  `UPPER_SNAKE_CASE`; every class carries a `private static final String
  TAG` for `Log.d/i/w/e`.
- **Error handling**: exceptions, not return codes. `PDU`'s constructor
  throws `java.text.ParseException` on malformed frames; failures surface to
  the user via `Toast` or `AlertDialog`, generally by returning the
  `Exception` from an `AsyncTask.doInBackground()` (see §11).
- **Dependency management**: no DI framework — singletons via static
  `getInstance(Context)`; fragments reach their host `Activity`/`Service`
  via `getActivity()`/`ServiceConnection`.
- **State**: the `ECM` singleton is the single source of truth for
  connection/protocol/EEPROM state; `SharedPreferences` hold *user* settings
  (protocol index, storage location URI, log interval, keep-screen-on,
  per-ECM data-channel selection); `MainActivity` persists the active drawer
  fragment across rotation via `onSaveInstanceState`.
- **Database access**: raw SQL via `SQLiteDatabase.rawQuery()`, never an
  ORM. New lookups should get an in-memory `HashMap` cache exactly like
  `DatabaseVariableProvider`/`DatabaseBitSetProvider` — the DB is queried
  every live-poll cycle.
- **Resource naming**: layouts named after their feature (`main.xml` ↔
  `MainFragment`, `log.xml` ↔ `LogFragment`); menus `*_menu.xml`/
  `main_drawer.xml`; drawables descriptive (`ic_connected.xml`).
- **Vendored code**: `de.kai_morich.simple_bluetooth_le_terminal` is
  third-party BLE code kept in-tree — treat it as external and avoid
  unrelated edits/reformatting.

## 15. Common Pitfalls

- **Forgetting to bump `DBHelper.DB_VERSION`** after regenerating
  `ecmdroid.db.gz` — installed apps only re-extract the asset when the
  version constant changes (§8).
- **Editing page 0 semantics without reading `PAGE_ZERO_VARS_TO_WRITE`** —
  page 0 has a restricted write set on purpose (`ECM.java:121-124`); adding
  a new page-0-writable variable to the Setup UI without also adding it here
  means edits appear to work but are silently never burned.
- **Changing the PDU frame shape without updating both request and
  response paths** — `getPayload()` vs. `getEEPromData()` intentionally
  strip a different number of header bytes depending on `isRequest()`
  (§4); a naive change to one usually breaks the other.
- **New runtime-exported parameters need two edits**, not one: the SQLite
  `rtoffsets`/`names` tables (live UI) and the `runtime{1,2,3}.tab` resource
  (log conversion) — see §12.
- **Switching `ECM.Protocol` without going through `ECM.connect(...)`** —
  `PDU.setProtocol()` must run before any further PDU is built, or requests
  go to the wrong `ECM_ID` and silently time out.
- **Un-cached DB lookups on the polling hot path** — anything called from
  `EcmDroidService.ReaderThread` runs up to 20×/second (50 ms interval);
  always route new variable/bitset lookups through the provider caches.
