package com.es.Api_Rest_Segura2.error.exception

class UnauthorizedException(message: String) : Exception("Not authorized exception (401). $message") {
}