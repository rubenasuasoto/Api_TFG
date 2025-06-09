package com.es.TFG.service

import com.es.TFG.error.exception.BadRequestException
import com.es.TFG.error.exception.NotFoundException
import com.es.TFG.error.exception.UnauthorizedException
import com.es.TFG.dto.PedidoDTO
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
        log.info("Iniciando creación de pedido self para usuario: $username, producto: ${dto.numeroProducto}")

        try {
            val producto = productoRepository.findProductosBynumeroProducto(dto.numeroProducto)
                .orElseThrow {
                    log.warn("Producto no encontrado: ${dto.numeroProducto}")
                    NotFoundException(ERROR_PRODUCTO_NO_ENCONTRADO)
                }

            log.debug("Producto encontrado: ${producto.numeroProducto}, stock actual: ${producto.stock}")

            val usuario = usuarioRepository.findByUsername(username)
                .orElseThrow {
                    log.warn("Usuario no encontrado: $username")
                    NotFoundException(ERROR_USUARIO_NO_ENCONTRADO)
                }

            if (producto.stock <= 0) {
                log.warn("Intento de pedido sin stock. Producto: ${producto.numeroProducto}")
                throw BadRequestException(ERROR_SIN_STOCK)
            }

            producto.stock -= 1
            producto.fechaActualizacion = Date.from(Instant.now())
            productoRepository.save(producto)
            log.info("Stock actualizado. Nuevo stock: ${producto.stock}")

            val pedido = Pedido(
                numeroPedido = UUID.randomUUID().toString(),
                numeroProducto = producto.numeroProducto,
                usuario = username,
                articulo = producto.articulo,
                precioFinal = producto.precio,
                factura = Factura(
                    numeroFactura = UUID.randomUUID().toString(),
                    fecha = Date.from(Instant.now())
                ),
                estado = ESTADO_PENDIENTE
            )

            val pedidoGuardado = pedidoRepository.insert(pedido)
            log.info("Pedido creado exitosamente. ID: ${pedidoGuardado.numeroPedido}")

            registrarAccion(pedidoGuardado, usuario, "PEDIDO CREADO (SELF)")

            return pedidoGuardado

        } catch (ex: Exception) {
            log.error("Error al crear pedido self. Usuario: $username. Error: ${ex.message}", ex)
            throw ex
        }
    }

    fun insertPedidoAdmin(pedido: Pedido): Pedido {
        log.info("Iniciando creación de pedido admin para usuario: ${pedido.usuario}")

        try {
            val producto = productoRepository.findProductosBynumeroProducto(pedido.numeroProducto)
                .orElseThrow {
                    log.warn("Producto no encontrado: ${pedido.numeroProducto}")
                    NotFoundException(ERROR_PRODUCTO_NO_ENCONTRADO)
                }

            val usuario = usuarioRepository.findByUsername(pedido.usuario)
                .orElseThrow {
                    log.warn("Usuario no encontrado: ${pedido.usuario}")
                    NotFoundException(ERROR_USUARIO_NO_ENCONTRADO)
                }

            if (producto.stock <= 0) {
                log.warn("Intento de pedido admin sin stock. Producto: ${producto.numeroProducto}")
                throw BadRequestException(ERROR_SIN_STOCK)
            }

            producto.stock -= 1
            producto.fechaActualizacion = Date.from(Instant.now())
            productoRepository.save(producto)
            log.debug("Stock actualizado por admin. Nuevo stock: ${producto.stock}")

            val nuevoPedido = pedido.copy(
                numeroPedido = UUID.randomUUID().toString(),
                articulo = producto.articulo,
                precioFinal = producto.precio,
                factura = pedido.factura.copy(
                    numeroFactura = UUID.randomUUID().toString(),
                    fecha = Date.from(Instant.now())
                ),
                estado = ESTADO_PENDIENTE,
                fechaCreacion = Date.from(Instant.now())
            )

            val pedidoGuardado = pedidoRepository.insert(nuevoPedido)
            log.info("Pedido admin creado exitosamente. ID: ${pedidoGuardado.numeroPedido}")

            registrarAccion(pedidoGuardado, usuario, "PEDIDO CREADO (ADMIN)")

            return pedidoGuardado

        } catch (ex: Exception) {
            log.error("Error al crear pedido admin. Error: ${ex.message}", ex)
            throw ex
        }
    }
    fun updateEstadoPedido(id: String, nuevoEstado: String, usernameActual: String): Pedido {
        log.info("Actualizando estado de pedido. ID: $id, Nuevo estado: $nuevoEstado, Usuario: $usernameActual")

        try {
            validateEstado(nuevoEstado)

            val pedido = pedidoRepository.findById(id)
                .orElseThrow {
                    log.warn("Pedido no encontrado para actualización: $id")
                    NotFoundException(ERROR_PEDIDO_NO_ENCONTRADO)
                }

            if (pedido.usuario != usernameActual && !esAdmin(usernameActual)) {
                log.warn("Intento no autorizado de actualización. Usuario: $usernameActual, Dueño: ${pedido.usuario}")
                throw UnauthorizedException(ERROR_NO_AUTORIZADO)
            }

            pedido.estado = nuevoEstado
            val pedidoActualizado = pedidoRepository.save(pedido)
            log.info("Estado actualizado exitosamente. Pedido: $id, Nuevo estado: $nuevoEstado")

            return pedidoActualizado

        } catch (ex: Exception) {
            log.error("Error al actualizar estado del pedido $id. Error: ${ex.message}", ex)
            throw ex
        }
    }


    fun deletePedidoSelf(id: String, username: String) {
        log.info("Iniciando cancelación de pedido self. ID: $id, Usuario: $username")

        try {
            val pedido = pedidoRepository.findById(id)
                .orElseThrow {
                    log.warn("Pedido no encontrado para cancelación: $id")
                    NotFoundException(ERROR_PEDIDO_NO_ENCONTRADO)
                }

            if (pedido.usuario != username) {
                log.warn("Intento no autorizado de cancelación. Usuario: $username, Dueño: ${pedido.usuario}")
                throw UnauthorizedException(ERROR_NO_AUTORIZADO)
            }

            verificarPlazoCancelacion(pedido.fechaCreacion)
            revertirPedido(pedido)
            pedidoRepository.deleteById(id)
            log.info("Pedido cancelado exitosamente. ID: $id")

            registrarAccion(pedido, null, "PEDIDO CANCELADO (SELF)")

        } catch (ex: Exception) {
            log.error("Error al cancelar pedido $id. Error: ${ex.message}", ex)
            throw ex
        }
    }

    // --- Métodos auxiliares ---
    private fun esAdmin(username: String): Boolean {
        log.debug("Verificando rol admin para usuario: $username")
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
            log.warn("Estado no válido proporcionado: $estado")
            throw BadRequestException(ERROR_ESTADO_NO_VALIDO)
        }
    }

    private fun revertirPedido(pedido: Pedido) {
        log.info("Revertiendo pedido. ID: ${pedido.numeroPedido}, Producto: ${pedido.numeroProducto}")

        try {
            val producto = productoRepository.findProductosBynumeroProducto(pedido.numeroProducto)
                .orElseThrow {
                    log.error("Producto no encontrado al revertir pedido: ${pedido.numeroProducto}")
                    NotFoundException("Producto no encontrado")
                }

            producto.stock += 1
            producto.fechaActualizacion = Date.from(Instant.now())
            productoRepository.save(producto)
            log.info("Stock revertido. Producto: ${producto.numeroProducto}, Nuevo stock: ${producto.stock}")

        } catch (ex: Exception) {
            log.error("Error crítico al revertir pedido ${pedido.numeroPedido}", ex)
            throw ex
        }
    }

    private fun verificarPlazoCancelacion(fechaCreacion: Date) {
        log.debug("Verificando plazo de cancelación. Fecha creación: $fechaCreacion")
        val tresDiasEnMilis = 3 * 24 * 60 * 60 * 1000L
        if (Date().time - fechaCreacion.time > tresDiasEnMilis) {
            log.warn("Plazo de cancelación excedido. Fecha creación: $fechaCreacion")
            throw BadRequestException(ERROR_PLAZO_CANCELACION)
        }
    }

    private fun registrarAccion(pedido: Pedido, usuario: Usuario?, accion: String) {
        log.debug("Registrando acción: $accion para pedido: ${pedido.numeroPedido}")

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
                log.debug("Enviando email de confirmación a: $email")
                emailService.enviarConfirmacionPedido(email, pedido)
            }

        } catch (ex: Exception) {
            log.error("Error al registrar acción $accion para pedido ${pedido.numeroPedido}", ex)

        }
    }
}