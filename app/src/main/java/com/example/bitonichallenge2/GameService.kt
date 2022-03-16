package com.example.bitonichallenge2

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.example.bitonichallenge2.model.*
import com.google.android.gms.maps.model.LatLng

class GameService: LifecycleService() {
    var isFirstGame = true
    companion object{
        var isGameOngoing = MutableLiveData<Boolean>()
        var coordinates = MutableLiveData<List<LatLng>>()


    }

    private fun postInitialValues(){

        isGameOngoing.postValue(false)
        coordinates.postValue(mutableListOf())

    }

    // See https://www.youtube.com/watch?v=JpVBPKf2mIU&list=PLQkwcJG4YTCQ6emtoqSZS2FVwZR9FT3BV
    // why we need Foreground Service
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent?.let {

            when(it.action){
                ACTION_START_OR_RESUME_SERVICE -> {
                    if(isFirstGame){
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

    private fun startForegroundService(){
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