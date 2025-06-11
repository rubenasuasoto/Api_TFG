package com.es.TFG.service

import com.es.TFG.error.exception.BadRequestException
import com.es.TFG.error.exception.NotFoundException
import com.es.TFG.error.exception.UnauthorizedException
import com.es.TFG.dto.PedidoDTO
import com.es.TFG.dto.ProductoDTO
import com.es.TFG.model.Factura
import com.es.TFG.model.LogSistema
import com.es.TFG.model.Pedido
import com.es.TFG.model.Usuario
import com.es.TFG.repository.LogSistemaRepository
import com.es.TFG.repository.PedidoRepository
import com.es.TFG.repository.ProductoRepository
import com.es.TFG.repository.UsuarioRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.UUID
@Service
class PedidoService {

    companion object {
        // Estados
        const val ESTADO_PENDIENTE = "PENDIENTE"
        const val ESTADO_COMPLETADO = "COMPLETADO"
        const val ESTADO_CANCELADO = "CANCELADO"

        // Mensajes de error
        const val ERROR_PRODUCTO_NO_ENCONTRADO = "Producto no encontrado"
        const val ERROR_USUARIO_NO_ENCONTRADO = "Usuario no encontrado"
        const val ERROR_SIN_STOCK = "Producto sin stock disponible"
        const val ERROR_PEDIDO_NO_ENCONTRADO = "Pedido no encontrado"
        const val ERROR_NO_AUTORIZADO = "No tienes permiso para esta acción"
        const val ERROR_PLAZO_CANCELACION = "No se puede cancelar: plazo de 3 días expirado"
        const val ERROR_ESTADO_NO_VALIDO = "Estado no válido. Use: PENDIENTE, COMPLETADO o CANCELADO"
        private val log = LoggerFactory.getLogger(PedidoService::class.java)
    }

    @Autowired
    private lateinit var pedidoRepository: PedidoRepository
    @Autowired
    private lateinit var productoRepository: ProductoRepository
    @Autowired
    private lateinit var usuarioRepository: UsuarioRepository
    @Autowired
    private lateinit var logSistemaRepository: LogSistemaRepository
    @Autowired
    private lateinit var emailService: EmailService

    fun insertPedidoSelf(dto: PedidoDTO, username: String): Pedido {
        log.info("🛒 Iniciando creación de pedido con múltiples productos para usuario: $username")

        try {
            val usuario = usuarioRepository.findByUsername(username)
                .orElseThrow {
                    log.warn("Usuario no encontrado: $username")
                    NotFoundException(ERROR_USUARIO_NO_ENCONTRADO)
                }

            if (dto.productos.isEmpty()) {
                log.warn("Intento de crear pedido sin productos")
                throw BadRequestException("Debe incluir al menos un producto")
            }

            val productos = mutableListOf<ProductoDTO>()
            val productosUnicos = dto.productos.toSet()

            productosUnicos.forEach { numeroProducto ->
                val producto = productoRepository.findProductosBynumeroProducto(numeroProducto)
                    .orElseThrow {
                        log.warn("Producto no encontrado: $numeroProducto")
                        NotFoundException("$ERROR_PRODUCTO_NO_ENCONTRADO ($numeroProducto)")
                    }

                if (producto.stock <= 0) {
                    log.warn("Producto sin stock: ${producto.numeroProducto}")
                    throw BadRequestException("$ERROR_SIN_STOCK (${producto.numeroProducto})")
                }

                producto.stock -= 1
                producto.fechaActualizacion = Date.from(Instant.now())
                productoRepository.save(producto)
                log.info("📦 Stock actualizado para ${producto.numeroProducto}: ${producto.stock}")

                productos.add(
                    ProductoDTO(
                        numeroProducto = producto.numeroProducto,
                        articulo = producto.articulo ?: "Sin nombre",
                        precio = producto.precio
                    )
                )
            }

            val pedido = Pedido(
                numeroPedido = UUID.randomUUID().toString(),
                productos = dto.productos,
                usuario = username,
                detalles = productos,
                precioFinal = dto.precioFinal,
                factura = Factura(numeroFactura = UUID.randomUUID().toString()),
                estado = ESTADO_PENDIENTE
            )

            val pedidoGuardado = pedidoRepository.insert(pedido)
            log.info("✅ Pedido guardado con éxito: ${pedidoGuardado.numeroPedido}")

            registrarAccion(pedidoGuardado, usuario, "PEDIDO CREADO (SELF)")

            return pedidoGuardado

        } catch (ex: Exception) {
            log.error("❌ Error al crear pedido de múltiples productos para $username: ${ex.message}", ex)
            throw ex
        }
    }


