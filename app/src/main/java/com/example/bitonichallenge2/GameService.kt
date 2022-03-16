package com.example.bitonichallenge2

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
    var isFirstGame = true
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    companion object{
        var isGameOngoing = MutableLiveData<Boolean>()
        var coordinates = MutableLiveData<LatLng>()
    }

    override fun onCreate() {
        super.onCreate()
        postInitialValues()
        fusedLocationProviderClient = FusedLocationProviderClient(this)

        isGameOngoing.observe(this,{
            updateLocation(it)
        })
    }
    private fun postInitialValues(){
        isGameOngoing.postValue(false)
        coordinates.postValue(LatLng(0.0,0.0))
    }

    // See https://www.youtube.com/watch?v=JpVBPKf2mIU&list=PLQkwcJG4YTCQ6emtoqSZS2FVwZR9FT3BV
    // why we need Foreground Service
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("GameService","${isFirstGame}")

        intent?.let {

            when(it.action){
                ACTION_START_OR_RESUME_SERVICE -> {
                    if(isFirstGame){
                        Log.d("GameService","Starrr Service")

                        startForegroundService()
                    }else{
                        Log.d("GameService","Resuming Service")
                    }
                }
                ACTION_PAUSE_SERVICE -> {
                    Log.d("GameService","Paused")}
                ACTION_STOP_SERVICE -> {
                    Log.d("GameService","Stopped")}

                else -> Log.d("GameService","Nothing")
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }
    @SuppressLint("MissingPermission")
    private fun updateLocation(isGame:Boolean){
        if(isGame){
            if(Utils.hasPermissions(this)){
                val request = LocationRequest.create().apply {
                    interval = 5000L
                    fastestInterval = 2000L
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
            Log.d("GameService","Lat:${locationResult.lastLocation.latitude} Lon:${locationResult.lastLocation.longitude}")
            if(isGameOngoing.value!!){
                coordinates.postValue(LatLng(locationResult.lastLocation.latitude,locationResult.lastLocation.longitude))
            }
        }

        override fun onLocationAvailability(p0: LocationAvailability) {
            super.onLocationAvailability(p0)
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