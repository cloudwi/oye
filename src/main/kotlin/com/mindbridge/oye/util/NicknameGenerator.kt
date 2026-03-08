package com.mindbridge.oye.util

import java.security.SecureRandom

object NicknameGenerator {
    private val secureRandom = SecureRandom()

    private val ADJECTIVES = listOf(
        "행복한", "빛나는", "용감한", "따뜻한", "귀여운",
        "씩씩한", "활발한", "느긋한", "당당한", "상냥한",
        "재빠른", "조용한", "신비한", "명랑한", "겸손한",
        "유쾌한", "산뜻한", "포근한", "영리한", "기운찬",
        "듬직한", "사려깊은", "정직한", "배려하는", "지혜로운",
        "즐거운", "청량한", "다정한", "솔직한", "순수한",
        "열정적인", "찬란한", "평화로운", "자유로운", "감성적인",
        "낭만적인", "활기찬", "담대한", "소중한", "설레는"
    )

    private val NOUNS = listOf(
        "고양이", "강아지", "토끼", "여우", "곰돌이",
        "다람쥐", "펭귄", "코알라", "판다", "수달",
        "햄스터", "고슴도치", "기린", "사슴", "돌고래",
        "부엉이", "참새", "나비", "꿀벌", "별빛",
        "달빛", "햇살", "구름", "무지개", "바람",
        "이슬", "단풍", "벚꽃", "해바라기", "민들레",
        "은하수", "오로라", "노을", "새벽", "물결",
        "소나기", "눈송이", "동백꽃", "라벤더", "장미"
    )

    fun generate(): String {
        val adj = ADJECTIVES[secureRandom.nextInt(ADJECTIVES.size)]
        val noun = NOUNS[secureRandom.nextInt(NOUNS.size)]
        val number = secureRandom.nextInt(1000)
        return "${adj}${noun}${number}"
    }
}
