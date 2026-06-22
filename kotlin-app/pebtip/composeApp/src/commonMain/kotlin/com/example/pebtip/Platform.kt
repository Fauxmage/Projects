package com.example.pebtip

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

/*
 * Function to get the local IP address of the device.
 * Only for displaying while Dev is enabled for the Pebble.
 */
expect fun getLocalIpAddress(): String