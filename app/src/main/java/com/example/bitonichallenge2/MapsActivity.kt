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
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.lang.Exception

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, EasyPermissions.PermissionCallbacks {

    private lateinit var mMap: GoogleMap
    private lateinit var fuelsOnMap: MutableList<Fuel>
    var isGameOngoing : Boolean = false
    var isDistanceClose : Boolean = false

    var fuelToCatchIndex : Int = -1
    var userMarker : Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        requestPermissions()
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fuelsOnMap = mutableListOf()

        btnSendCommand.setOnClickListener {
            sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
            userMarker = mMap.addMarker(MarkerOptions().position(LatLng(0.0,0.0)).title("Marker in Sydney").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)))

        }


        btnStopGame.setOnClickListener{
            sendCommandToService(ACTION_STOP_SERVICE)
        }

        btnCatch.setOnClickListener{
            try{
                Toast.makeText(this,"Caught ${fuelsOnMap[fuelToCatchIndex].litres}!", Toast.LENGTH_SHORT).show()
                deleteMarkerFromListAndUpdateMap(fuelToCatchIndex, GameService.coordinatesUser.value!!)
                it.visibility = View.INVISIBLE
                isDistanceClose = false
            }catch (e:Exception){e.printStackTrace()}
        }
        subscribeToObservers()


    }


    private fun subscribeToObservers(){
        GameService.isGameOngoing.observe(this,{
        })
        GameService.coordinatesUser.observe(this,{
            updateUserMarker(it)
            val currentLocation = Location("user").apply {
                latitude = it.latitude
                longitude = it.longitude
            }
            // Should this for-loop be inside a coroutine scope? It's a long running calculation that will occur every interval
            // maybe it's too much for the main thread

            CoroutineScope(Dispatchers.Default).launch{
                if(!isDistanceClose){
                for(i in fuelsOnMap.indices){

                    // Prompts user to catch fuel while he is close
                    if (distanceFromUserAndMarker(currentLocation, fuelsOnMap[i].coordinates) < MAX_DISTANCE_TO_CATCH_FUEL ){
                        CoroutineScope(Dispatchers.Main).launch {
                            btnCatch.visibility = View.VISIBLE
                            fuelToCatchIndex = i
                            isDistanceClose = true
                        }
                        break

                    }
                }
                } else{
                    Log.d("MapsActivity","Einai konta ${fuelToCatchIndex}!")
                    if(distanceFromUserAndMarker(currentLocation,fuelsOnMap[fuelToCatchIndex].coordinates)> MAX_DISTANCE_TO_CATCH_FUEL){
                        Log.d("MapsActivity","Efuge ${fuelToCatchIndex}")

                        CoroutineScope(Dispatchers.Main).launch {
                            btnCatch.visibility = View.INVISIBLE
                            fuelToCatchIndex = -1
                            isDistanceClose = false
                        }
                    }
                }
            }





        })
        GameService.coordinatesInitialFuel.observe(this,{
            fuelsOnMap=it
            markerListFromFuelList(mMap,it)

        })
    }

    private fun deleteMarkerFromListAndUpdateMap(index:Int,currentLatLng: LatLng) {
        fuelsOnMap.removeAt(index)
        mMap.clear()
        updateUserMarker(currentLatLng)
        markerListFromFuelList(mMap,fuelsOnMap)
    }

    private fun markerListFromFuelList(mMap:GoogleMap, mutableListFuel: MutableList<Fuel>) : MutableList<Marker>{
        val markersList = mutableListOf<Marker>()

        for (fuel in mutableListFuel){
            mMap.addMarker(MarkerOptions().icon(Utils.bitmapDescriptorFromVector(this,R.drawable.ic_gas)).position(fuel.coordinates).title(fuel.litres.toString()))?.let {
                markersList.add(it)
            }
        }
        return markersList
    }



    private fun distanceFromUserAndMarker(location : Location, latLng: LatLng) : Float{
       return location.distanceTo(Location("coords").apply {
            latitude = latLng.latitude
            longitude = latLng.longitude
        })
    }

    private fun updateUserMarker(latLng: LatLng){
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,18f))
        userMarker?.remove()
        userMarker = mMap.addMarker(MarkerOptions().position(latLng).title("Marker in Sydney").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)))
    }
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap


        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)

        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
    }
    private fun sendCommandToService(action:String){
        Intent(this, GameService::class.java).also {
            it.action = action
            this.startService(it)
        }
    }
    private fun requestPermissions(){
        if(Utils.hasPermissions(this)){
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