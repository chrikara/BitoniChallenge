package com.example.bitonichallenge2.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.*
import pub.devrel.easypermissions.EasyPermissions
import kotlin.random.Random

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

    private fun randomLitres() : Int {
        return when (Random.nextInt(1,5)){
            1-> 10
            2-> 15
            3-> 20
            else -> 25
        }
    }

    fun generateFuelListWithin120mRad(currentlocation : Location) : MutableList<Fuel> {
        val mutableFuelList = mutableListOf<Fuel>()
        val randomLocation = Location("random")

        var latRand : Double
        var longRand : Double

        repeat (INITIAL_FUEL_MARKERS){
                randomLocation.apply {
                latitude = 0.0
                longitude = 0.0
            }
            while (currentlocation.distanceTo(randomLocation) > DIAMETER){
                latRand = Random.nextDouble(currentlocation.latitude-0.0071,currentlocation.latitude+0.0071)
                longRand = Random.nextDouble(currentlocation.longitude-0.0139999,currentlocation.latitude+0.0139999)
                randomLocation.latitude = latRand
                randomLocation.longitude = longRand
            }
            mutableFuelList.add(Fuel(LatLng(randomLocation.latitude,randomLocation.longitude), randomLitres()))
        }

        return mutableFuelList
    }

    fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
        return ContextCompat.getDrawable(context, vectorResId)?.run {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
            draw(Canvas(bitmap))
            BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    }



}
