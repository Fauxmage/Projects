# Project Repository

This repository collects the codebase developed for my masters thesis, along with a coursework project. 
The work centers on collecting raw accelerometer data from a smartwatch, transmitting it through a mobile app, and hosting it on a web server with a dashboard for inspection.

**Live dashboard:** https://www.ubr004.xyz/
A simple front-end for getting an overview of the data stored in the database along with statistics from persisted data.

## Projects

| Project | Description |
|---|---|
| **Kotlin KMP app** | A Kotlin Multiplatform application. The companion app for the Pebble smartwatch, responsible for collecting and forwarding raw accelerometer data. |
| **Pebble smartwatch app** | Firmware/app for the Pebble smartwatch that captures raw accelerometer data. |
| **Distributed logging system** | A distributed logging system built on the Multi-Paxos consensus protocol (coursework assignment). |
| **Web server (back-end & front-end)** | The back-end and front-end serving the data collected from the Pebble. |

## Where to look first

For the Kotlin app, the most relevant code lives under `composeApp/`, and in particular the Android-specific implementation:

- [`PebbleBackgroundService.kt`](https://github.com/Fauxmage/Projects/blob/main/kotlin-app/pebtip/composeApp/src/androidMain/kotlin/com/example/pebtip/service/PebbleBackgroundService.kt) — the background service that handles communication with the Pebble watch.
