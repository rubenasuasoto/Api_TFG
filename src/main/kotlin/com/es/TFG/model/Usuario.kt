package com.es.Api_Rest_Segura2.model

import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.Document
import java.util.Date

@Document("collUsuarios")
data class Usuario(
    @BsonId
    val _id : String?,
    val username: String,
    val password: String,
    val email: String,
    val roles: String? = "USER",
    val fechacrea: Date ,
    val direccion: Direccion?

)




