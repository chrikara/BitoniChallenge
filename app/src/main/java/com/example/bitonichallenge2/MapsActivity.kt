package com.example.bitonichallenge2

import android.content.DialogInterface
import android.content.Intent
import android.location.Location
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.bitonichallenge2.model.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL
import com.google.android.gms.maps.GoogleMap.MAP_TYPE_TERRAIN
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.lang.Exception

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, EasyPermissions.PermissionCallbacks {

    private lateinit var fuelsOnMap: MutableList<Fuel>
    private lateinit var mapFragment: SupportMapFragment
    var isDistanceClose : Boolean = false

    var fuelToCatchIndex : Int = -1
    var userMarker : Marker? = null

    companion object{
        var isGameJustStarted = false
        lateinit var mMap: GoogleMap
    }

    private var menu: Menu? = null

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        requestPermissions()
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)


        fuelsOnMap = mutableListOf()

        btnSendCommand.setOnClickListener {
            menu!!.getItem(0)!!.isVisible = true
            sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
            mapFragment.alpha(1f)

            it.visibility = View.GONE
            spMapStyles.visibility = View.GONE
            btnPauseGame.visibility = View.VISIBLE

            mapFragment.alpha(1f)
            mMap.uiSettings.setAllGesturesEnabled(true)


            if(!isGameJustStarted && btnSendCommand.text == "Start") {
                isGameJustStarted=true
                btnSendCommand.text = "Resume"
            }

        }


        spMapStyles.apply {
            onItemSelectedListener = object : AdapterView.OnItemClickListener, AdapterView.OnItemSelectedListener {
                override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {}
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    when (position){
                        0 -> { mMap.setMapStyle(null)}
                        1 -> {setCustomMapStyle(R.raw.map_style_midnight)}
                        2 -> {setCustomMapStyle(R.raw.map_style_dark)}
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    TODO("Not yet implemented")
                }

            }
        }

        btnGoToUser.setOnClickListener{
            try{
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(GameService.coordinatesUser.value!!,18f))
            }catch (e:Exception){}
        }


        btnPauseGame.setOnClickListener{
            mapFragment.alpha(0.6f)
            mMap.uiSettings.setAllGesturesEnabled(false)

            sendCommandToService(ACTION_PAUSE_SERVICE)
            it.visibility = View.GONE
            btnSendCommand.visibility = View.VISIBLE
            btnCatch.visibility = View.INVISIBLE
        }

        btnCatch.setOnClickListener{
            try{
                Toast.makeText(this,"Caught ${fuelsOnMap[fuelToCatchIndex].litres} litres!", Toast.LENGTH_SHORT).show()
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
                Log.d("MapsFor", "$isDistanceClose")
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
                    // If user walks away from catchable position
                    if(distanceFromUserAndMarker(currentLocation,fuelsOnMap[fuelToCatchIndex].coordinates)> MAX_DISTANCE_TO_CATCH_FUEL){

                        CoroutineScope(Dispatchers.Main).launch {
                            btnCatch.visibility = View.INVISIBLE
                            fuelToCatchIndex = -1
                            isDistanceClose = false
                        }
                        // If user just resumed game and is in a catchable position
                    }else if(fuelToCatchIndex!=-1){
                        CoroutineScope(Dispatchers.Main).launch{btnCatch.visibility = View.VISIBLE}
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
        userMarker?.remove()
        userMarker = mMap.addMarker(MarkerOptions().position(latLng).title("It's me").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)))
    }
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(XANTHI_KENTRO,15f))
    }

    private fun setCustomMapStyle(mapStyle:Int) {
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this,mapStyle))
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_maps_activity,menu)
        this.menu = menu
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId){
            R.id.miCancelGame->{showCancelGameDialog()}
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showCancelGameDialog(){
        val dialog = MaterialAlertDialogBuilder(this)
                .setTitle("Cancel Game")
                .setMessage("Do you want to cancel this game?")
                .setPositiveButton("Yes") { _: DialogInterface, i: Int ->
                    sendCommandToService(ACTION_STOP_SERVICE)
                    btnSendCommand.visibility = View.VISIBLE
                    spMapStyles.visibility = View.VISIBLE
                    btnCatch.visibility = View.INVISIBLE
                    btnPauseGame.visibility = View.GONE
                    menu?.getItem(0)?.isVisible = false

                    mapFragment.alpha(1f)
                    mMap.uiSettings.setAllGesturesEnabled(true)



                    btnSendCommand.text = "Start"

                    isDistanceClose = false
                    fuelsOnMap.clear()
                    mMap.clear()
                }
                .setNegativeButton("No") { dialogInterface: DialogInterface, i: Int -> dialogInterface.cancel()}
                .create()
        dialog.show()
    }

    private fun SupportMapFragment.alpha(alpha : Float){
        this.view?.alpha = alpha
    }
}