    fun insertPedidoAdmin(pedido: Pedido): Pedido {
        log.info("🛒 Iniciando creación de pedido admin para usuario: ${pedido.usuario}")

        try {
            val usuario = usuarioRepository.findByUsername(pedido.usuario)
                .orElseThrow {
                    log.warn("Usuario no encontrado: ${pedido.usuario}")
                    NotFoundException(ERROR_USUARIO_NO_ENCONTRADO)
                }

            if (pedido.productos.isEmpty()) {
                log.warn("Pedido admin con productos vacíos")
                throw BadRequestException("Debe incluir al menos un producto")
            }

            val productos = mutableListOf<ProductoDTO>()
            var precioTotal = 0.0

            pedido.productos.toSet().forEach { numeroProducto ->
                val producto = productoRepository.findProductosBynumeroProducto(numeroProducto)
                    .orElseThrow {
                        log.warn("Producto no encontrado: $numeroProducto")
                        NotFoundException("$ERROR_PRODUCTO_NO_ENCONTRADO ($numeroProducto)")
                    }

                if (producto.stock <= 0) {
                    log.warn("Intento admin de pedido sin stock: $numeroProducto")
                    throw BadRequestException("$ERROR_SIN_STOCK ($numeroProducto)")
                }

                producto.stock -= 1
                producto.fechaActualizacion = Date.from(Instant.now())
                productoRepository.save(producto)

                productos.add(
                    ProductoDTO(
                        numeroProducto = producto.numeroProducto,
                        articulo = producto.articulo ?: "Sin nombre",
                        precio = producto.precio
                    )
                )

                precioTotal += producto.precio
                log.debug("Stock actualizado admin para $numeroProducto → ${producto.stock}")
            }

            val nuevoPedido = pedido.copy(
                numeroPedido = UUID.randomUUID().toString(),
                detalles = productos,
                precioFinal = precioTotal,
                factura = Factura(
                    numeroFactura = UUID.randomUUID().toString(),
                    fecha = Date.from(Instant.now())
                ),
                estado = ESTADO_PENDIENTE,
                fechaCreacion = Date.from(Instant.now())
            )

            val pedidoGuardado = pedidoRepository.insert(nuevoPedido)
            log.info("✅ Pedido admin creado con éxito. ID: ${pedidoGuardado.numeroPedido}")

            registrarAccion(pedidoGuardado, usuario, "PEDIDO CREADO (ADMIN)")

            return pedidoGuardado

        } catch (ex: Exception) {
            log.error("❌ Error al crear pedido admin: ${ex.message}", ex)
            throw ex
        }
    }

    fun updateEstadoPedido(numeroPedido: String, nuevoEstado: String, usernameActual: String): Pedido {
        log.info("Actualizando estado de pedido. Nº Pedido: $numeroPedido, Nuevo estado: $nuevoEstado, Usuario: $usernameActual")

        try {
            validateEstado(nuevoEstado)

            val pedido = pedidoRepository.findByNumeroPedido(numeroPedido)
                .orElseThrow {
                    log.warn("Pedido no encontrado: $numeroPedido")
                    NotFoundException(ERROR_PEDIDO_NO_ENCONTRADO)
                }

            if (pedido.usuario != usernameActual && !esAdmin(usernameActual)) {
                log.warn("Intento no autorizado. Usuario: $usernameActual, Dueño: ${pedido.usuario}")
                throw UnauthorizedException(ERROR_NO_AUTORIZADO)
            }

            pedido.estado = nuevoEstado
            val actualizado = pedidoRepository.save(pedido)
            log.info("✅ Estado actualizado. Pedido: $numeroPedido → $nuevoEstado")

            return actualizado

        } catch (ex: Exception) {
            log.error("❌ Error al actualizar estado $numeroPedido: ${ex.message}", ex)
            throw ex
        }
    }



    fun deletePedidoSelf(numeroPedido: String, username: String) {
        log.info("Iniciando cancelación de pedido self. Nº Pedido: $numeroPedido, Usuario: $username")

        try {
            val pedido = pedidoRepository.findByNumeroPedido(numeroPedido)
                .orElseThrow {
                    log.warn("Pedido no encontrado para cancelación: $numeroPedido")
                    NotFoundException(ERROR_PEDIDO_NO_ENCONTRADO)
                }

            if (pedido.usuario != username) {
                log.warn("Intento no autorizado. Usuario: $username, Dueño: ${pedido.usuario}")
                throw UnauthorizedException(ERROR_NO_AUTORIZADO)
            }

            verificarPlazoCancelacion(pedido.fechaCreacion)
            revertirPedido(pedido)
            pedidoRepository.delete(pedido)

            log.info("✅ Pedido cancelado exitosamente. Nº Pedido: $numeroPedido")
            registrarAccion(pedido, null, "PEDIDO CANCELADO (SELF)")

        } catch (ex: Exception) {
            log.error("❌ Error al cancelar pedido $numeroPedido. ${ex.message}", ex)
            throw ex
        }
    }

