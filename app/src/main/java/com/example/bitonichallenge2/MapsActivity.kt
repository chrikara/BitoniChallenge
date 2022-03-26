package com.example.bitonichallenge2

import android.content.Intent
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
import com.example.bitonichallenge2.model.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions

class MapsActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    lateinit var mapFragment: SupportMapFragment

    var mMap: GoogleMap? = null

    private var isGameOnGoingMap : Boolean = false
    private var coordinatesFuelMap : MutableList<Fuel> = mutableListOf()
    private var coordinatesUserMap : LatLng = LatLng(0.0,0.0)

    private var userMarker : Marker? = null

    private var fuelToCatchIndex = -1
    private var isDistanceClose = false
    private var isFirstgame = true

    private var menu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        requestPermissions()
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync {
            mMap = it
            updateFuelMapLocation(coordinatesFuelMap)
            updateUserLocation(coordinatesUserMap)
            cameraToUser()
            GameService.isPaused.value?.let { isPaused ->
                alphaMapWhenPaused(isPaused)
            }

        }

        subscribeToObservers()
        spinnerAddStyles()

        btnStartGame.setOnClickListener {
            sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
        }
        btnPauseGame.setOnClickListener {
            sendCommandToService(ACTION_PAUSE_SERVICE)
        }

        btnCatch.setOnClickListener {
            Toast.makeText(this@MapsActivity, "Caught ${coordinatesFuelMap[fuelToCatchIndex].litres} litres!", Toast.LENGTH_SHORT).show()

            btnCatch.isEnabled = false
            animateCatchButton(false)

            deleteMarkerFromListAndUpdateMap(fuelToCatchIndex, coordinatesUserMap)
            isDistanceClose = false
        }


        fabGoToUser.setOnClickListener {
            cameraToUser()
        }
    }

    private fun cameraToUser(){
        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinatesUserMap, ZOOM_CAMERA))

    }

    private fun deleteMarkerFromListAndUpdateMap(index:Int,currentLatLng: LatLng) {
        coordinatesFuelMap.removeAt(index)
        mMap?.clear()
        updateUserLocation(currentLatLng)
        updateFuelMapLocation(coordinatesFuelMap)
    }


    private fun subscribeToObservers(){
        GameService.isGameOngoing.observe(this,{
            isGameOnGoingMap = it
            updateUi(it)
        })

        GameService.coordinatesUser.observe(this,{
            coordinatesUserMap = it
            if(!isGameOnGoingMap) return@observe

            updateUserLocation(it)
            updateProgressBarAndMap()
            userAndFuelDistance(it)

        })

        GameService.coordinatesFuel.observe(this,{
            coordinatesFuelMap = it
            updateFuelMapLocation(coordinatesFuelMap)
        })
    }
    private fun updateUi(isGamePlaying : Boolean){

        if(isGamePlaying)                                           updateUiScenario(PLAYER_PLAYING)
        else if(!isGamePlaying && GameService.isPaused.value==true) updateUiScenario(PLAYER_PAUSED)
        else                                                        updateUiScenario(PLAYER_STOPPED)
    }
    private fun updateUiScenario(scenario : String){
        when(scenario){
            PLAYER_PLAYING ->{
                btnStartGame.visibility = View.GONE
                btnPauseGame.visibility = View.VISIBLE
                btnCatch.visibility = View.VISIBLE
                menu?.getItem(0)?.isVisible = true
                alphaMapWhenPaused(false)
                fabGoToUser.visibility = View.VISIBLE
            }
            PLAYER_PAUSED ->{
                btnStartGame.text = "Resume"
                btnStartGame.visibility = View.VISIBLE
                btnPauseGame.visibility = View.GONE
                btnCatch.visibility = View.GONE
                alphaMapWhenPaused(true)
            }
            PLAYER_STOPPED ->{
                btnStartGame.text = "Start"
                btnStartGame.visibility = View.VISIBLE
                btnPauseGame.visibility = View.GONE
                btnCatch.visibility = View.GONE
                btnCatch.isEnabled = false
                fabGoToUser.visibility = View.GONE
                isFirstgame = true
                isDistanceClose = false
                fuelToCatchIndex = -1
                menu?.getItem(0)?.isVisible = false
                mMap?.clear()
                updateUserLocation(coordinatesUserMap)
            }
        }
    }
    private fun updateProgressBarAndMap(){
        // If-block is executed until all random fuels are finished generating on the map
        if(isGameOnGoingMap && GameService.isFirstGame)
        {
            progressBar.visibility = View.VISIBLE
            mapFragment.view?.alpha = 0.1f
        }

        // Else-block is executed ONCE when GameService.isFirstGame gets false, that's when all random fuels have been generated
        else if(isFirstgame && !GameService.isFirstGame ) {
            cameraToUser()
            progressBar.visibility = View.GONE
            alphaMapWhenPaused(false)
            isFirstgame = false
        }
    }
    private fun updateUserLocation(userLatLng: LatLng){
        userMarker?.remove()
        userMarker = mMap?.addMarker(MarkerOptions()
                .position(userLatLng)
                .title("It's me")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)))
    }

    private fun userAndFuelDistance(latLng: LatLng){
        CoroutineScope(Dispatchers.Default).launch{
            if(!isDistanceClose){
                for(i in coordinatesFuelMap.indices){
                    if (distanceFromUserAndMarker(locationConverter(latLng), coordinatesFuelMap[i].coords) < DISTANCE_FUEL_IS_CATCHABLE ){
                        CoroutineScope(Dispatchers.Main).launch {
                            btnCatch.isEnabled = true
                            animateCatchButton(true)
                            fuelToCatchIndex = i
                            isDistanceClose = true
                        }
                        break
                    }
                }
                return@launch
            }
            if(distanceFromUserAndMarker(locationConverter(latLng),coordinatesFuelMap[fuelToCatchIndex].coords)> DISTANCE_FUEL_IS_CATCHABLE){
                CoroutineScope(Dispatchers.Main).launch {
                    btnCatch.isEnabled = false
                    animateCatchButton(false)
                    fuelToCatchIndex = -1
                    isDistanceClose = false
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
            mMap?.addMarker(MarkerOptions()
                    .position(fuel.coords)
                    .title(fuel.litres.toString())
                    .icon(utils.bitmapDescriptorFromVector(this,fuel.drawable,fuel.dimensions)))
            Log.d("MapsActivity","${fuel.coords}")
        }
    }
    private fun animateCatchButton(isCatchable: Boolean) {
            // Sometimes, app crashes during animation
        CoroutineScope(Dispatchers.Main).launch {
            if (isCatchable) {
                btnCatch.apply {
                    btnCatch.isEnabled = true
                    strokeWidth = 7
                    animate().apply {
                        rotationYBy(ROTATION_BUTTON)
                        duration = ANIMATION_DURATION
                    }
                }
                return@launch
            }
            btnCatch.isEnabled = false
            btnCatch.strokeWidth = 0
        }
        }



    private fun alphaMapWhenPaused(paused: Boolean){
        if(paused){
            mapFragment.view?.alpha = 0.6f
            mMap?.uiSettings?.setAllGesturesEnabled(false)
        }else{
            mapFragment.view?.alpha = 1f
            mMap?.uiSettings?.setAllGesturesEnabled(true)
        }

    }

    private fun spinnerAddStyles(){
        spMapStyles.apply {
            onItemSelectedListener = object : AdapterView.OnItemClickListener, AdapterView.OnItemSelectedListener {
                override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {}
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    when (position){
                        0 -> { mMap?.setMapStyle(null) }
                        1 -> { mMap?.setMapStyle(MapStyleOptions.loadRawResourceStyle(this@MapsActivity,R.raw.map_style_midnight))}
                        2 -> { mMap?.setMapStyle(MapStyleOptions.loadRawResourceStyle(this@MapsActivity,R.raw.map_style_midnight_brand))}
                        3 -> { mMap?.setMapStyle(MapStyleOptions.loadRawResourceStyle(this@MapsActivity,R.raw.map_style_muted_blue))}
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    TODO("Not yet implemented")
                }

            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_maps_activity,menu)
        this.menu = menu
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        // Make visible the menu if the game is either being played or paused
        GameService.isGameOngoing.value?.let { isPlaying ->
            if(isPlaying || GameService.isPaused.value!!){
                this.menu?.getItem(0)?.isVisible = true
            }
        }
            return super.onPrepareOptionsMenu(menu)
    }
    private fun showCancelGameDialog() {
        val dialog = MaterialAlertDialogBuilder(this,R.style.AlertDialogTheme)
                .setTitle("Cancel game?")
                .setMessage("Are you sure to cancel current game?")
                .setIcon(R.drawable.ic_delete)
                .setPositiveButton("Absolutely") { _, _ ->
                    stopGame()
                }
                .setNegativeButton("No") { dialogInterface, _ ->
                    dialogInterface.cancel()
                }
                .create()
        dialog.show()
    }
    private fun stopGame(){
        sendCommandToService(ACTION_STOP_SERVICE)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId){
            R.id.miCancelGame->{showCancelGameDialog()}
        }
        return super.onOptionsItemSelected(item)
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