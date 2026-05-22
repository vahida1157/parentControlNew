package com.vahak.mehrban.core.util

import java.time.LocalDate

object JalaliConverter {
    /**
     * Converts Jalali (Shamsi) Year/Month/Day to a standard Java LocalDate.
     */
    fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): LocalDate {
        var gy = if (jy <= 979) 621 else 1600
        var jy2 = if (jy <= 979) jy else jy - 979
        val days = 365 * jy2 + (jy2 / 33) * 8 + (jy2 % 33 + 3) / 4 + 78 + jd + if (jm < 7) (jm - 1) * 31 else (jm - 7) * 30 + 186
        gy += 400 * (days / 146097)
        var days2 = days % 146097
        if (days2 > 36524) {
            gy += 100 * (--days2 / 36524)
            days2 %= 36524
            if (days2 >= 365) days2++
        }
        gy += 4 * (days2 / 1461)
        days2 %= 1461
        if (days2 > 365) {
            gy += (days2 - 1) / 365
            days2 = (days2 - 1) % 365
        }
        var gd = days2 + 1
        val salA = intArrayOf(0, 31, if (gy % 4 == 0 && gy % 100 != 0 || gy % 400 == 0) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gm = 0
        while (gm < 13 && gd > salA[gm]) gd -= salA[gm++]
        return LocalDate.of(gy, gm, gd)
    }
}