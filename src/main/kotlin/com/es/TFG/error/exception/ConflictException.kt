package com.es.TFG.error.exception

class ConflictException(message: String) : RuntimeException("Conflict Exception (409). $message") {
}