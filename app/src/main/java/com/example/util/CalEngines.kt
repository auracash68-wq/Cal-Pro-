package com.example.util

import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit
import kotlin.math.pow

object CalEngines {

    // EMI calculation model
    data class EmiResult(
        val monthlyEmi: Double,
        val totalInterest: Double,
        val totalPayment: Double
    )

    fun calculateEmi(principal: Double, annualRate: Double, tenureYears: Double): EmiResult {
        if (principal <= 0.0 || annualRate <= 0.0 || tenureYears <= 0.0) {
            return EmiResult(0.0, 0.0, 0.0)
        }
        val r = annualRate / 12.0 / 100.0
        val n = tenureYears * 12.0
        
        val emi = if (r == 0.0) {
            principal / n
        } else {
            principal * r * (1.0 + r).pow(n) / ((1.0 + r).pow(n) - 1.0)
        }
        
        val totalPayment = emi * n
        val totalInterest = totalPayment - principal
        return EmiResult(emi, totalInterest, totalPayment)
    }

    // Age calculation model
    data class AgeResult(
        val years: Int,
        val months: Int,
        val days: Int
    )

    fun calculateAge(dob: LocalDate, today: LocalDate = LocalDate.now()): AgeResult {
        if (dob.isAfter(today)) {
            return AgeResult(0, 0, 0)
        }
        val period = Period.between(dob, today)
        return AgeResult(period.years, period.months, period.days)
    }

    // GST calculation
    data class GstResult(
        val gstAmount: Double,
        val finalAmount: Double
    )

    fun calculateGst(amount: Double, ratePercent: Double): GstResult {
        if (amount <= 0.0) return GstResult(0.0, 0.0)
        val gst = amount * (ratePercent / 100.0)
        return GstResult(gst, amount + gst)
    }

    // Unit Converter configurations
    object Units {
        // Length conversion relative to Meters
        val lengthFactors = mapOf(
            "Meters" to 1.0,
            "Kilometers" to 1000.0,
            "Miles" to 1609.344,
            "Feet" to 0.3048,
            "Inches" to 0.0254
        )

        // Weight conversion relative to Grams
        val weightFactors = mapOf(
            "Grams" to 1.0,
            "Kilograms" to 1000.0,
            "Pounds" to 453.59237,
            "Ounces" to 28.34952
        )

        // Area conversion relative to Square Meters
        val areaFactors = mapOf(
            "Sq Meters" to 1.0,
            "Sq Kilometers" to 1000000.0,
            "Acres" to 4046.856,
            "Hectares" to 10000.0
        )
    }

    fun convertUnits(category: String, value: Double, fromUnit: String, toUnit: String): Double {
        if (category == "Temperature") {
            // Special conversion for celsius/fahrenheit/kelvin
            val celsiusValue = when (fromUnit) {
                "Celsius" -> value
                "Fahrenheit" -> (value - 32.0) * 5.0 / 9.0
                "Kelvin" -> value - 273.15
                else -> value
            }
            return when (toUnit) {
                "Celsius" -> celsiusValue
                "Fahrenheit" -> celsiusValue * 9.0 / 5.0 + 32.0
                "Kelvin" -> celsiusValue + 273.15
                else -> celsiusValue
            }
        }

        val factors = when (category) {
            "Length" -> Units.lengthFactors
            "Weight" -> Units.weightFactors
            "Area" -> Units.areaFactors
            else -> return 0.0
        }

        val fromFactor = factors[fromUnit] ?: 1.0
        val toFactor = factors[toUnit] ?: 1.0

        val valueInBase = value * fromFactor
        return valueInBase / toFactor
    }

    // Currency values mapped relative to USD
    val currencyRatesToUsd = mapOf(
        "USD" to 1.0,
        "EUR" to 0.92,
        "INR" to 83.50,
        "GBP" to 0.79,
        "JPY" to 157.30,
        "CAD" to 1.37,
        "AUD" to 1.51,
        "AED" to 3.67
    )

    fun convertCurrency(amount: Double, fromCode: String, toCode: String): Double {
        if (amount <= 0.0) return 0.0
        val fromRate = currencyRatesToUsd[fromCode] ?: 1.0
        val toRate = currencyRatesToUsd[toCode] ?: 1.0
        
        // Convert to USD base first, then to target
        val amountInUsd = amount / fromRate
        return amountInUsd * toRate
    }
}
