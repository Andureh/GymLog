package com.gymlog.app.util

import android.text.format.DateFormat
import com.gymlog.app.i18n.I18n
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

const val KG_TO_LB = 2.2046226218
private val locale: Locale get() = I18n.current.locale

fun toDisplayWeight(kg: Double, lbs: Boolean) = if (lbs) kg * KG_TO_LB else kg
fun fromDisplayWeight(v: Double, lbs: Boolean) = if (lbs) v / KG_TO_LB else v
fun unitLabel(lbs: Boolean) = if (lbs) "lb" else "kg"

fun fmtNum(v: Double, decimals: Int = 2): String {
    if (v.isNaN() || v.isInfinite() || abs(v) < 0.005) return "0"
    return BigDecimal(v).setScale(decimals, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
}

fun fmtWeight(kg: Double, lbs: Boolean, decimals: Int = 2) = "${fmtNum(toDisplayWeight(kg, lbs), decimals)} ${unitLabel(lbs)}"

/** Números grandes com separador de milhares (ex.: 12 450). */
fun fmtBig(v: Double): String = String.format(locale, "%,d", v.roundToLong())

fun fmtVolume(kg: Double, lbs: Boolean) = "${fmtBig(toDisplayWeight(kg, lbs))} ${unitLabel(lbs)}"

/** 75 -> "1:15"; 3725 -> "1:02:05" */
fun fmtClock(totalSec: Long): String {
    val s = totalSec.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, sec) else String.format(Locale.US, "%d:%02d", m, sec)
}

fun fmtClock(totalSec: Int) = fmtClock(totalSec.toLong())

/** Duração legível: "1h 05min", "42min". */
fun fmtDuration(ms: Long): String {
    val min = (ms / 60000).coerceAtLeast(0)
    val h = min / 60
    val m = min % 60
    return if (h > 0) String.format(Locale.US, "%dh %02dmin", h, m) else "${m}min"
}

/** Formata com o padrão mais adequado à língua para o "esqueleto" dado (ex.: "dMMMyyyy"). */
fun fmtSkeleton(skeleton: String, ms: Long): String =
    SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, skeleton), locale).format(Date(ms))

fun fmtDate(ms: Long) = fmtSkeleton("dMMMyyyy", ms)
fun fmtDateShort(ms: Long) = fmtSkeleton("dMMM", ms)
fun fmtDateTime(ms: Long) = (fmtSkeleton("EEEEdMMMMyyyy", ms) + " · " + fmtSkeleton("Hm", ms)).replaceFirstChar { it.uppercase(locale) }
fun fmtMonthYear(ms: Long) = fmtSkeleton("LLLLyyyy", ms).replaceFirstChar { it.uppercase(locale) }

fun parseDecimal(s: String): Double? = s.replace(',', '.').trim().toDoubleOrNull()

/** Duração escrita só com dígitos: "90" = 90s, "130" = 1:30, "1:30" = 1:30. */
fun parseDuration(s: String): Double? {
    val d = s.filter { it.isDigit() }
    if (d.isEmpty()) return null
    if (d.length <= 2) return d.toDouble()
    val min = d.dropLast(2).toLong()
    val sec = d.takeLast(2).toLong()
    return (min * 60 + sec).toDouble()
}

fun String.normalized(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()
