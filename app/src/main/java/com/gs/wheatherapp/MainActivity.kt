package com.gs.wheatherapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class MainActivity : AppCompatActivity() {

    private lateinit var cityInput: EditText
    private lateinit var searchButton: Button
    private lateinit var cityText: TextView
    private lateinit var temperatureText: TextView
    private lateinit var conditionText: TextView
    private lateinit var humidityText: TextView
    private lateinit var windText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cityInput = findViewById(R.id.cityInput)
        searchButton = findViewById(R.id.searchButton)
        cityText = findViewById(R.id.cityText)
        temperatureText = findViewById(R.id.temperatureText)
        conditionText = findViewById(R.id.conditionText)
        humidityText = findViewById(R.id.humidityText)
        windText = findViewById(R.id.windText)

        searchButton.setOnClickListener {
            val city = cityInput.text.toString().trim()

            if (city.isEmpty()) {
                Toast.makeText(
                    this,
                    "Please enter a city name",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                getWeather(city)
            }
        }
    }

    private fun getWeather(city: String) {
        Thread {
            try {
                val encodedCity = URLEncoder.encode(city, "UTF-8")
                val geoUrl = URL(
                    "https://geocoding-api.open-meteo.com/v1/search" +
                            "?name=$encodedCity&count=1&language=en&format=json"
                )

                val geoJson = JSONObject(request(geoUrl))
                val results = geoJson.optJSONArray("results")

                if (results == null || results.length() == 0) {
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            "City not found",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@Thread
                }

                val place = results.getJSONObject(0)
                val latitude = place.getDouble("latitude")
                val longitude = place.getDouble("longitude")
                val placeName = place.getString("name")

                val weatherUrl = URL(
                    "https://api.open-meteo.com/v1/forecast" +
                            "?latitude=$latitude&longitude=$longitude" +
                            "&current=temperature_2m,relative_humidity_2m," +
                            "wind_speed_10m,weather_code"
                )

                val weatherJson = JSONObject(request(weatherUrl))
                val current = weatherJson.getJSONObject("current")

                val temperature = current.getDouble("temperature_2m")
                val humidity = current.getInt("relative_humidity_2m")
                val wind = current.getDouble("wind_speed_10m")
                val code = current.getInt("weather_code")
                val description = getWeatherDescription(code)

                runOnUiThread {
                    cityText.text = placeName
                    temperatureText.text = "$temperature °C"
                    conditionText.text = description
                    humidityText.text = "Humidity: $humidity%"
                    windText.text = "Wind: $wind km/h"
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Unable to get weather. Check your internet.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun request(url: URL): String {
        val connection = url.openConnection() as HttpURLConnection

        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            connection.inputStream.bufferedReader().use {
                it.readText()
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun getWeatherDescription(code: Int): String {
        return when (code) {
            0 -> "Clear sky"
            1, 2 -> "Mainly clear / partly cloudy"
            3 -> "Overcast"
            45, 48 -> "Fog"
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rain"
            71, 73, 75 -> "Snow"
            80, 81, 82 -> "Rain showers"
            95, 96, 99 -> "Thunderstorm"
            else -> "Unknown weather"
        }
    }
}

