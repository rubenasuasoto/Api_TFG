package com.es.TFG.repository

import com.es.TFG.model.Producto
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ProductoRepository : MongoRepository<Producto, String> {
    fun findProductosByArticulo(articulo: String): List<Producto>
}
