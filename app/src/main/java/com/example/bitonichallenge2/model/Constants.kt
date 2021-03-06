package com.example.bitonichallenge2.model

import com.google.android.gms.maps.model.LatLng

const val REQUEST_CODE_PERMISSIONS = 0

const val SHARED_START = "SHARED_START"
const val SHARED_START_BOOLEAN = "SHARED_START_BOOLEAN"

const val ACTION_START_OR_RESUME_SERVICE = "ACTION_START_OR_RESUME_SERVICE"
const val ACTION_PAUSE_SERVICE = "ACTION_PAUSE_SERVICE"
const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
const val ACTION_SHOW_MAPS_ACTIVITY = "ACTION_SHOW_MAPS_ACTIVITY"

val XANTHI_CENTER = LatLng(41.1396, 24.8877)
const val ZOOM_CAMERA = 19.5f

const val MY_INTERVAL = 1000L
const val MY_MAXIMUM_INTERVAL = 200L

const val DISTANCE_FUEL_IS_CATCHABLE = 5f
const val RADIUS = 30f
const val INITIAL_FUEL_MARKERS = 10

const val ANIMATION_DURATION = 200L
const val ROTATION_BUTTON = 360f

const val NOTIFICATION_CHANNEL_ID = "game_channel"
const val NOTIFICATION_CHANNEL_NAME = "game_ongoing"
const val NOTIFICATION_ID = 1  // At least to 1 to work

const val SIZE_SMALL = 80
const val SIZE_MEDIUM = 100
const val SIZE_LARGE = 120
const val SIZE_VERY_LARGE = 160

const val LITRES_SMALL = 10
const val LITRES_MEDIUM = 15
const val LITRES_LARGE = 20
const val LITRES_VERY_LARGE = 40

const val USER_PLAYING = "Playing"
const val USER_PAUSED = "Paused"
const val USER_STOPPED = "Stopped"