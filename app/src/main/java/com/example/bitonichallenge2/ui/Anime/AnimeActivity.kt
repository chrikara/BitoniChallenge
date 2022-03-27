package com.example.bitonichallenge2.ui.Anime

import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.example.bitonichallenge2.R
import com.example.bitonichallenge2.model.SHARED_START
import com.example.bitonichallenge2.model.SHARED_START_BOOLEAN
import com.example.bitonichallenge2.ui.Maps.MapsActivity
import kotlinx.android.synthetic.main.activity_anime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AnimeActivity : AppCompatActivity() {
    lateinit var sharedPrefs : SharedPreferences
    lateinit var viewModel : AnimeViewModel
    var hasAlreadyStartedGame : Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_anime)

        sharedPrefs = getSharedPreferences(SHARED_START, MODE_PRIVATE)
        hasAlreadyStartedGame = sharedPrefs.getBoolean(SHARED_START_BOOLEAN,false)
        viewModel = ViewModelProvider(this).get(AnimeViewModel::class.java)


        CoroutineScope(Dispatchers.Main).launch {
            if(!viewModel.hasAnimationEnded)  fuelAnimation()

            showViewsAfterAnimation()
            btnContinue.isEnabled = hasAlreadyStartedGame
        }

        btnStart.setOnClickListener {

            if(hasAlreadyStartedGame){ alertDialogNewGame()}
            else{ startNewGame() }
        }
        btnContinue.setOnClickListener {
            continueGame()
        }

    }


    private suspend fun fuelAnimation(){
        ivBitoniAnime.startAnimation(AnimationUtils.loadAnimation(this, R.anim.splash_in))
        delay(2000L)
        ivBitoniAnime.startAnimation(AnimationUtils.loadAnimation(this, R.anim.splash_out))
        delay(2000L)
        viewModel.hasAnimationEnded=true

    }

    private fun showViewsAfterAnimation(){
        ivAdonis.visibility = View.VISIBLE
        tvAdonis.visibility = View.VISIBLE
        btnStart.visibility = View.VISIBLE
        btnContinue.visibility = View.VISIBLE
    }

    private fun startNewGame(){
        sharedPrefs.edit().putBoolean(SHARED_START_BOOLEAN, true).apply()
        Intent(this, MapsActivity::class.java).also { startActivity(it) }
        finish()
    }

    private fun alertDialogNewGame(){
        AlertDialog.Builder(this)
                .setTitle("Start new game")
                .setMessage("Do you want to delete your last save and start a new game? Το λιγουρεύεσαι?")
                .setNegativeButton("No") { _: DialogInterface, _: Int ->  }
                .setPositiveButton("Yes"){ _: DialogInterface, _: Int -> startNewGame() }
                .create()
                .show()
    }

    private fun continueGame(){
        Intent(this, MapsActivity::class.java).also { startActivity(it) }
        finish()
    }
}
