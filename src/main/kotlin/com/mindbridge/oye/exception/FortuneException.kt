package com.mindbridge.oye.exception

open class FortuneException(message: String) : RuntimeException(message)

class UserNotFoundException(message: String = "사용자를 찾을 수 없습니다.") : FortuneException(message)

class FortuneGenerationException(message: String = "예감 생성에 실패했습니다.") : FortuneException(message)

class UnauthorizedException(message: String = "인증이 필요합니다.") : FortuneException(message)
