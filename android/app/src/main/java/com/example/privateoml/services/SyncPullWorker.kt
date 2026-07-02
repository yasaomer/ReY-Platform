package com.example.privateoml.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.telephony.SmsManager
import android.util.Log
import com.example.privateoml.data.DatabaseHelper
import com.example.privateoml.utils.NetworkUtils
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject

class SyncPullWorker : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private lateinit var dbHelper: DatabaseHelper
    private var isRunning = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        dbHelper = DatabaseHelper(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            isRunning = true
            startSyncLoop()
        }
        return START_STICKY
    }

    private fun startSyncLoop() {
        serviceScope.launch {
            while (isActive) {
                try {
                    val serverUrl = dbHelper.getConfig("server_url", "https://rey-backend.yasaomer123.workers.dev/api/v1")
                    val token = dbHelper.getConfig("session_token", "")

                    if (token.isNotEmpty()) {
                        // 1. Pull tasks
                        val responseRaw = NetworkUtils.httpGet("$serverUrl/sync/pull", token)
                        val res = JSONObject(responseRaw)
                        if (res.getBoolean("success")) {
                            val tasks = res.getJSONArray("data")
                            for (i in 0 until tasks.length()) {
                                val task = tasks.getJSONObject(i)
                                processTask(task, serverUrl, token)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SyncPullWorker", "Sync polling failed: ${e.message}")
                }
                delay(10000) // Poll every 10 seconds
            }
        }
    }

    private suspend fun processTask(task: JSONObject, serverUrl: String, token: String) {
        val taskId = task.getString("task_id")
        val taskType = task.getString("task_type")
        val errorInfo = task.optString("error_info", "")

        try {
            when (taskType) {
                "send_sms_reset" -> {
                    // Extract payload from error_info / payload metadata
                    val payload = JSONObject(errorInfo)
                    val phoneNumber = payload.getString("phoneNumber")
                    val message = payload.getString("message")

                    // Dispatch SMS using SIM (Section 190)
                    val smsManager = getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
                    smsManager.sendTextMessage(phoneNumber, null, message, null, null)

                    // Complete task
                    reportTaskCompletion(taskId, "completed", "", serverUrl, token)
                }
                "refresh_location" -> {
                    // Query GPS coordinates (Section 57)
                    val location = getLastKnownLocation()
                    if (location != null) {
                        // Post coordinates to update endpoint
                        val payload = JSONObject().apply {
                            put("latitude", location.latitude)
                            put("longitude", location.longitude)
                            put("accuracy", location.accuracy)
                            put("altitude", location.altitude)
                            put("speed", location.speed)
                            put("heading", location.bearing)
                            put("provider", location.provider)
                            put("timestamp", java.time.Instant.now().toString())
                        }
                        NetworkUtils.httpPost("$serverUrl/location/update", payload.toString(), token)
                        reportTaskCompletion(taskId, "completed", "", serverUrl, token)
                    } else {
                        reportTaskCompletion(taskId, "failed", "Location unavailable on device GPS", serverUrl, token)
                    }
                }
            }
        } catch (e: Exception) {
            reportTaskCompletion(taskId, "failed", e.message ?: "Task execution error", serverUrl, token)
        }
    }

    private fun reportTaskCompletion(taskId: String, status: String, errorMsg: String, serverUrl: String, token: String) {
        serviceScope.launch {
            try {
                val payload = JSONObject().apply {
                    put("taskId", taskId)
                    put("status", status)
                    put("errorInfo", errorMsg)
                }
                NetworkUtils.httpPost("$serverUrl/sync/complete", payload.toString(), token)
            } catch (e: Exception) {
                Log.e("SyncPullWorker", "Failed to report task completion: ${e.message}")
            }
        }
    }

    private fun getLastKnownLocation(): Location? {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return try {
            val gpsLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val netLoc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            gpsLoc ?: netLoc
        } catch (e: SecurityException) {
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
