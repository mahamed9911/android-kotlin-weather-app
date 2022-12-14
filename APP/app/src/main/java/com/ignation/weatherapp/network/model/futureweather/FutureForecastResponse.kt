package com.ignation.weatherapp.network.model.futureweather

import com.ignation.weatherapp.adapter.Forecast

data class FutureForecastResponse(
    val daily: List<Daily>,
    val lat: Double,
    val lon: Double,
    val timezone: String,
    val timezone_offset: Int
) {
    fun asModel(): List<Forecast> {
        val list = mutableListOf<Forecast>()
        for (i in 1 until daily.size) {
            list.add(
                Forecast(
                    temp = daily[i].temp.day,
                    date = daily[i].dt,
                    weatherCondition = daily[i].weather[0].main
                )
            )
        }
        return list
    }
}