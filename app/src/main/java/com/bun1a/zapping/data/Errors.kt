package com.bun1a.zapping.data

/** Błędy, które interfejs potrafi pokazać użytkownikowi. [code] rozróżnia je po stronie JS. */
open class ServiceException(message: String, val code: String) : Exception(message)

class NotLoggedInException : ServiceException("Zaloguj się, aby kontynuować.", "not_logged_in")

class UnauthorizedException : ServiceException("Sesja wygasła.", "unauthorized")

class LoginRejectedException(message: String) : ServiceException(message, "login_rejected")

class RequestFailedException(val status: Int, message: String) :
    ServiceException(message, "request_failed")

class StreamUnavailableException(message: String) : ServiceException(message, "stream_unavailable")
