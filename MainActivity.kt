package com.example.projectapplication2

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.projectapplication2.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var apiService: WeatherApiService
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    
    // IMPORTANT: Replace with your actual OpenWeatherMap API Key
    private val API_KEY = "a697db8e75e0bde3dc2d57099e5f4dd4"

    private val citySuggestions = arrayOf(
        "Ludhiana", "Amritsar", "Jalandhar", "Patiala", "Bathinda", "Hoshiarpur", "Mohali",
        "Lahore", "Faisalabad", "Multan", "Rawalpindi", "Gujranwala",
        "Delhi", "Mumbai", "Bangalore", "Chennai", "Kolkata", "Hyderabad", "Pune", "Ahmedabad",
        "London", "New York", "Paris", "Tokyo", "Dubai", "Sydney", "Berlin"
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            getCurrentLocationWeather()
        } else {
            // Permission denied, use default city
            fetchWeatherData("Ludhiana")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupRetrofit()
        setupRecyclerView()
        setupSearchAutoComplete()

        binding.btnSearch.setOnClickListener {
            val city = binding.etCitySearch.text.toString().trim()
            if (city.isNotEmpty()) {
                fetchWeatherData(city)
            } else {
                Toast.makeText(this, "Please enter a city", Toast.LENGTH_SHORT).show()
            }
        }

        // Try to get weather for current location on startup
        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && 
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            
            requestPermissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        } else {
            getCurrentLocationWeather()
        }
    }

    private fun getCurrentLocationWeather() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    fetchWeatherDataByCoords(location.latitude, location.longitude)
                } else {
                    // Location null, use default
                    fetchWeatherData("Ludhiana")
                }
            }.addOnFailureListener {
                fetchWeatherData("Ludhiana")
            }
        } catch (e: SecurityException) {
            fetchWeatherData("Ludhiana")
        }
    }

    private fun setupSearchAutoComplete() {
        val adapter = ArrayAdapter(this, R.layout.custom_dropdown_item, citySuggestions)
        binding.etCitySearch.setAdapter(adapter)
        binding.etCitySearch.setOnItemClickListener { parent, _, position, _ ->
            val city = parent.getItemAtPosition(position).toString()
            fetchWeatherData(city)
        }
    }

    private fun setupRetrofit() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(WeatherApiService::class.java)
    }

    private fun setupRecyclerView() {
        binding.rvForecast.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    }

    private fun fetchWeatherData(city: String) {
        if (API_KEY == "YOUR_API_KEY_HERE") {
            Toast.makeText(this, "API Key is missing", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val weatherResponse = apiService.getCurrentWeather(city, apiKey = API_KEY)
                val forecastResponse = apiService.getForecast(city, apiKey = API_KEY)

                if (weatherResponse.isSuccessful && weatherResponse.body() != null) {
                    updateCurrentWeatherUI(weatherResponse.body()!!)
                } else {
                    val errorMsg = when(weatherResponse.code()) {
                        401 -> "Invalid API Key. Please check your key."
                        404 -> "City not found."
                        else -> "Error: ${weatherResponse.code()}"
                    }
                    Toast.makeText(this@MainActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }

                if (forecastResponse.isSuccessful && forecastResponse.body() != null) {
                    updateForecastUI(forecastResponse.body()!!)
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchWeatherDataByCoords(lat: Double, lon: Double) {
        lifecycleScope.launch {
            try {
                val weatherResponse = apiService.getWeatherByLocation(lat, lon, apiKey = API_KEY)
                val forecastResponse = apiService.getForecastByLocation(lat, lon, apiKey = API_KEY)

                if (weatherResponse.isSuccessful && weatherResponse.body() != null) {
                    updateCurrentWeatherUI(weatherResponse.body()!!)
                }
                if (forecastResponse.isSuccessful && forecastResponse.body() != null) {
                    updateForecastUI(forecastResponse.body()!!)
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateCurrentWeatherUI(weather: WeatherResponse) {
        binding.tvCityName.text = "${weather.name}, ${weather.sys.country}"
        binding.tvTemperature.text = "${weather.main.temp.toInt()}°C"
        binding.tvDescription.text = weather.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() }
        binding.tvHumidity.text = "Humidity: ${weather.main.humidity}%"
        binding.tvWindSpeed.text = "Wind: ${weather.wind.speed} m/s"

        val iconCode = weather.weather.firstOrNull()?.icon
        val iconUrl = "https://openweathermap.org/img/wn/$iconCode@4x.png"
        binding.ivWeatherIcon.load(iconUrl)
    }

    private fun updateForecastUI(forecast: ForecastResponse) {
        val adapter = ForecastAdapter(forecast.list.take(10))
        binding.rvForecast.adapter = adapter
    }
}
