package com.example.bitonichallenge2

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.example.bitonichallenge2.model.*
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng

class GameService: LifecycleService() {
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    var serviceKilled = false
    private var userLastLocation : Location = Location("user")

    companion object{
        var isGameOngoing = MutableLiveData<Boolean>()
        var isPaused = MutableLiveData<Boolean>()
        var coordinatesFuel = MutableLiveData<MutableList<Fuel>>()
        var coordinatesUser = MutableLiveData<LatLng>()

        var isFirstGame = true

    }

    override fun onCreate() {
        super.onCreate()
        postInitialValues()
        fusedLocationProviderClient = FusedLocationProviderClient(this)

        isGameOngoing.observe(this,{
            updateLocation(it)
        })
    }


    // See https://www.youtube.com/watch?v=JpVBPKf2mIU&list=PLQkwcJG4YTCQ6emtoqSZS2FVwZR9FT3BV
    // why we need Foreground Service
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("GameService","${isFirstGame}")

        intent?.let {

            when(it.action){
                ACTION_START_OR_RESUME_SERVICE -> {
                    if(isFirstGame){
                        startForegroundService()
                    }else{
                        resumeService()
                    }
                }
                ACTION_PAUSE_SERVICE -> {
                    pauseService()
                }

                ACTION_STOP_SERVICE -> {
                    killService()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun postInitialValues(){
        isPaused.postValue(false)
        isGameOngoing.postValue(false)
        coordinatesFuel.postValue(mutableListOf())
        coordinatesUser.postValue(LatLng(0.0,0.0))
    }

    private fun resumeService(){
        isPaused.postValue(false)
        isGameOngoing.postValue(true)
    }

    private fun pauseService(){
        isPaused.postValue(true)
        isGameOngoing.postValue(false)
    }
    private fun killService(){
        isFirstGame=true
        isPaused.postValue(false)
        isGameOngoing.postValue(false)
        coordinatesFuel.postValue(mutableListOf())
        coordinatesUser.postValue(LatLng(userLastLocation.latitude,userLastLocation.longitude))
        stopSelf()
        stopForeground(true)
    }
    @SuppressLint("MissingPermission")
    private fun updateLocation(isGame:Boolean){
        if(isGame){
            if(utils.hasPermissions(this)){
                val request = LocationRequest.create().apply {
                    interval = MY_INTERVAL
                    fastestInterval = MY_MAXIMUM_INTERVAL
                    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                }
                fusedLocationProviderClient.requestLocationUpdates(
                        request,
                        lastLocation,
                        Looper.getMainLooper()
                )
            }
        }else{
            fusedLocationProviderClient.removeLocationUpdates(lastLocation)
        }
    }
    val lastLocation = object : LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)

            userLastLocation = locationResult.lastLocation
            if(isGameOngoing.value!!){
                coordinatesUser.postValue(LatLng(locationResult.lastLocation.latitude,locationResult.lastLocation.longitude))
            }
            if(isFirstGame && coordinatesUser.value!=null){
                coordinatesFuel.postValue((utils.generateFuelListWithin120mRad(locationResult.lastLocation)))
                isFirstGame=false
            }
        }

    }

    private fun startForegroundService(){
        isGameOngoing.postValue(true)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            createNotificationChannel(notificationManager)
        }

        val notificationBuilder= NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(false) // User can't cancel notification
            .setOngoing(true) // User can't swipe away notification
            .setSmallIcon(R.drawable.ic_baseline_directions_run_24)
            .setContentTitle("Game ongoing")
            .setContentText("00:00:00")
            .setContentIntent(getMapsActivityPendingIntent())

        startForeground(NOTIFICATION_ID, notificationBuilder.build())

    }

    private fun getMapsActivityPendingIntent() = PendingIntent.getActivity(
        this,
        0,
        Intent(this,MapsActivity::class.java).also {
            it.action = ACTION_SHOW_MAPS_ACTIVITY
        },
        PendingIntent.FLAG_UPDATE_CURRENT // When we launch pending intent and it already exists
        // this will update instead of recreating it
    )
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager){
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )

        notificationManager.createNotificationChannel(channel)
    }
}