package com.example.bitonichallenge2.model

import com.google.android.gms.maps.model.LatLng

const val REQUEST_CODE_PERMISSIONS = 0

const val ACTION_START_OR_RESUME_SERVICE = "ACTION_START_OR_RESUME_SERVICE"
const val ACTION_PAUSE_SERVICE = "ACTION_PAUSE_SERVICE"
const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
const val ACTION_SHOW_MAPS_ACTIVITY = "ACTION_SHOW_MAPS_ACTIVITY"

val XANTHI_CENTER = LatLng(41.1396, 24.8877)
const val ZOOM_CAMERA = 17.5f

const val MAX_DISTANCE_TO_CATCH_FUEL = 20f
const val RADIUS = 120f

const val INITIAL_FUEL_MARKERS = 10

const val NOTIFICATION_CHANNEL_ID = "game_channel"
const val NOTIFICATION_CHANNEL_NAME = "game_ongoing"
const val NOTIFICATION_ID = 1  // At least to 1 to work