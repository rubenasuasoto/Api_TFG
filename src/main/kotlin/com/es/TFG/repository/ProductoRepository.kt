package com.es.TFG.repository

import com.es.TFG.model.Producto
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface ProductoRepository : MongoRepository<Producto, String> {
    fun findByArticuloContainingIgnoreCase(query: String): List<Producto>

    fun findProductosBynumeroProducto(numeroProducto: String): Optional<Producto>

    fun deleteByNumeroProducto(numeroProducto: String): Long
    fun existsByNumeroProducto(numeroProducto: String): Boolean
}
