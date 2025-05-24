package com.es.TFG.model

import com.es.Api_Rest_Segura2.model.Usuario
import org.bson.codecs.pojo.annotations.BsonId
import org.springframework.data.mongodb.core.mapping.Document
import java.util.Date

@Document("collPedidos")
data class Pedido (
    @BsonId
    val numeroPedido: String?,

    val numeroProducto: String,

    val usuario: String ,

    var articulo: String?,

    var precioFinal: Double,

    var factura: Factura
)