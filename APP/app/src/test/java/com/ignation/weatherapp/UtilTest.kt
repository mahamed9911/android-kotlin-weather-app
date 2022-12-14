package com.ignation.weatherapp

import org.junit.Assert
import org.junit.Test
import java.util.*

class UtilTest {

    @Test
    fun convertDegreeToStringForPositive() {
        val result = convertDegreeToString(7.9)
        Assert.assertEquals("Temperature display is wrong", "+7℃", result)
    }

    @Test
    fun convertDegreeToStringFor0() {
        val result = convertDegreeToString(0.0)
        Assert.assertEquals("Temperature display is wrong", "0℃", result)
    }

    @Test
    fun convertDegreeToStringForNegative() {
        val result = convertDegreeToString(-22.1)
        Assert.assertEquals("Temperature display is wrong", "-22℃", result)
    }

    @Test
    fun convertLongToDateGives19April() {
        val newLocale = Locale("en")
        val result = convertLongToDate(1650380234, newLocale)
        Assert.assertEquals("Date display is wrong", "19 Apr", result)
    }
}