package com.example.bitonichallenge2.model

import android.content.Context
import android.os.Build
import com.google.android.gms.maps.model.LatLng
import pub.devrel.easypermissions.EasyPermissions

object Utils {

    fun hasPermissions(context: Context) =
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){
            EasyPermissions.hasPermissions(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
            )
        }else{
            EasyPermissions.hasPermissions(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            )
        }

    var fuelRandomCoordinatesList = mutableListOf(
        LatLng(41.1438, 24.9001),
        LatLng(41.1445, 24.8988),
        LatLng(41.1439, 24.8979),
        LatLng(41.1434, 24.8988)
    )
}
