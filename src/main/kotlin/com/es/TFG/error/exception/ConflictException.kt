package com.es.Api_Rest_Segura2.error.exception

class ConflictException(message: String) : RuntimeException("Conflict Exception (409). $message") {
}