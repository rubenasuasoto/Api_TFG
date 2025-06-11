package com.es.TFG.dto

data class PedidoDTO(
    val productos: List<String> = emptyList(), // lista de códigos de producto
    val precioFinal: Double
)
