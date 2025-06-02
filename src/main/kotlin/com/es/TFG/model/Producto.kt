package com.es.TFG.model

import org.bson.codecs.pojo.annotations.BsonId
import org.springframework.data.mongodb.core.mapping.Document
import java.util.Date
@Document("collProductos")
data class Producto (
    @BsonId
    val id: String?,

    val numeroProducto: String,

    var articulo: String?,

    var descripcion: String? = null,

    var precio: Double,

    var stock: Int,

    var imagenUrl: String? = null,

    val fechaCreacion: Date?,

    var fechaActualizacion: Date?
)
