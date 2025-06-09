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
        // Validaciones iniciales
        val producto = productoRepository.findProductosBynumeroProducto(dto.numeroProducto)
            .orElseThrow { NotFoundException(ERROR_PRODUCTO_NO_ENCONTRADO) }

        val usuario = usuarioRepository.findByUsername(username)
            .orElseThrow { NotFoundException(ERROR_USUARIO_NO_ENCONTRADO) }

        // Validar stock
        if (producto.stock <= 0) {
            throw BadRequestException(ERROR_SIN_STOCK)
        }

        // Actualizar stock
        producto.stock -= 1
        producto.fechaActualizacion = Date.from(Instant.now())
        productoRepository.save(producto)

        // Crear pedido
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

        // Registrar log y enviar email
        registrarAccion(pedidoGuardado, usuario, "PEDIDO CREADO (SELF)")

        return pedidoGuardado
    }

    fun insertPedidoAdmin(pedido: Pedido): Pedido {
        // Validaciones
        val producto = productoRepository.findProductosBynumeroProducto(pedido.numeroProducto)
            .orElseThrow { NotFoundException(ERROR_PRODUCTO_NO_ENCONTRADO) }

        val usuario = usuarioRepository.findByUsername(pedido.usuario)
            .orElseThrow { NotFoundException(ERROR_USUARIO_NO_ENCONTRADO) }

        if (producto.stock <= 0) {
            throw BadRequestException(ERROR_SIN_STOCK)
        }

        // Actualizar stock
        producto.stock -= 1
        producto.fechaActualizacion = Date.from(Instant.now())
        productoRepository.save(producto)

        // Crear pedido con datos limpios
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
        registrarAccion(pedidoGuardado, usuario, "PEDIDO CREADO (ADMIN)")

        return pedidoGuardado
    }

    fun updateEstadoPedido(id: String, nuevoEstado: String, usernameActual: String): Pedido {
        validateEstado(nuevoEstado)

        val pedido = pedidoRepository.findById(id)
            .orElseThrow { NotFoundException(ERROR_PEDIDO_NO_ENCONTRADO) }

        // Validar permisos
        if (pedido.usuario != usernameActual && !esAdmin(usernameActual)) {
            throw UnauthorizedException(ERROR_NO_AUTORIZADO)
        }

        pedido.estado = nuevoEstado
        return pedidoRepository.save(pedido)
    }

    fun deletePedidoSelf(id: String, username: String) {
        val pedido = pedidoRepository.findById(id)
            .orElseThrow { NotFoundException(ERROR_PEDIDO_NO_ENCONTRADO) }

        // Validar propiedad
        if (pedido.usuario != username) {
            throw UnauthorizedException(ERROR_NO_AUTORIZADO)
        }

        verificarPlazoCancelacion(pedido.fechaCreacion)
        revertirPedido(pedido)
        pedidoRepository.deleteById(id)

        registrarAccion(pedido, null, "PEDIDO CANCELADO (SELF)")
    }

    // --- Métodos auxiliares ---

    private fun esAdmin(username: String): Boolean {
        return usuarioRepository.findByUsername(username)
            .orElseThrow { NotFoundException(ERROR_USUARIO_NO_ENCONTRADO) }
            .roles!!.contains("ADMIN")
    }

    private fun validateEstado(estado: String) {
        val estadosValidos = listOf(ESTADO_PENDIENTE, ESTADO_COMPLETADO, ESTADO_CANCELADO)
        if (!estadosValidos.contains(estado)) {
            throw BadRequestException(ERROR_ESTADO_NO_VALIDO)
        }
    }

    private fun revertirPedido(pedido: Pedido) {
        val producto = productoRepository.findProductosBynumeroProducto(pedido.numeroProducto)
            .orElseThrow { NotFoundException("Producto no encontrado") }

        producto.stock += 1
        producto.fechaActualizacion = Date.from(Instant.now())
        productoRepository.save(producto)
    }

    private fun verificarPlazoCancelacion(fechaCreacion: Date) {
        val tresDiasEnMilis = 3 * 24 * 60 * 60 * 1000L
        if (Date().time - fechaCreacion.time > tresDiasEnMilis) {
            throw BadRequestException(ERROR_PLAZO_CANCELACION)
        }
    }

    private fun registrarAccion(pedido: Pedido, usuario: Usuario?, accion: String) {
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
    }
}
