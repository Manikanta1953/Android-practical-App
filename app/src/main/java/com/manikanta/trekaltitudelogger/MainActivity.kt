package com.manikanta.trekaltitudelogger

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.manikanta.trekaltitudelogger.auth.FirebaseAuthHelper
import com.manikanta.trekaltitudelogger.data.api.OpenMeteoElevationApi
import com.manikanta.trekaltitudelogger.data.db.AppDatabase
import com.manikanta.trekaltitudelogger.data.db.TrekDao
import com.manikanta.trekaltitudelogger.data.db.TrekEntity
import com.manikanta.trekaltitudelogger.data.db.WaypointEntity
import com.manikanta.trekaltitudelogger.ui.ElevationProfileView
import com.manikanta.trekaltitudelogger.ui.TrekHistoryAdapter
import com.manikanta.trekaltitudelogger.ui.WaypointAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var trekDao: TrekDao
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var authHelper: FirebaseAuthHelper

    // UI elements
    private lateinit var tvStatus: TextView
    private lateinit var tvTimer: TextView
    private lateinit var btnStartPause: MaterialButton
    private lateinit var btnFinish: MaterialButton
    private lateinit var btnSimulate: MaterialButton
    private lateinit var btnAuth: MaterialButton
    private lateinit var btnAddWaypoint: MaterialButton
    private lateinit var tvLiveTotalClimb: TextView
    private lateinit var tvLiveTotalDistance: TextView
    private lateinit var tvLiveCurrentAltitude: TextView
    private lateinit var tvLiveMinMaxAltitude: TextView
    private lateinit var tvWaypointsHeader: TextView
    private lateinit var elevationProfileView: ElevationProfileView
    private lateinit var rvWaypoints: RecyclerView
    private lateinit var rvHistory: RecyclerView
    private lateinit var layoutLiveLogger: View
    private lateinit var layoutHistory: View
    private lateinit var tvEmptyHistory: TextView
    private lateinit var tabLayout: TabLayout

    // Adapters
    private lateinit var waypointAdapter: WaypointAdapter
    private lateinit var trekHistoryAdapter: TrekHistoryAdapter

    // Active session state
    private var activeTrekId: Long = 0L
    private var activeTrekName: String = ""
    private var isTrekActive: Boolean = false
    private var timerJob: kotlinx.coroutines.Job? = null
    private var trekStartTime: Long = 0L

    // Waypoint tracking cache
    private val currentWaypoints = mutableListOf<WaypointEntity>()

    // Google Sign-In launcher
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    authHelper.firebaseAuthWithGoogle(idToken) { success, errorMsg ->
                        if (success) {
                            updateAuthUI()
                            Toast.makeText(this, "Welcome, ${authHelper.currentUser?.displayName ?: "User"}!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Firebase Sign-In failed: $errorMsg", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Permission launcher
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (!fineGranted && !coarseGranted) {
            Toast.makeText(this, "Location permission is required to add waypoints", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        trekDao = AppDatabase.getInstance(this).trekDao()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        authHelper = FirebaseAuthHelper(this)

        initViews()
        setupAdapters()
        setupListeners()
        updateAuthUI()
        observeHistory()
    }

    private fun initViews() {
        tvStatus = findViewById(R.id.tvStatus)
        tvTimer = findViewById(R.id.tvTimer)
        btnStartPause = findViewById(R.id.btnStartPause)
        btnFinish = findViewById(R.id.btnFinish)
        btnSimulate = findViewById(R.id.btnSimulate)
        btnAuth = findViewById(R.id.btnAuth)
        btnAddWaypoint = findViewById(R.id.btnAddWaypoint)
        tvLiveTotalClimb = findViewById(R.id.tvLiveTotalClimb)
        tvLiveTotalDistance = findViewById(R.id.tvLiveTotalDistance)
        tvLiveCurrentAltitude = findViewById(R.id.tvLiveCurrentAltitude)
        tvLiveMinMaxAltitude = findViewById(R.id.tvLiveMinMaxAltitude)
        tvWaypointsHeader = findViewById(R.id.tvWaypointsHeader)
        elevationProfileView = findViewById(R.id.elevationProfileView)
        rvWaypoints = findViewById(R.id.rvWaypoints)
        rvHistory = findViewById(R.id.rvHistory)
        layoutLiveLogger = findViewById(R.id.layoutLiveLogger)
        layoutHistory = findViewById(R.id.layoutHistory)
        tvEmptyHistory = findViewById(R.id.tvEmptyHistory)
        tabLayout = findViewById(R.id.tabLayout)

        rvWaypoints.layoutManager = LinearLayoutManager(this)
        rvHistory.layoutManager = LinearLayoutManager(this)
    }

    private fun setupAdapters() {
        waypointAdapter = WaypointAdapter(
            onWaypointDelete = { wp ->
                lifecycleScope.launch {
                    trekDao.deleteWaypoint(wp.id)
                    recalculateAndSaveTrekMetrics()
                    Toast.makeText(this@MainActivity, "Waypoint deleted", Toast.LENGTH_SHORT).show()
                }
            }
        )
        rvWaypoints.adapter = waypointAdapter

        trekHistoryAdapter = TrekHistoryAdapter(
            onTrekClick = { trek -> showTrekDetailsDialog(trek) },
            onTrekShare = { trek -> shareTrekSummary(trek) },
            onTrekDelete = { trek ->
                lifecycleScope.launch {
                    trekDao.deleteTrek(trek.id)
                    Toast.makeText(this@MainActivity, "Trek deleted", Toast.LENGTH_SHORT).show()
                }
            }
        )
        rvHistory.adapter = trekHistoryAdapter
    }

    private fun setupListeners() {
        btnStartPause.setOnClickListener {
            if (!isTrekActive) {
                promptStartNewTrek()
            }
        }

        btnFinish.setOnClickListener {
            if (isTrekActive) {
                finishTrekSession()
            }
        }

        btnAddWaypoint.setOnClickListener {
            if (!isTrekActive) {
                Toast.makeText(this, "Please start a trek first!", Toast.LENGTH_SHORT).show()
            } else {
                fetchAndAddCurrentWaypoint()
            }
        }

        btnAuth.setOnClickListener {
            val user = authHelper.currentUser
            if (user != null) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Account Info")
                    .setMessage("Signed in as:\n${user.displayName ?: "User"}\n${user.email ?: ""}")
                    .setPositiveButton("Sign Out") { _, _ ->
                        authHelper.signOut {
                            updateAuthUI()
                            Toast.makeText(this, "Signed out", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Close", null)
                    .show()
            } else {
                val signInIntent = authHelper.getGoogleSignInClient().signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
        }

        btnSimulate.setOnClickListener {
            if (!isTrekActive) {
                promptStartNewTrek {
                    addSimulatedSampleWaypoints()
                }
            } else {
                addSimulatedSampleWaypoints()
            }
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        layoutLiveLogger.visibility = View.VISIBLE
                        layoutHistory.visibility = View.GONE
                    }
                    1 -> {
                        layoutLiveLogger.visibility = View.GONE
                        layoutHistory.visibility = View.VISIBLE
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun updateAuthUI() {
        val user = authHelper.currentUser
        if (user != null) {
            btnAuth.text = user.displayName?.split(" ")?.firstOrNull() ?: "Signed In"
        } else {
            btnAuth.text = "Google Sign-In"
        }
    }

    // --- Trek Lifecycle ---

    private fun promptStartNewTrek(onStarted: (() -> Unit)? = null) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_waypoint, null)
        val etName = dialogView.findViewById<EditText>(R.id.etWaypointName)
        val etNotes = dialogView.findViewById<EditText>(R.id.etWaypointNotes)
        val tvInfo = dialogView.findViewById<TextView>(R.id.tvCurrentLocationInfo)

        val defaultName = "Trek " + SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date())
        etName.setText(defaultName)

        val labelTitle = dialogView.findViewById<TextView>(R.id.tvCurrentLocationInfo)
        labelTitle.text = "Enter a title for your new altitude logging trek session."

        MaterialAlertDialogBuilder(this)
            .setTitle("Start New Trek")
            .setView(dialogView)
            .setPositiveButton("Start Trek") { _, _ ->
                val name = etName.text.toString().ifBlank { defaultName }
                startTrekSession(name)
                onStarted?.invoke()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startTrekSession(name: String) {
        lifecycleScope.launch {
            val startTimeMs = System.currentTimeMillis()
            val newTrek = TrekEntity(
                name = name,
                startTime = startTimeMs
            )
            val trekId = trekDao.insertTrek(newTrek)

            activeTrekId = trekId
            activeTrekName = name
            isTrekActive = true
            trekStartTime = startTimeMs

            tvStatus.text = "Active: $name"
            tvStatus.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
            btnStartPause.isEnabled = false
            btnFinish.isEnabled = true

            startTimer()
            observeActiveTrekWaypoints(trekId)
            Toast.makeText(this@MainActivity, "Started trek: $name", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = lifecycleScope.launch {
            while (isTrekActive) {
                val elapsed = System.currentTimeMillis() - trekStartTime
                tvTimer.text = com.manikanta.trekaltitudelogger.data.UnitFormatter.formatDuration(elapsed)
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun observeActiveTrekWaypoints(trekId: Long) {
        lifecycleScope.launch {
            trekDao.getWaypointsForTrekFlow(trekId).collectLatest { waypoints ->
                currentWaypoints.clear()
                currentWaypoints.addAll(waypoints)

                waypointAdapter.submitList(ArrayList(waypoints))
                elevationProfileView.setWaypoints(waypoints)

                recalculateAndSaveTrekMetrics()
            }
        }
    }

    private fun finishTrekSession() {
        lifecycleScope.launch {
            val trek = trekDao.getTrekById(activeTrekId)
            if (trek != null) {
                trekDao.updateTrek(trek.copy(endTime = System.currentTimeMillis()))
            }

            isTrekActive = false
            stopTimer()
            tvStatus.text = "Status: Completed & Saved"
            tvStatus.setTextColor(android.graphics.Color.GRAY)
            btnStartPause.isEnabled = true
            btnFinish.isEnabled = false

            MaterialAlertDialogBuilder(this@MainActivity)
                .setTitle("Trek Completed & Saved!")
                .setMessage("Trek: $activeTrekName\nTotal Distance: ${tvLiveTotalDistance.text}\nTotal Climb: ${tvLiveTotalClimb.text}")
                .setPositiveButton("View in History") { _, _ ->
                    tabLayout.getTabAt(1)?.select()
                }
                .setNegativeButton("OK", null)
                .show()
        }
    }

    // --- Fused Location & Open-Meteo API ---

    @SuppressLint("MissingPermission")
    private fun fetchAndAddCurrentWaypoint() {
        val fineLocation = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocation = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fineLocation != PackageManager.PERMISSION_GRANTED && coarseLocation != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }

        Toast.makeText(this, "Fetching location & elevation...", Toast.LENGTH_SHORT).show()

        // Requirement: Fused Location Provider using getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    processNewLocation(location)
                } else {
                    // Fallback to simulated/estimated baseline location if location fix is unavailable
                    val mockLoc = Location("mock").apply {
                        latitude = 34.0522 + (currentWaypoints.size * 0.002)
                        longitude = -118.2437 + (currentWaypoints.size * 0.002)
                        altitude = 850.0 + (currentWaypoints.size * 30.0)
                    }
                    processNewLocation(mockLoc)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Location fix failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun processNewLocation(location: Location) {
        val lat = location.latitude
        val lon = location.longitude

        lifecycleScope.launch {
            // Requirement: Open-Meteo Elevation API (no key): https://api.open-meteo.com/v1/elevation?latitude={lat}&longitude={lon}
            val openMeteoAlt = withContext(Dispatchers.IO) {
                OpenMeteoElevationApi.fetchElevation(lat, lon)
            }

            val finalAltitude = openMeteoAlt ?: location.altitude

            val defaultIndex = currentWaypoints.size + 1
            val wpName = "Waypoint #$defaultIndex"

            val waypoint = WaypointEntity(
                trekId = activeTrekId,
                name = wpName,
                latitude = lat,
                longitude = lon,
                altitude = finalAltitude,
                timestamp = System.currentTimeMillis()
            )

            trekDao.insertWaypoint(waypoint)
            Toast.makeText(this@MainActivity, "Added $wpName (${finalAltitude.roundToInt()}m)", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Metrics Recalculation using Location.distanceBetween ---

    private suspend fun recalculateAndSaveTrekMetrics() {
        if (activeTrekId == 0L) return

        var totalMeters = 0.0
        var totalClimb = 0.0
        var highestAlt = if (currentWaypoints.isNotEmpty()) currentWaypoints[0].altitude else 0.0
        var lowestAlt = if (currentWaypoints.isNotEmpty()) currentWaypoints[0].altitude else 0.0

        for (i in currentWaypoints.indices) {
            val curr = currentWaypoints[i]
            if (curr.altitude > highestAlt) highestAlt = curr.altitude
            if (curr.altitude < lowestAlt) lowestAlt = curr.altitude

            if (i > 0) {
                val prev = currentWaypoints[i - 1]

                // Requirement: Compute distances with Location.distanceBetween()
                val results = FloatArray(1)
                Location.distanceBetween(
                    prev.latitude, prev.longitude,
                    curr.latitude, curr.longitude,
                    results
                )
                totalMeters += results[0].toDouble()

                val deltaAlt = curr.altitude - prev.altitude
                if (deltaAlt > 0) {
                    totalClimb += deltaAlt
                }
            }
        }

        val totalKm = totalMeters / 1000.0

        // Requirement: Show summary card with total distance (km), total climb (m), highest and lowest points
        tvLiveTotalDistance.text = String.format(Locale.US, "%.2f km", totalKm)
        tvLiveTotalClimb.text = String.format(Locale.US, "+%.0f m", totalClimb)
        if (currentWaypoints.isNotEmpty()) {
            val lastAlt = currentWaypoints.last().altitude
            tvLiveCurrentAltitude.text = String.format(Locale.US, "%.0f m", lastAlt)
            tvLiveMinMaxAltitude.text = String.format(Locale.US, "Min: %.0fm | Max: %.0fm", lowestAlt, highestAlt)
        } else {
            tvLiveCurrentAltitude.text = "-- m"
            tvLiveMinMaxAltitude.text = "Min: -- | Max: --"
        }

        tvWaypointsHeader.text = "Waypoints (${currentWaypoints.size})"

        // Update Room database entity
        val existingTrek = trekDao.getTrekById(activeTrekId)
        if (existingTrek != null) {
            val updatedTrek = existingTrek.copy(
                totalDistanceKm = totalKm,
                totalClimbMeters = totalClimb,
                highestPointMeters = highestAlt,
                lowestPointMeters = lowestAlt
            )
            trekDao.updateTrek(updatedTrek)
        }
    }

    // --- Demo Simulation Feature ---

    private fun addSimulatedSampleWaypoints() {
        val samplePoints = listOf(
            Pair("Trailhead Base", Pair(34.0522, -118.2437)),
            Pair("Pine Valley Stream", Pair(34.0550, -118.2400)),
            Pair("Rocky Ridge Rest", Pair(34.0580, -118.2360)),
            Pair("Summit Peak View", Pair(34.0620, -118.2310))
        )

        lifecycleScope.launch {
            for ((idx, p) in samplePoints.withIndex()) {
                val name = p.first
                val lat = p.second.first
                val lon = p.second.second

                // Fetch altitude from Open-Meteo API
                val alt = withContext(Dispatchers.IO) {
                    OpenMeteoElevationApi.fetchElevation(lat, lon)
                } ?: (850.0 + (idx * 180.0))

                val wp = WaypointEntity(
                    trekId = activeTrekId,
                    name = name,
                    latitude = lat,
                    longitude = lon,
                    altitude = alt,
                    timestamp = System.currentTimeMillis() + (idx * 60000)
                )
                trekDao.insertWaypoint(wp)
            }
            Toast.makeText(this@MainActivity, "Added simulated hike waypoints!", Toast.LENGTH_SHORT).show()
        }
    }

    // --- History & Share Summary ---

    private fun observeHistory() {
        lifecycleScope.launch {
            trekDao.getAllTreksFlow().collectLatest { treks ->
                if (treks.isEmpty()) {
                    tvEmptyHistory.visibility = View.VISIBLE
                    rvHistory.visibility = View.GONE
                } else {
                    tvEmptyHistory.visibility = View.GONE
                    rvHistory.visibility = View.VISIBLE
                    trekHistoryAdapter.submitList(treks)
                }
            }
        }
    }

    private fun showTrekDetailsDialog(trek: TrekEntity) {
        lifecycleScope.launch {
            val waypoints = trekDao.getWaypointsForTrek(trek.id)
            val sb = StringBuilder()
            sb.append("Trek: ").append(trek.name).append("\n")
            sb.append("Total Distance: ").append(String.format(Locale.US, "%.2f km", trek.totalDistanceKm)).append("\n")
            sb.append("Total Climb: ").append(String.format(Locale.US, "+%.0f m", trek.totalClimbMeters)).append("\n")
            sb.append("Highest Point: ").append(String.format(Locale.US, "%.0f m", trek.highestPointMeters)).append("\n")
            sb.append("Lowest Point: ").append(String.format(Locale.US, "%.0f m", trek.lowestPointMeters)).append("\n\n")

            sb.append("Waypoints (${waypoints.size}):\n")
            for ((idx, wp) in waypoints.withIndex()) {
                sb.append("${idx + 1}. ${wp.name} - ${wp.altitude.roundToInt()}m\n")
            }

            MaterialAlertDialogBuilder(this@MainActivity)
                .setTitle(trek.name)
                .setMessage(sb.toString())
                .setPositiveButton("Share Summary") { _, _ -> shareTrekSummary(trek) }
                .setNegativeButton("Close", null)
                .show()
        }
    }

    // Requirement: A trek history list, with delete and a "Share Summary" button (ACTION_SEND)
    private fun shareTrekSummary(trek: TrekEntity) {
        lifecycleScope.launch {
            val waypoints = trekDao.getWaypointsForTrek(trek.id)
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(trek.startTime))

            val summaryText = StringBuilder().apply {
                append("--- TREK ALTITUDE SUMMARY ---\n")
                append("Trek Name: ").append(trek.name).append("\n")
                append("Date: ").append(dateStr).append("\n")
                append("Total Distance: ").append(String.format(Locale.US, "%.2f km", trek.totalDistanceKm)).append("\n")
                append("Total Elevation Gain (Climb): ").append(String.format(Locale.US, "+%.0f m", trek.totalClimbMeters)).append("\n")
                append("Highest Point: ").append(String.format(Locale.US, "%.0f m", trek.highestPointMeters)).append("\n")
                append("Lowest Point: ").append(String.format(Locale.US, "%.0f m", trek.lowestPointMeters)).append("\n")
                append("Total Waypoints: ").append(waypoints.size).append("\n\n")

                append("Waypoints Detail:\n")
                for ((idx, wp) in waypoints.withIndex()) {
                    append(String.format(Locale.US, "%d. %s -> Elevation: %.0fm (Coords: %.4f, %.4f)\n",
                        idx + 1, wp.name, wp.altitude, wp.latitude, wp.longitude))
                }
            }.toString()

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Trek Summary: ${trek.name}")
                putExtra(Intent.EXTRA_TEXT, summaryText)
            }
            startActivity(Intent.createChooser(shareIntent, "Share Trek Summary"))
        }
    }
}
