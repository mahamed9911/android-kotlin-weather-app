package com.ignation.weatherapp.network.model.futureweather

data class Weather(
    val description: String,
    val icon: String,
    val id: Int,
    val main: String
)