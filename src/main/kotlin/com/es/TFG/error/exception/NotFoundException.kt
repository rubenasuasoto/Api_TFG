package com.es.Api_Rest_Segura2.error.exception

class NotFoundException(message: String) : RuntimeException("Not Found Exception (404). $message") {
}