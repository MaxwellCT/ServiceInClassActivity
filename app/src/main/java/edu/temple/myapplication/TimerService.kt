package edu.temple.myapplication

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.util.Log

class TimerService : Service() {

    private var isRunning = false
    private var paused = false
    private var currentTimerValue = 100 // Default timer value
    private var timerHandler: Handler? = null
    private lateinit var sharedPreferences: SharedPreferences

    inner class TimerBinder : Binder() {
        fun getService(): TimerService {
            return this@TimerService
        }
    }

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = getSharedPreferences("TimerPrefs", MODE_PRIVATE)
        restoreState() // Restore the saved state when the service is created
    }

    override fun onBind(intent: Intent): IBinder {
        return TimerBinder()
    }

    fun setHandler(handler: Handler) {
        timerHandler = handler
    }

    fun start(startValue: Int) {
        if (paused) {
            paused = false
            isRunning = true
        } else {
            currentTimerValue = startValue
            TimerThread(currentTimerValue).start()
        }
        saveState(currentTimerValue, false) // Save the state when starting
    }
    fun stop() {
        isRunning = false
        paused = false
        saveState(100, false) // Reset to default value and save state
    }

    fun pause() {
        paused = true
        isRunning = false
        saveState(currentTimerValue, true) // Save the current state when paused
    }

    private fun saveState(timerValue: Int, isPaused: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putInt("timer_value", timerValue)
        editor.putBoolean("is_paused", isPaused)
        editor.apply()
        Log.d("TimerService", "State saved: timer_value=$timerValue, is_paused=$isPaused")
    }

    private fun restoreState() {
        currentTimerValue = sharedPreferences.getInt("timer_value", 100) // Default to 100
        paused = sharedPreferences.getBoolean("is_paused", false)
        Log.d("TimerService", "State restored: timer_value=$currentTimerValue, is_paused=$paused")
    }

    inner class TimerThread(private val startValue: Int) : Thread() {
        override fun run() {
            isRunning = true
            try {
                for (i in startValue downTo 1) {
                    if (!isRunning) break
                    currentTimerValue = i
                    timerHandler?.sendEmptyMessage(i)
                    while (paused) {
                        sleep(100) // Wait while paused
                    }
                    sleep(1000)
                }
                isRunning = false
            } catch (e: InterruptedException) {
                Log.d("TimerService", "Timer interrupted")
            }
        }
    }
}