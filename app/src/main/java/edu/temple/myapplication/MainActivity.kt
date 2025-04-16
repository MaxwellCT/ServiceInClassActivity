package edu.temple.myapplication

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {

    private var timerService: TimerService? = null
    private var isBound = false
    private var currentTimerValue = 100 // Default timer value

    private class TimerHandler(activity: MainActivity) : Handler() {
        private val activityReference = WeakReference(activity)

        override fun handleMessage(msg: Message) {
            val activity = activityReference.get()
            activity?.let {
                val textView = it.findViewById<TextView>(R.id.textView)
                textView.text = msg.what.toString()
            }
        }
    }

    private val timerHandler = TimerHandler(this)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TimerService.TimerBinder
            timerService = binder.getService()
            timerService?.setHandler(timerHandler)
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            timerService = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val serviceIntent = Intent(this, TimerService::class.java)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        // Restore the saved state
        val sharedPreferences = getSharedPreferences("TimerPrefs", Context.MODE_PRIVATE)
        val savedTimerValue = sharedPreferences.getInt("timer_value", 100)
        val isPaused = sharedPreferences.getBoolean("is_paused", false)

        if (isPaused) {
            currentTimerValue = savedTimerValue
            findViewById<TextView>(R.id.textView).text = currentTimerValue.toString()
        } else {
            currentTimerValue = 100 // Reset to default if not paused
        }

        findViewById<Button>(R.id.startButton).setOnClickListener {
            if (isBound) {
                timerService?.start(currentTimerValue)
            } else {
                Toast.makeText(this, "Service not bound", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.pauseButton).setOnClickListener {
            if (isBound) {
                timerService?.pause()
            } else {
                Toast.makeText(this, "Service not bound", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.stopButton).setOnClickListener {
            if (isBound) {
                timerService?.stop()
                currentTimerValue = 100 // Reset to default
                findViewById<TextView>(R.id.textView).text = currentTimerValue.toString()
            } else {
                Toast.makeText(this, "Service not bound", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }
}