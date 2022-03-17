package com.example.bitonichallenge2

import android.content.Intent
import android.location.Location
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.bitonichallenge2.model.ACTION_START_OR_RESUME_SERVICE
import com.example.bitonichallenge2.model.ACTION_STOP_SERVICE
import com.example.bitonichallenge2.model.REQUEST_CODE_PERMISSIONS
import com.example.bitonichallenge2.model.Utils

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_maps.*
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, EasyPermissions.PermissionCallbacks {

    private lateinit var mMap: GoogleMap
    private lateinit var fuelCoordinates: MutableList<LatLng>
    var isGameOngoing : Boolean = false
    var a : String? = null

    var userMarker : Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        requestPermissions()
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fuelCoordinates = mutableListOf()

        btnSendCommand.setOnClickListener {
            sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
            userMarker = mMap.addMarker(MarkerOptions().position(LatLng(0.0,0.0)).title("Marker in Sydney").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)))

        }


        btnStopGame.setOnClickListener{
            sendCommandToService(ACTION_STOP_SERVICE)

        }
        subscribeToObservers()

    }


    private fun subscribeToObservers(){
        GameService.isGameOngoing.observe(this,{
        })
        GameService.coordinatesUser.observe(this,{
            updateUserMarker(it)
            val location = Location("user").apply {
                latitude = it.latitude
                longitude = it.longitude
            }
            for(i in fuelCoordinates.indices){
                if (distanceFromUserAndMarker(location,fuelCoordinates[i])<20f){
                    println(fuelCoordinates[i])
                }
            }
        })
        GameService.coordinatesFuel.observe(this,{
            fuelCoordinates=it
            markerListFromLatLngList(mMap,it)

        })
    }
    private fun markerListFromLatLngList(mMap:GoogleMap,mutableListLatLng: MutableList<LatLng>) : MutableList<Marker>{
        val markersList = mutableListOf<Marker>()

        for (latlng in mutableListLatLng){
            mMap.addMarker(MarkerOptions().position(latlng).title("Marker in Sydney"))?.let {
                markersList.add(it)
            }
        }
        return markersList
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap


        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)

        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
    }

    private fun distanceFromUserAndMarker(location1 : Location, latLng: LatLng) : Float{
       return location1.distanceTo(Location("coor").apply {
            latitude = latLng.latitude
            longitude = latLng.longitude
        })
    }

    private fun updateUserMarker(latLng: LatLng){
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,18f))
        userMarker?.remove()
        userMarker = mMap.addMarker(MarkerOptions().position(latLng).title("Marker in Sydney").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)))
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