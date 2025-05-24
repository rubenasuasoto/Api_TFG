package com.es.TFG.model

import org.bson.codecs.pojo.annotations.BsonId
import java.util.Date

data class Factura(
    @BsonId
    val NºFactura: String,
    val fecha: Date

) {


}