package com.example.bitonichallenge2

import android.content.Intent
import android.location.Location
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.bitonichallenge2.model.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions

class MapsActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    var mMap: GoogleMap? = null

    private var isGameOnGoingMap : Boolean = false
    private var coordinatesFuelMap : MutableList<Fuel> = mutableListOf()
    private var coordinatesUserMap : LatLng = LatLng(0.0,0.0)

    private var userMarker : Marker? = null

    private var fuelToCatchIndex = -1
    private var isDistanceClose = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        requestPermissions()
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync{
            mMap = it
            updateFuelMapLocation(coordinatesFuelMap)
        }

        subscribeToObservers()

        btnStartGame.setOnClickListener {
            sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
        }
        btnPauseGame.setOnClickListener {
            sendCommandToService(ACTION_PAUSE_SERVICE)
        }

        btnCatch.setOnClickListener{
                Toast.makeText(this,"Caught ${coordinatesFuelMap[fuelToCatchIndex].litres} litres!", Toast.LENGTH_SHORT).show()
                deleteMarkerFromListAndUpdateMap(fuelToCatchIndex, GameService.coordinatesUser.value!!)
                btnCatch.isEnabled = false
                isDistanceClose = false
        }
    }

    // Clears whole map, updates map with user marker, all fuels except from the removed fuel
    private fun deleteMarkerFromListAndUpdateMap(index:Int,currentLatLng: LatLng) {
        coordinatesFuelMap.removeAt(index)
        mMap?.clear()
        updateUserLocation(currentLatLng)
        updateFuelMapLocation(coordinatesFuelMap)
    }


    private fun subscribeToObservers(){
        GameService.isGameOngoing.observe(this,{
            isGameOnGoingMap = it
            updateUiButtons(it)
        })



        GameService.coordinatesUser.observe(this,{
            coordinatesUserMap = it
            updateUserLocation(it)
            userAndFuelDistance(it)
        })

        GameService.coordinatesFuel.observe(this,{
            coordinatesFuelMap = it
            updateFuelMapLocation(coordinatesFuelMap)
        })
    }
    private fun updateUiButtons(isGamePlaying : Boolean){
        if(isGamePlaying){
            btnStartGame.visibility = View.GONE
            btnPauseGame.visibility = View.VISIBLE
            btnCatch.visibility = View.VISIBLE
        }else if (!isGamePlaying && GameService.isPaused.value==true){
            btnStartGame.text = "Resume"
            btnStartGame.visibility = View.VISIBLE
            btnPauseGame.visibility = View.GONE
            btnCatch.visibility = View.GONE
        }
    }

    private fun updateUserLocation(userLatLng: LatLng){

        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 18f))
        userMarker?.remove()
        userMarker = mMap?.addMarker(MarkerOptions().position(userLatLng).title("It's me"))


    }

    private fun userAndFuelDistance(latLng: LatLng){
        CoroutineScope(Dispatchers.Default).launch{
            if(!isDistanceClose){
                for(i in coordinatesFuelMap.indices){

                    // Prompts user to catch fuel if he is close to a fuel marker
                    if (distanceFromUserAndMarker(locationConverter(latLng), coordinatesFuelMap[i].coords) < MAX_DISTANCE_TO_CATCH_FUEL ){
                        CoroutineScope(Dispatchers.Main).launch {
                            btnCatch.isEnabled = true
                            fuelToCatchIndex = i
                            isDistanceClose = true
                        }
                        break
                    }
                }

            } else{
                // If user walks away from catchable position
                if(distanceFromUserAndMarker(locationConverter(latLng),coordinatesFuelMap[fuelToCatchIndex].coords)> MAX_DISTANCE_TO_CATCH_FUEL){
                    CoroutineScope(Dispatchers.Main).launch {
                        btnCatch.isEnabled = false
                        fuelToCatchIndex = -1
                        isDistanceClose = false
                    }
                }
            }
        }
    }

    private fun distanceFromUserAndMarker(currentLocation : Location, fuelLocation: LatLng) : Float{
        return currentLocation.distanceTo(Location("coords").apply {
            latitude = fuelLocation.latitude
            longitude = fuelLocation.longitude
        })
    }

    private fun locationConverter(latLng: LatLng) : Location {
        val location = Location("user").apply {
            latitude = latLng.latitude
            longitude = latLng.longitude
        }
        return location
    }
    private fun updateFuelMapLocation(listOfFuels : MutableList<Fuel>){
        // This won't fire in screen rotation from observer because mMap is null. It will fire from observer when we start our service.
        // So, we put this on mMap async (onCreate) and it fires from there when we rotate
        for (fuel in listOfFuels){
            mMap?.addMarker(MarkerOptions().position(fuel.coords).title("It's me"))

        }
    }




    private fun sendCommandToService(action:String){
        Intent(this, GameService::class.java).also {
            it.action = action
            this.startService(it)
        }
    }
    private fun requestPermissions(){
        if(utils.hasPermissions(this)){
            return
        }
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){
            EasyPermissions.requestPermissions(
                this,
                "Βρε μπαγλαμά, πως θα παίξεις αν δε δεχτείς;",
                REQUEST_CODE_PERMISSIONS,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
            )
        }else{
            EasyPermissions.requestPermissions(
                this,
                "Βρε μπαγλαμά, αν δε βλέπουμε την τοποθεσία σου ακόμα κι όταν χέζεις, πως θα παίξεις;",
                REQUEST_CODE_PERMISSIONS,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode,permissions,grantResults,this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if(EasyPermissions.somePermissionPermanentlyDenied(this,perms)){
            AppSettingsDialog.Builder(this)
                .setTitle("Γαμιέσαι λίγο, ε;")
                .setRationale("Κοίτα να δεις εδώ τενεκέ, αν δε δώσεις άδεια για να μπούμε μέσα και να τα κάνουμε πουτάνα όλα, δε γίνεται δουλειά. Τράβα στις ρυθμίσεις.")
                .build()
                .show()
        } else{
            requestPermissions()
        }
    }

}