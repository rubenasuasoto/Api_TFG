package com.es.TFG.model

import org.bson.codecs.pojo.annotations.BsonId
import org.springframework.data.mongodb.core.mapping.Document
import java.util.Date
@Document("collProductos")
data class Producto (
    @BsonId
    val NºProducto: String?,

    var Articulo: String?,

    var descripcion: String? = null,

    var precio: String,

    var stock: String ,

    val fechaCreacion: Date?,

    var fechaActualizacion: Date?
)