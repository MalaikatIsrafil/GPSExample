package id.ac.polbeng.riskiafendi.gpsexample

import android.Manifest
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.text.method.ScrollingMovementMethod
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.Task
import id.ac.polbeng.riskiafendi.gpsexample.databinding.ActivityMainBinding

import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var trackingLocation: Boolean = false
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.tvLocation.text = ""

        binding.tvLocation.movementMethod = ScrollingMovementMethod()
        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {

                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION,
                    false) -> {
                    // Akses lokasi yang tepat diberikan.
                }

                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION,
                    false) -> {
                    // Hanya perkiraan akses lokasi yang diberikan.
                } else -> {
                // Tidak ada akses lokasi yang diberikan.
                binding.btnUpdate.isEnabled = false
            }
            }
        }
        locationPermissionRequest.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION))
        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)
        val builder = LocationSettingsRequest.Builder()
        val client: SettingsClient =
            LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> =
            client.checkLocationSettings(builder.build())
        task.addOnSuccessListener {
            locationRequest = LocationRequest.create().apply {
                interval = TimeUnit.SECONDS.toMillis(20)

                // Menetapkan kecepatan tercepat untuk pembaruan lokasi aktif. Interval ini tepat, dan milik Anda
                // aplikasi tidak akan menerima pembaruan lebih sering dari nilai ini.
                fastestInterval = TimeUnit.SECONDS.toMillis(10)

                // Menyetel waktu maksimum saat pembaruan lokasi batch dikirimkan. Pembaruan mungkin
                // dikirim lebih cepat dari interval ini.
                        maxWaitTime = TimeUnit.SECONDS.toMillis(40)

                //Perpindahan terkecil = 107f //170m = 0,1 mil
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult:
                                              LocationResult) {
                    /*for (location in locationResult.locations){
                    val latitude = location.latitude.toString()
                   val longitude = location.longitude.toString()
                   logResultsToScreen("$latitude, $longitude")
                    }*/
                    if (locationResult.locations.isNotEmpty()) {
                        // dapatkan lokasi terbaru
                        val location = locationResult.lastLocation

                        // gunakan objek lokasi Anda
                        // dapatkan informasi lintang, bujur, dan lainnya dari ini

                        val latitude = location.latitude.toString()
                        val longitude = location.longitude.toString()
                        logResultsToScreen("$latitude, $longitude")
                    }
                }
            }
        }
        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException){
                // Pengaturan lokasi tidak memuaskan, tapi ini bisa diperbaiki
                // dengan menampilkan dialog kepada pengguna.
                        try {

                            // Tampilkan dialog dengan menelepon startResolutionForResult(),
                            // dan periksa hasilnya onActivityResult().
                            exception.startResolutionForResult(this@MainActivity, 100)
                        } catch (sendEx: IntentSender.SendIntentException) {
                            // Abaikan kesalahannya.
                        }
            }
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location : Location? ->

                // Mendapatkan lokasi terakhir yang diketahui. Dalam beberapa situasi yang jarang terjadi, ini bisa menjadi nol.
            logResultsToScreen("${location?.latitude}, ${location?.longitude}")
            }
        binding.btnUpdate.setOnClickListener {
            if(!trackingLocation){
                startLocationUpdates()
                trackingLocation = true
            }else{
                stopLocationUpdates()
                trackingLocation = false
            }
            updateButtonState(trackingLocation)
        }
    }
    private fun logResultsToScreen(output: String) {
        val outputWithPreviousLogs =
            "$output\n${binding.tvLocation.text}"
        binding.tvLocation.text = outputWithPreviousLogs
    }
    private fun updateButtonState(trackingLocation: Boolean) {
        if (trackingLocation) {
            binding.btnUpdate.text = getString(R.string.stop_update)
        } else {
            binding.btnUpdate.text = getString(R.string.start_update)
        }
    }
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        Log.d(TAG, "Request Location Updates")
    }
    private fun stopLocationUpdates() {
        //fusedLocationClient.removeLocationUpdates(locationCallback)
        val removeTask =
            fusedLocationClient.removeLocationUpdates(locationCallback)
        removeTask.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "Location Callback removed.")
            } else {
                Log.d(TAG, "Failed to remove Location Callback.")
            }
        }
    }
    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }


}
