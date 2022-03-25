package com.example.bitonichallenge2.model

import com.google.android.gms.maps.model.LatLng

data class Fuel(
        val coords : LatLng,
        val litres : Int,
        val dimensions : Int
) {
}