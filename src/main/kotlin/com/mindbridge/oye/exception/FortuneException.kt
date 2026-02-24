package com.mindbridge.oye.exception

open class OyeException(message: String) : RuntimeException(message)

class UserNotFoundException(message: String = "사용자를 찾을 수 없습니다.") : OyeException(message)

class FortuneGenerationException(message: String = "예감 생성에 실패했습니다.") : OyeException(message)

class UnauthorizedException(message: String = "인증이 필요합니다.") : OyeException(message)

class TooManyRequestsException(message: String = "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.") : OyeException(message)

class InquiryNotFoundException(message: String = "문의를 찾을 수 없습니다.") : OyeException(message)

class ForbiddenException(message: String = "권한이 없습니다.") : OyeException(message)

class ConnectionNotFoundException(message: String = "연결을 찾을 수 없습니다.") : OyeException(message)

class CompatibilityGenerationException(message: String = "궁합 생성에 실패했습니다.") : OyeException(message)

class SelfConnectionException(message: String = "자기 자신과는 연결할 수 없습니다.") : OyeException(message)

class DuplicateConnectionException(message: String = "이미 연결된 사용자입니다.") : OyeException(message)

class LottoRoundNotFoundException(message: String = "로또 회차를 찾을 수 없습니다.") : OyeException(message)

class LottoAlreadyRecommendedException(message: String = "이미 해당 회차에 추천을 받았습니다.") : OyeException(message)

class LottoDrawNotAvailableException(message: String = "아직 추첨 결과를 가져올 수 없습니다.") : OyeException(message)
