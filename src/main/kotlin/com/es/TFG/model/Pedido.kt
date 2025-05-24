package com.es.TFG.model

import org.bson.codecs.pojo.annotations.BsonId
import org.springframework.data.mongodb.core.mapping.Document
import java.util.Date


@Document("collPedidos")
data class Pedido (
    @BsonId
    val NºPedido: String?,

    val NºProducto: String,

    var Articulo: String?,

    var precio_final: String,

    var factura: Factura


)