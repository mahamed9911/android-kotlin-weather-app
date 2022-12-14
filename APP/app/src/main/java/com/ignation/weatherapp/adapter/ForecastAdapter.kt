package com.ignation.weatherapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ignation.weatherapp.convertDegreeToString
import com.ignation.weatherapp.convertLongToDate
import com.ignation.weatherapp.databinding.ForecastLayoutBinding
import com.ignation.weatherapp.setImage

class ForecastAdapter(private val forecasts: List<Forecast>) : RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder>() {

    class ForecastViewHolder(val binding: ForecastLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ForecastLayoutBinding.inflate(layoutInflater, parent, false)
        return ForecastViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ForecastViewHolder, position: Int) {
        holder.binding.apply {
            forecastDegree.text = convertDegreeToString(forecasts[position].temp)
            setImage(forecasts[position].weatherCondition, forecastWeatherImage)
            forecastDate.text = convertLongToDate(forecasts[position].date)
        }
    }

    override fun getItemCount(): Int {
        return forecasts.size
    }
}