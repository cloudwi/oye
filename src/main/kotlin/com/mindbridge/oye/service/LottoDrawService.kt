package com.mindbridge.oye.service

import com.mindbridge.oye.domain.LottoRank
import com.mindbridge.oye.domain.LottoRound
import com.mindbridge.oye.exception.ExternalApiException
import com.mindbridge.oye.repository.LottoRecommendationRepository
import com.mindbridge.oye.repository.LottoRoundRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClient
import java.time.LocalDate

@Service
class LottoDrawService(
    private val lottoRoundRepository: LottoRoundRepository,
    private val lottoRecommendationRepository: LottoRecommendationRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val restClient = RestClient.create()

    @Transactional
    fun fetchDrawResult(round: Int): LottoRound {
        lottoRoundRepository.findByRound(round)?.let {
            log.info("회차 {}의 당첨 결과가 이미 존재합니다.", round)
            return it
        }

        log.info("동행복권 API에서 회차 {} 당첨 결과를 조회합니다.", round)

        val response = restClient.get()
            .uri("https://www.dhlottery.co.kr/common.do?method=getLottoNumber&drwNo={round}", round)
            .retrieve()
            .body(Map::class.java)
            ?: throw ExternalApiException("동행복권 API 응답이 비어있습니다. round=$round")

        val returnValue = response["returnValue"] as? String
        if (returnValue != "success") {
            throw ExternalApiException("동행복권 API 오류: returnValue=$returnValue, round=$round")
        }

        fun parseIntField(key: String): Int {
            return (response[key] as? Number)?.toInt()
                ?: throw ExternalApiException("동행복권 API 응답에서 $key 필드를 파싱할 수 없습니다. round=$round")
        }

        val drawDateStr = response["drwNoDate"] as? String
            ?: throw ExternalApiException("동행복권 API 응답에서 drwNoDate 필드를 파싱할 수 없습니다. round=$round")

        val firstPrizeAmount = (response["firstWinamnt"] as? Number)?.toLong()

        val lottoRound = LottoRound(
            round = round,
            number1 = parseIntField("drwtNo1"),
            number2 = parseIntField("drwtNo2"),
            number3 = parseIntField("drwtNo3"),
            number4 = parseIntField("drwtNo4"),
            number5 = parseIntField("drwtNo5"),
            number6 = parseIntField("drwtNo6"),
            bonusNumber = parseIntField("bnusNo"),
            drawDate = LocalDate.parse(drawDateStr),
            firstPrizeAmount = firstPrizeAmount
        )

        val saved = lottoRoundRepository.save(lottoRound)
        log.info("회차 {} 당첨 결과 저장 완료: [{}, {}, {}, {}, {}, {}] + 보너스 {}",
            round, saved.number1, saved.number2, saved.number3,
            saved.number4, saved.number5, saved.number6, saved.bonusNumber)

        return saved
    }

    @Transactional
    fun evaluateRecommendations(lottoRound: LottoRound) {
        val winningNumbers = setOf(
            lottoRound.number1, lottoRound.number2, lottoRound.number3,
            lottoRound.number4, lottoRound.number5, lottoRound.number6
        )
        val bonusNumber = lottoRound.bonusNumber

        val recommendations = lottoRecommendationRepository.findByRound(lottoRound.round)
        if (recommendations.isEmpty()) {
            log.info("회차 {}에 대한 추천 번호가 없습니다.", lottoRound.round)
            return
        }

        log.info("회차 {} 추천 번호 {}건에 대해 등수를 판정합니다.", lottoRound.round, recommendations.size)

        var winnerCount = 0

        for (recommendation in recommendations) {
            val userNumbers = setOf(
                recommendation.number1, recommendation.number2, recommendation.number3,
                recommendation.number4, recommendation.number5, recommendation.number6
            )

            val matchCount = userNumbers.intersect(winningNumbers).size
            val bonusMatch = bonusNumber in userNumbers

            recommendation.matchCount = matchCount
            recommendation.bonusMatch = bonusMatch
            recommendation.rank = determineRank(matchCount, bonusMatch)
            recommendation.evaluated = true
            recommendation.prizeAmount = determinePrizeAmount(recommendation.rank, lottoRound.firstPrizeAmount)

            if (recommendation.rank != null) {
                winnerCount++
            }
        }

        lottoRecommendationRepository.saveAll(recommendations)
        log.info("회차 {} 등수 판정 완료: 전체 {}건 중 당첨 {}건", lottoRound.round, recommendations.size, winnerCount)
    }

    private fun determineRank(matchCount: Int, bonusMatch: Boolean): LottoRank? {
        return when {
            matchCount == 6 -> LottoRank.FIRST
            matchCount == 5 && bonusMatch -> LottoRank.SECOND
            matchCount == 5 -> LottoRank.THIRD
            matchCount == 4 -> LottoRank.FOURTH
            matchCount == 3 -> LottoRank.FIFTH
            else -> null
        }
    }

    private fun determinePrizeAmount(rank: LottoRank?, firstPrizeAmount: Long?): Long? {
        return when (rank) {
            LottoRank.FIRST -> firstPrizeAmount
            LottoRank.FOURTH -> 50_000L
            LottoRank.FIFTH -> 5_000L
            else -> null
        }
    }
}
