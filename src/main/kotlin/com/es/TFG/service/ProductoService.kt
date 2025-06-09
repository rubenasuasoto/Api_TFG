package com.es.TFG.service

import com.es.TFG.error.exception.BadRequestException
import com.es.TFG.error.exception.NotFoundException
import com.es.TFG.model.Producto
import com.es.TFG.repository.ProductoRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Date
@Service
class ProductoService {

    companion object {
        private val log = LoggerFactory.getLogger(ProductoService::class.java)

        // Mensajes de error
        const val ERROR_CAMPOS_INVALIDOS = "Campos inválidos o vacíos"
        const val ERROR_ARTICULO_VACIO = "El artículo no puede estar vacío"
        const val ERROR_PRECIO_INVALIDO = "El precio debe ser mayor que 0"
        const val ERROR_STOCK_INVALIDO = "El stock no puede ser negativo"
        const val ERROR_PRODUCTO_NO_ENCONTRADO = "Producto no encontrado"
        const val ERROR_NUMERO_PRODUCTO_VACIO = "El número de producto no puede estar vacío"
        const val ERROR_NUMERO_PRODUCTO_DUPLICADO = "Ya existe un producto con este número"
    }

    @Autowired
    private lateinit var productoRepository: ProductoRepository

    fun insertProducto(producto: Producto): Producto {
        log.info("Intentando insertar nuevo producto: ${producto.numeroProducto}")

        try {
            // Validaciones básicas
            validateProducto(producto)

            // Validar unicidad del número de producto
            if (productoRepository.existsByNumeroProducto(producto.numeroProducto)) {
                log.warn("Intento de insertar producto duplicado: ${producto.numeroProducto}")
                throw BadRequestException(ERROR_NUMERO_PRODUCTO_DUPLICADO)
            }

            val nuevoProducto = producto.copy(
                id = null,
                fechaCreacion = Date.from(Instant.now()),
                fechaActualizacion = Date.from(Instant.now())
            )

            val productoGuardado = productoRepository.insert(nuevoProducto)
            log.info("Producto insertado correctamente: ${productoGuardado.numeroProducto}")

            return productoGuardado

        } catch (ex: Exception) {
            log.error("Error al insertar producto: ${ex.message}", ex)
            throw ex
        }
    }

    fun findAllProductos(): List<Producto> {
        log.debug("Obteniendo todos los productos")
        return productoRepository.findAll().also {
            log.debug("Encontrados ${it.size} productos")
        }
    }

    fun findProductosBynumeroProducto(numeroProducto: String): Producto {
        log.debug("Buscando producto por número: $numeroProducto")

        require(numeroProducto.isNotBlank()) { ERROR_NUMERO_PRODUCTO_VACIO }

        return productoRepository.findProductosBynumeroProducto(numeroProducto)
            .orElseThrow {
                log.warn("Producto no encontrado: $numeroProducto")
                NotFoundException(ERROR_PRODUCTO_NO_ENCONTRADO)
            }
            .also {
                log.debug("Producto encontrado: ${it.numeroProducto}")
            }
    }

    fun updateProducto(numeroProducto: String, productoActualizado: Producto): Producto {
        log.info("Actualizando producto: $numeroProducto")

        try {
            require(numeroProducto.isNotBlank()) { ERROR_NUMERO_PRODUCTO_VACIO }
            validateProducto(productoActualizado)

            val productoExistente = productoRepository.findProductosBynumeroProducto(numeroProducto)
                .orElseThrow {
                    log.warn("Producto a actualizar no encontrado: $numeroProducto")
                    NotFoundException(ERROR_PRODUCTO_NO_ENCONTRADO)
                }


            val productoActualizado = productoExistente.copy(
                articulo = productoActualizado.articulo ?: productoExistente.articulo,
                descripcion = productoActualizado.descripcion ?: productoExistente.descripcion,
                precio = productoActualizado.precio,
                stock = productoActualizado.stock,
                imagenUrl = productoActualizado.imagenUrl ?: productoExistente.imagenUrl,
                fechaActualizacion = Date.from(Instant.now())
            )

            return productoRepository.save(productoActualizado).also {
                log.info("Producto actualizado correctamente: ${it.numeroProducto}")
            }

        } catch (ex: Exception) {
            log.error("Error al actualizar producto $numeroProducto: ${ex.message}", ex)
            throw ex
        }
    }

    fun deleteProducto(numeroProducto: String) {
        log.info("Eliminando producto: $numeroProducto")

        try {
            require(numeroProducto.isNotBlank()) { ERROR_NUMERO_PRODUCTO_VACIO }

            if (!productoRepository.existsByNumeroProducto(numeroProducto)) {
                log.warn("Producto a eliminar no encontrado: $numeroProducto")
                throw NotFoundException(ERROR_PRODUCTO_NO_ENCONTRADO)
            }

            val deletedCount = productoRepository.deleteByNumeroProducto(numeroProducto)

            if (deletedCount > 0) {
                log.info("Producto eliminado: $numeroProducto")
            } else {
                log.warn("No se eliminó ningún producto: $numeroProducto")
            }

        } catch (ex: Exception) {
            log.error("Error al eliminar producto $numeroProducto: ${ex.message}", ex)
            throw ex
        }
    }

    // --- Métodos de validación privados ---

    private fun validateProducto(producto: Producto) {
        require(!producto.articulo.isNullOrBlank()) { ERROR_ARTICULO_VACIO }
        require(producto.precio > 0) { ERROR_PRECIO_INVALIDO }
        require(producto.stock >= 0) { ERROR_STOCK_INVALIDO }
        require(producto.numeroProducto.isNotBlank()) { ERROR_NUMERO_PRODUCTO_VACIO }
    }
}