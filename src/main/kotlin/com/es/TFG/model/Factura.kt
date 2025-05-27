package com.es.TFG.model

import org.bson.codecs.pojo.annotations.BsonId
import java.time.Instant
import java.util.Date

data class Factura(
    @BsonId
    val numeroFactura: String,
    val fecha: Date = Date.from(Instant.now())
)


