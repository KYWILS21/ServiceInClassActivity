package edu.temple.myapplication

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Button
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    var timerBinder: TimerService.TimerBinder? = null

    val handler = Handler(Looper.getMainLooper()) { msg ->
        findViewById<TextView>(R.id.textView).text = msg.what.toString()
        true
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindService(
            Intent(this, TimerService::class.java),
            object: ServiceConnection{
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    (service as TimerService.TimerBinder).setHandler(handler)
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    timerBinder = null
                }
            },
            BIND_AUTO_CREATE
        )

        findViewById<Button>(R.id.startButton).setOnClickListener {
            timerBinder?.run {
                if(!isRunning){
                    start(10)
                } else {
                    pause()
                }
            }
        }
        
        findViewById<Button>(R.id.stopButton).setOnClickListener {
            timerBinder?.stop()
        }
    }
}