    fun findAll(): List<Pedido> {
        log.debug("Obteniendo todos los pedidos")
        return pedidoRepository.findAll().also {
            log.debug("Encontrados ${it.size} pedidos")
        }
    }

    fun findPedidosByUsuario(username: String): List<Pedido> {
        log.debug("Buscando pedidos para el usuario: $username")
        validateUsername(username)
        return pedidoRepository.findPedidosByUsuario(username).also {
            log.debug("Encontrados ${it.size} pedidos para el usuario $username")
        }
    }

    fun findById(id: String): Pedido {
        log.debug("Buscando pedido por ID: $id")
        return pedidoRepository.findById(id)
            .orElseThrow {
                log.warn("Pedido no encontrado: $id")
                NotFoundException(ERROR_PEDIDO_NO_ENCONTRADO)
            }
            .also {
                log.debug("Pedido encontrado: ${it.numeroPedido}")
            }
    }

    fun deletePedido(numeroPedido: String) {
        log.info("Eliminando pedido (admin). Nº Pedido: $numeroPedido")

        try {
            val pedido = pedidoRepository.findByNumeroPedido(numeroPedido)
                .orElseThrow {
                    log.warn("Pedido no encontrado: $numeroPedido")
                    NotFoundException(ERROR_PEDIDO_NO_ENCONTRADO)
                }

            revertirPedido(pedido)
            pedidoRepository.delete(pedido)

            log.info("✅ Pedido eliminado por admin. Nº Pedido: $numeroPedido")

        } catch (ex: Exception) {
            log.error("❌ Error al eliminar pedido $numeroPedido: ${ex.message}", ex)
            throw ex
        }
    }


    // --- Métodos auxiliares mejorados ---

    private fun validateUsername(username: String) {
        if (username.isBlank()) {
            log.warn("Nombre de usuario vacío")
            throw IllegalArgumentException("Nombre de usuario no puede estar vacío")
        }
    }

    private fun esAdmin(username: String): Boolean {
        log.debug("Verificando si usuario es admin: $username")
        return usuarioRepository.findByUsername(username)
            .orElseThrow {
                log.warn("Usuario no encontrado al verificar admin: $username")
                NotFoundException(ERROR_USUARIO_NO_ENCONTRADO)
            }
            .roles!!.contains("ADMIN")
            .also { isAdmin ->
                if (isAdmin) log.debug("Usuario $username es admin")
                else log.debug("Usuario $username NO es admin")
            }
    }

    private fun validateEstado(estado: String) {
        log.debug("Validando estado: $estado")
        val estadosValidos = listOf(ESTADO_PENDIENTE, ESTADO_COMPLETADO, ESTADO_CANCELADO)
        if (!estadosValidos.contains(estado)) {
            log.warn("Estado no válido: $estado")
            throw BadRequestException(ERROR_ESTADO_NO_VALIDO)
        }
    }

    private fun revertirPedido(pedido: Pedido) {
        log.info("↩️ Revirtiendo stock del pedido. ID: ${pedido.numeroPedido}")
        try {
            pedido.productos.toSet().forEach { numeroProducto ->
                val producto = productoRepository.findProductosBynumeroProducto(numeroProducto)
                    .orElseThrow {
                        log.error("❌ Producto no encontrado al revertir: $numeroProducto")
                        NotFoundException("$ERROR_PRODUCTO_NO_ENCONTRADO ($numeroProducto)")
                    }

                producto.stock += 1
                producto.fechaActualizacion = Date.from(Instant.now())
                productoRepository.save(producto)

                log.info("✅ Stock revertido para $numeroProducto → nuevo stock: ${producto.stock}")
            }
        } catch (ex: Exception) {
            log.error("❌ Error al revertir productos del pedido ${pedido.numeroPedido}", ex)
            throw ex
        }
    }


    private fun verificarPlazoCancelacion(fechaCreacion: Date) {
        log.debug("Verificando plazo de cancelación. Fecha creación: $fechaCreacion")
        val tresDiasEnMilis = 3 * 24 * 60 * 60 * 1000L
        if (Date().time - fechaCreacion.time > tresDiasEnMilis) {
            log.warn("Plazo de cancelación excedido")
            throw BadRequestException(ERROR_PLAZO_CANCELACION)
        }
    }

    private fun registrarAccion(pedido: Pedido, usuario: Usuario?, accion: String) {
        log.debug("Registrando acción: $accion para pedido ${pedido.numeroPedido}")
        try {
            logSistemaRepository.save(
                LogSistema(
                    usuario = usuario?.username ?: "SISTEMA",
                    accion = accion,
                    referencia = pedido.numeroPedido ?: "SIN_ID",
                    fecha = Date.from(Instant.now())
                )
            )
            usuario?.email?.let { email ->
                emailService.enviarConfirmacionPedido(email, pedido)
            }
        } catch (ex: Exception) {
            log.error("Error al registrar acción: ${ex.message}", ex)
        }
    }
}