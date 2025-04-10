package edu.temple.myapplication

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

class TimerService : Service() {

    private var isRunning = false
    private var timerHandler: Handler? = null
    private lateinit var timerThread: TimerThread
    private var paused = false
    private val stopRequested = AtomicBoolean(false)

    inner class TimerBinder : Binder() {
        // Check if Timer is already running
        val isRunning: Boolean
            get() = this@TimerService.isRunning

        // Check if Timer is paused
        val paused: Boolean
            get() = this@TimerService.paused

        // Start a new timer
        fun start(startValue: Int) {
            if (isRunning && !paused) return

            if (isRunning && paused) {
                resume()
                return
            }

            this@TimerService.start(startValue)
        }

        // Resume a paused timer
        fun resume() {
            this@TimerService.resume()
        }

        // Receive updates from Service
        fun setHandler(handler: Handler) {
            timerHandler = handler
        }

        // Stop a currently running timer
        fun stop() {
            this@TimerService.stop()
        }

        // Pause a running timer
        fun pause() {
            this@TimerService.pause()
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("TimerService status", "Created")
    }

    override fun onBind(intent: Intent): IBinder {
        return TimerBinder()
    }

    fun start(startValue: Int) {
        stopRequested.set(false)
        paused = false
        timerThread = TimerThread(startValue)
        timerThread.start()
    }

    fun pause() {
        paused = true
        isRunning = false
    }

    fun resume() {
        paused = false
        isRunning = true
        synchronized(timerThread) {
            (timerThread as Object).notify()
        }
    }

    fun stop() {
        if (::timerThread.isInitialized && timerThread.isAlive) {
            stopRequested.set(true)
            resume() // In case timer is paused
            timerThread.interrupt()
        }
    }

    inner class TimerThread(private val startValue: Int) : Thread() {
        private var currentValue = startValue

        override fun run() {
            isRunning = true
            try {
                while (currentValue > 0 && !stopRequested.get()) {
                    Log.d("Countdown", currentValue.toString())
                    timerHandler?.sendEmptyMessage(currentValue)

                    if (paused) {
                        isRunning = false
                        synchronized(this) {
                            try {
                                (this as Object).wait()
                            } catch (e: InterruptedException) {
                                if (stopRequested.get()) {
                                    return
                                } else {
                                    Log.d("Timer interrupted", e.toString())
                                }
                            }
                        }
                        isRunning = true
                    }

                    sleep(1000)
                    currentValue--
                }

                if (!stopRequested.get()) {
                    // Timer completed naturally
                    timerHandler?.sendEmptyMessage(0)
                }

            } catch (e: InterruptedException) {
                Log.d("Timer interrupted", e.toString())
            } finally {
                isRunning = false
                paused = false
            }
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        stop()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        stop()
        super.onDestroy()
        Log.d("TimerService status", "Destroyed")
    }
}