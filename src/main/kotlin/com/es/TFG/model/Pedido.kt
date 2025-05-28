package com.es.TFG.model

import org.bson.codecs.pojo.annotations.BsonId
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import java.util.Date

@Document("collPedidos")
data class Pedido (

    val numeroPedido: String?,

    val numeroProducto: String,

    val usuario: String ,

    var articulo: String?,

    var precioFinal: Double,

    var factura: Factura,

    val fechaCreacion: Date = Date.from(Instant.now())
)