package com.es.TFG.service

import com.es.Api_Rest_Segura2.error.exception.BadRequestException
import com.es.Api_Rest_Segura2.error.exception.NotFoundException
import com.es.TFG.model.Producto
import com.es.TFG.repository.ProductoRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Date

@Service
class ProductoService {
    @Autowired
    private lateinit var productoRepository: ProductoRepository

    fun insertProducto(producto: Producto): Producto {
        if (producto.articulo.isNullOrBlank() || producto.precio <= 0 || producto.stock < 0) {
            throw BadRequestException("Campos inválidos o vacíos")
        }

        val nuevoProducto = producto.copy(
            numeroProducto = null,
            fechaCreacion = Date.from(Instant.now()),
            fechaActualizacion = Date.from(Instant.now())
        )
        return productoRepository.insert(nuevoProducto)
    }

    fun findAllProductos(): List<Producto> = productoRepository.findAll()

    fun findById(id: String): Producto = productoRepository.findById(id)
        .orElseThrow { NotFoundException("Producto con id $id no encontrado") }

    fun updateProducto(id: String, productoActualizado: Producto): Producto {
        val producto = findById(id)

        producto.articulo = productoActualizado.articulo ?: producto.articulo
        producto.descripcion = productoActualizado.descripcion ?: producto.descripcion
        producto.precio = productoActualizado.precio
        producto.stock = productoActualizado.stock
        producto.imagenUrl = productoActualizado.imagenUrl ?: producto.imagenUrl
        producto.fechaActualizacion = Date.from(Instant.now())

        return productoRepository.save(producto)
    }

    fun deleteProducto(id: String) {
        if (!productoRepository.existsById(id)) {
            throw NotFoundException("Producto con id $id no encontrado")
        }
        productoRepository.deleteById(id)
    }
}
