package com.es.Api_Rest_Segura2.model




import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.Document

import java.util.Date


@Document("collTareas")
data class Tarea(
    @BsonId
    val id: String?,

    var titulo: String?,

    var descripcion: String? = null,

    var estado: String ="PENDIENTE",

    var usuario: String?,

    val fechaCreacion: Date?,

    var fechaActualizacion: Date?
)


