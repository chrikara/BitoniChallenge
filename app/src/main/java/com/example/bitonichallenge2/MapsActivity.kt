package com.example.bitonichallenge2

import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
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
import com.example.bitonichallenge2.model.Utils.hasPermissions
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
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

    lateinit var currentLocation : Location
    var fuelToCatchIndex : Int = -1
    var userMarker : Marker? = null

    companion object{
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


        subscribeToObservers()
        fuelsOnMap = mutableListOf()

        btnSendCommand.setOnClickListener {
            sendCommandToService(ACTION_START_OR_RESUME_SERVICE)

            menu!!.getItem(0)!!.isVisible = true
            it.visibility = View.GONE
            spMapStyles.visibility = View.GONE
            btnPauseGame.visibility = View.VISIBLE
            btnCatch.visibility = View.VISIBLE

            isMapPaused(false)

            // This is to fire a GameService-if-statement that posts initial fuel coordinates and initial user location (ctrl+left click isGameJustStarted)
            if(btnSendCommand.text == "Start") {
                GameService.isGameJustStarted.postValue(true)
            }
        }


        spMapStyles.apply {
            onItemSelectedListener = object : AdapterView.OnItemClickListener, AdapterView.OnItemSelectedListener {
                override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {}
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    when (position){
                        0 -> { mMap.setMapStyle(null) }
                        1 -> {setCustomMapStyle(R.raw.map_style_midnight)}
                        2 -> {setCustomMapStyle(R.raw.map_style_midnight_brand)}
                        3 -> {setCustomMapStyle(R.raw.map_style_muted_blue)}
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    TODO("Not yet implemented")
                }

            }
        }

        fabGoToUser.setOnClickListener{
            try{
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(GameService.coordinatesUser.value!!, ZOOM_CAMERA))
            }catch (e:Exception){}
        }


        btnPauseGame.setOnClickListener{
            isMapPaused(true)

            sendCommandToService(ACTION_PAUSE_SERVICE)
            it.visibility = View.GONE
            btnSendCommand.visibility = View.VISIBLE
            btnCatch.visibility = View.INVISIBLE
        }

        btnCatch.setOnClickListener{
            try{
                Toast.makeText(this,"Caught ${fuelsOnMap[fuelToCatchIndex].litres} litres!", Toast.LENGTH_SHORT).show()
                deleteMarkerFromListAndUpdateMap(fuelToCatchIndex, GameService.coordinatesUser.value!!)

                animateCatchButton(false)
                isDistanceClose = false
            }catch (e:Exception){e.printStackTrace()}
        }


    }


    private fun subscribeToObservers(){
        GameService.isGameJustStarted.observe(this, {
            if(it){
                btnSendCommand.text = "Resume"
                btnCatch.isEnabled = false
                GameService.isProgressBarVisible.postValue(true)
            }else{
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(GameService.coordinatesUser.value!!, ZOOM_CAMERA))
                GameService.isProgressBarVisible.postValue(false)
            }
        })
        GameService.isProgressBarVisible.observe(this,{
            setUpProgressBarVisibility(it)
        })
        GameService.coordinatesUser.observe(this,{
            updateUserMarker(it)
            currentLocation = Location("user").apply {
                latitude = it.latitude
                longitude = it.longitude
            }

            // Should this for-loop be inside a coroutine scope? It's a long running calculation that will occur every interval
            // maybe it's too much for the main thread

            CoroutineScope(Dispatchers.Default).launch{
                if(!isDistanceClose){
                for(i in fuelsOnMap.indices){

                    // Prompts user to catch fuel if he is close to a fuel marker
                    if (distanceFromUserAndMarker(currentLocation, fuelsOnMap[i].coordinates) < MAX_DISTANCE_TO_CATCH_FUEL ){
                        CoroutineScope(Dispatchers.Main).launch {
                            animateCatchButton(true)
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
                            animateCatchButton(false)
                            fuelToCatchIndex = -1
                            isDistanceClose = false
                        }
                    }


                }
            }
        })
        GameService.coordinatesInitialFuel.observe(this,{
            Log.d("MapsActivity", "$it")
            fuelsOnMap=it
            addFuelMarkersToMap(mMap,it)
        })
    }
   // Adds each fuel as a marker to the map
    private fun addFuelMarkersToMap(mMap:GoogleMap, mutableListFuel: MutableList<Fuel>){

        for (fuel in mutableListFuel){
            mMap.addMarker(MarkerOptions()
                    .icon(Utils.bitmapDescriptorFromVector(this,gasIconFromSpinner(),fuel.dimensions))
                    .position(fuel.coordinates)
                    .title(fuel.litres.toString()))
        }
    }

    // Clears whole map, updates map with user marker, all fuels except from the removed fuel
    private fun deleteMarkerFromListAndUpdateMap(index:Int,currentLatLng: LatLng) {
        fuelsOnMap.removeAt(index)
        mMap.clear()
        updateUserMarker(currentLatLng)
        addFuelMarkersToMap(mMap,fuelsOnMap)
    }

    // So that every last location is deleted
    private fun updateUserMarker(userLatLng: LatLng){
        userMarker?.remove()
        userMarker = mMap.addMarker(MarkerOptions().position(userLatLng).title("It's me").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)))
    }


    private fun distanceFromUserAndMarker(currentLocation : Location, fuelLocation: LatLng) : Float{
       return currentLocation.distanceTo(Location("coords").apply {
            latitude = fuelLocation.latitude
            longitude = fuelLocation.longitude
        })
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(XANTHI_CENTER, ZOOM_CAMERA))
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

    private fun animateCatchButton(isCatchable: Boolean) {
        try{
            // Sometimes, app crashes during animation
                if (isCatchable) {
                    btnCatch.apply {
                        strokeWidth = 7
                        animate().apply {
                            rotationYBy(360f)
                            duration = ANIMATION_DURATION
                        }
                        isEnabled = true
                    }
                }
                else {
                    btnCatch.apply {
                        strokeWidth = 0
                        isEnabled = false
                    }
                }
        }catch (e:Exception){
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }

    }
    private fun requestPermissions(){
        if(hasPermissions(this)){
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
                    animateCatchButton(false)
                    btnPauseGame.visibility = View.GONE
                    menu?.getItem(0)?.isVisible = false


                    isMapPaused(false)



                    btnSendCommand.text = "Start"

                    isDistanceClose = false
                    fuelsOnMap.clear()
                    mMap.clear()

                }
                .setNegativeButton("No") { dialogInterface: DialogInterface, i: Int -> dialogInterface.cancel()}
                .create()
        dialog.show()
    }
    private fun setUpProgressBarVisibility(isGameJustStarted : Boolean){
        if(isGameJustStarted){
            progressBar.visibility = View.VISIBLE
            mapFragment.alpha(0.1f)
        }else{
            progressBar.visibility = View.GONE
            mapFragment.alpha(1f)
        }

    }

    private fun SupportMapFragment.alpha(alpha : Float){
        this.view?.alpha = alpha
    }


    // Map freezes if user pauses the game
    private fun isMapPaused(paused: Boolean){

        if(paused){
            mapFragment.alpha(0.6f)
            mMap.uiSettings.setAllGesturesEnabled(false)
        }else{
            mapFragment.alpha(1f)
            mMap.uiSettings.setAllGesturesEnabled(true)
        }

    }

    private fun gasIconFromSpinner() : Int{
        return when(spMapStyles.selectedItemPosition){
            0 -> R.drawable.ic_gas_black
            1 -> R.drawable.ic_gas_white
            2 -> R.drawable.ic_gas_white
            else -> R.drawable.ic_gas_black
        }
    }

}