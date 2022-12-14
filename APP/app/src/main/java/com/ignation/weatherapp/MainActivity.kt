package com.ignation.weatherapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.ignation.weatherapp.adapter.ForecastAdapter
import com.ignation.weatherapp.databinding.ActivityMainBinding
import com.ignation.weatherapp.network.model.currentweather.WeatherResponse
import com.ignation.weatherapp.viewmodel.WeatherViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException

const val TAG: String = "MainActivity"

class MainActivity : AppCompatActivity() {

    private val viewModel: WeatherViewModel by viewModels()

    private lateinit var binding: ActivityMainBinding
    private lateinit var userSettings: UserSettings
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userSettings = UserSettings(this)

        sharedPreferences = getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE
        )

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        val savedLocation = sharedPreferences.getString(LAST_LOCATION, DEFAULT_VALUE)!!
        if (savedLocation != DEFAULT_VALUE) {
            performSearchByName(savedLocation)
        }

        binding.showLocation.setOnClickListener {
            editLocation()
        }

        viewModel.response.observe(this) {
            sharedPreferences.edit().putString(LAST_LOCATION, it.name).apply()
        }
    }

    override fun onStart() {
        super.onStart()
        if (isOnline()) {
            getLocation()
        } else {
            showOffline()
        }
    }

    private fun getLocation() {
        if (userSettings.checkPermission()) {
            Log.d(TAG, "getLocation: Has permission")
            if (userSettings.isLocatingEnabled()) {
                Log.d(TAG, "getLocation: Locating enabled")
                fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                    Log.d(TAG, "getLocation: success")
                    if (location != null) {
                        binding.progressBar.visibility = View.VISIBLE
                        CoroutineScope(Dispatchers.Main).launch {
                            viewModel.setResponse(viewModel.getResponseByLocation(location))
                            viewModel.setForecast(viewModel.getFutureForecast())
                            binding.progressBar.visibility = View.GONE
                            bindViews(viewModel.response.value!!)
                            bindRecyclerView()
                        }
                    } else {
                        enterLocation()
                    }
                }
            } else {
                val savedLocation = sharedPreferences.getString(LAST_LOCATION, DEFAULT_VALUE)!!
                if (savedLocation == DEFAULT_VALUE) {
                    showGpsDialog()
                }
            }
        } else {
            Log.d(TAG, "getLocation: No permission")
            userSettings.requestPermissions()
        }
    }

    private fun bindViews(response: WeatherResponse) {
        binding.apply {
            manualLocation.visibility = View.INVISIBLE
            binding.internetImage.visibility = View.GONE
            showLocation.visibility = View.VISIBLE
            showLocation.text = response.name
            degreeDisplay.text = convertDegreeToString(viewModel.response.value!!.main.temp)
            manualLocation.text.clear()
            weatherDesc.text = response.weather[0].description.replaceFirstChar {
                it.titlecase()
            }
            humidityData.text = getString(R.string.humidity_level, response.main.humidity)
            windData.text = getString(R.string.wind_level, response.wind.speed)
            visibilityData.text =
                getString(R.string.visibility_level, response.visibility / 1000)
            divider.visibility = View.VISIBLE
            weatherIndicators.visibility = View.VISIBLE
        }

        setImage(viewModel.response.value!!.weather[0].main, binding.weatherImage)
    }

    private fun bindRecyclerView() {
        binding.nextDaysTitle.visibility = View.VISIBLE
        val adapter = ForecastAdapter(viewModel.forecast.value!!.asModel())
        binding.recyclerForecast.adapter = adapter
    }

    private fun editLocation() {
        if (binding.showLocation.visibility == View.VISIBLE) {
            binding.showLocation.visibility = View.GONE
        }
        binding.manualLocation.visibility = View.VISIBLE
        binding.searchButton.visibility = View.VISIBLE

        clickSearchButton()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Log.d(TAG, "onRequestPermissionsResult: called")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_RQ && permissions.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation()
            } else {
                if (sharedPreferences.getString(LAST_LOCATION, DEFAULT_VALUE) == DEFAULT_VALUE) {
                    enterLocation()
                }
            }
        }
    }

    private fun enterLocation() {
        binding.manualLocation.visibility = View.VISIBLE
        binding.searchButton.visibility = View.VISIBLE
        clickSearchButton()
    }

    private fun clickSearchButton() {
        binding.searchButton.setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)
            binding.progressBar.visibility = View.VISIBLE
            val cityName = binding.manualLocation.text.toString()
            performSearchByName(cityName)
        }
    }

    private fun performSearchByName(cityName: String) {
        var isError = false
        CoroutineScope(Dispatchers.Main).launch {
            try {
                viewModel.setResponse(viewModel.getResponseByName(cityName))
                viewModel.setForecast(viewModel.getFutureForecast())
            } catch (e: HttpException) {
                isError = true
            }
            if (!isError) {
                binding.progressBar.visibility = View.GONE
                binding.searchButton.visibility = View.INVISIBLE
                bindViews(viewModel.response.value!!)
                bindRecyclerView()
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "No information about $cityName",
                    Toast.LENGTH_SHORT
                ).show()
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun showGpsDialog() {
        val gpsDialog = AlertDialog.Builder(this)
            .setTitle(R.string.gps_dialog_title)
            .setMessage(R.string.gps_dialog_message)
            .setIcon(R.drawable.ic_baseline_gps_fixed_24)
            .setPositiveButton("Turn on") { _, _ ->
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton("Cancel") { _, _ ->
                enterLocation()
            }.create()

        gpsDialog.show()
    }

    private fun isOnline(): Boolean {
        val connectivityManager =
            this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager != null) {
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                        Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun showOffline() {
        if (binding.weatherImage.drawable == null) {
            Toast.makeText(this, "No Internet connection", Toast.LENGTH_SHORT).show()
        } else {
            binding.internetImage.visibility = View.VISIBLE
        }
    }
}