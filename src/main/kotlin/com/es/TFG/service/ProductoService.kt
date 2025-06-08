package com.es.TFG.service

import com.es.TFG.error.exception.BadRequestException
import com.es.TFG.error.exception.NotFoundException
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
            numeroProducto = producto.numeroProducto,
            fechaCreacion = Date.from(Instant.now()),
            fechaActualizacion = Date.from(Instant.now())
        )
        return productoRepository.insert(nuevoProducto)
    }

    fun findAllProductos(): List<Producto> = productoRepository.findAll()

    fun findProductosBynumeroProducto(numeroProducto: String): Producto = productoRepository.findProductosBynumeroProducto(numeroProducto)
        .orElseThrow { NotFoundException("Producto con numero $numeroProducto no encontrado") }

    fun updateProducto(numeroProducto: String, productoActualizado: Producto): Producto {
        val producto = findProductosBynumeroProducto(numeroProducto)

        producto.articulo = productoActualizado.articulo ?: producto.articulo
        producto.descripcion = productoActualizado.descripcion ?: producto.descripcion
        producto.precio = productoActualizado.precio
        producto.stock = productoActualizado.stock
        producto.imagenUrl = productoActualizado.imagenUrl ?: producto.imagenUrl
        producto.fechaActualizacion = Date.from(Instant.now())

        return productoRepository.save(producto)
    }

    fun deleteProducto(numeroProducto: String) {
        productoRepository.findProductosBynumeroProducto(numeroProducto)
            .orElseThrow{ NotFoundException("Producto con numero $numeroProducto no encontrado")}

        productoRepository.deleteByNumeroProducto(numeroProducto)
    }
}
