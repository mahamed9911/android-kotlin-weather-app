package com.ignation.weatherapp

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.ignation.weatherapp.network.model.currentweather.WeatherResponse
import com.ignation.weatherapp.network.model.futureweather.FutureForecastResponse
import com.ignation.weatherapp.viewmodel.WeatherViewModel
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class WeatherViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: WeatherViewModel

    @Before
    fun setViewModel() {
        viewModel = WeatherViewModel()
    }

    @Test
    fun setResponseReturnsTemperature100_1() {
        val mock = mockk<WeatherResponse>()
        every { mock.main.temp } returns 100.1
        viewModel.setResponse(mock)

        assertEquals(100.1, viewModel.response.value!!.main.temp, 0.0)
    }

    @Test
    fun setForecastReturnsTomorrowDayTemperature25_4() {
        val mock = mockk<FutureForecastResponse>()
        every { mock.daily[1].temp.day } returns 25.4
        viewModel.setForecast(mock)

        assertEquals(25.4, viewModel.forecast.value!!.daily[1].temp.day, 0.0)
    }
}