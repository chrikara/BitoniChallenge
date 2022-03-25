package com.example.bitonichallenge2.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import pub.devrel.easypermissions.EasyPermissions
import kotlin.random.Random

object utils {

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
            1-> LITRES_SMALL
            2-> LITRES_MEDIUM
            3-> LITRES_LARGE
            else -> LITRES_VERY_LARGE
        }
    }
    private fun dimensionsOfFuel(randomLitres : Int):Int{
        return when(randomLitres){
            LITRES_SMALL -> SIZE_SMALL
            LITRES_MEDIUM -> SIZE_MEDIUM
            LITRES_LARGE -> SIZE_LARGE
            else -> SIZE_VERY_LARGE
        }
    }

    // Creates a mutableLisOf<Fuel> that adds Fuel instances within user's circle that has radius = 120m (RADIUS)
    fun generateFuelListWithin120mRad(currentlocation : Location) : MutableList<Fuel> {
        val mutableFuelList = mutableListOf<Fuel>()
        val randomLocation = Location("random")

        var latRand : Double
        var longRand : Double
        var litres : Int

        repeat (INITIAL_FUEL_MARKERS){
            randomLocation.apply {
                latitude = 0.0
                longitude = 0.0
            }
            while(currentlocation.distanceTo(randomLocation)> RADIUS){
                latRand = Random.nextDouble(currentlocation.latitude-0.004,currentlocation.latitude+0.004)
                longRand = Random.nextDouble(currentlocation.longitude-0.004,currentlocation.longitude+0.004)
                randomLocation.latitude = latRand
                randomLocation.longitude = longRand
            }

            litres = randomLitres()
            mutableFuelList.add(Fuel(LatLng(randomLocation.latitude,randomLocation.longitude), litres, dimensionsOfFuel(litres)))
        }

        return mutableFuelList
    }

    fun bitmapDescriptorFromVector(context: Context, vectorResId: Int,dimensions : Int): BitmapDescriptor? {
        return ContextCompat.getDrawable(context, vectorResId)?.run {
            setBounds(0, 0, dimensions, dimensions)
            val bitmap = Bitmap.createBitmap(dimensions, dimensions, Bitmap.Config.ARGB_8888)
            draw(Canvas(bitmap))
            BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    }
}
