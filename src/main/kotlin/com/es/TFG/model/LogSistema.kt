package com.es.TFG.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import java.util.Date

@Document("collLogs")
data class LogSistema(
    @Id val id: String? = null,
    val usuario: String,
    val accion: String,
    val referencia: String,
    val fecha: Date = Date.from(Instant.now())
)
