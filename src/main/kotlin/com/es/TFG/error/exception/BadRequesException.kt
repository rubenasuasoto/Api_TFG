package com.es.Api_Rest_Segura2.error.exception

class BadRequestException(message: String) : RuntimeException("Bad Request Exception (400). $message") {
}