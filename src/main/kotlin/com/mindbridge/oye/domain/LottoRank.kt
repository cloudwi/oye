package com.mindbridge.oye.domain

enum class LottoRank(val matchCount: Int, val description: String) {
    FIRST(6, "1등"),
    SECOND(5, "2등"),
    THIRD(5, "3등"),
    FOURTH(4, "4등"),
    FIFTH(3, "5등")
}
