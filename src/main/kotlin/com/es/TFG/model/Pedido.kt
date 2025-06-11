package com.es.TFG.model

import com.es.TFG.dto.ProductoDTO
import org.bson.codecs.pojo.annotations.BsonId
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import java.util.Date

@Document("collPedidos")
data class Pedido(
    @BsonId val numeroPedido: String?,
    val productos: List<String> = emptyList(),
    val usuario: String?,
    var detalles: List<ProductoDTO> = emptyList(),
    var precioFinal: Double,
    var factura: Factura,
    var estado: String = "PENDIENTE",
    val fechaCreacion: Date = Date.from(Instant.now())
)

