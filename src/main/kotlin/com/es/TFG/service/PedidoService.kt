package com.es.TFG.service

import com.es.Api_Rest_Segura2.error.exception.BadRequestException
import com.es.Api_Rest_Segura2.error.exception.NotFoundException
import com.es.Api_Rest_Segura2.error.exception.UnauthorizedException
import com.es.TFG.dto.PedidoDTO
import com.es.TFG.model.Factura
import com.es.TFG.model.LogSistema
import com.es.TFG.model.Pedido
import com.es.TFG.repository.LogSistemaRepository
import com.es.TFG.repository.PedidoRepository
import com.es.TFG.repository.ProductoRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.UUID

@Service
class PedidoService {

    @Autowired
    private lateinit var pedidoRepository: PedidoRepository

    @Autowired
    private lateinit var productoRepository: ProductoRepository

    @Autowired
    private lateinit var logSistemaRepository: LogSistemaRepository


    fun insertPedidoSelf(dto: PedidoDTO, username: String): Pedido {
        val producto = productoRepository.findProductosBynumeroProducto(dto.numeroProducto)
            .orElseThrow { NotFoundException("Producto con id ${dto.numeroProducto} no encontrado") }

        if (producto.stock <= 0) {
            throw BadRequestException("Producto sin stock disponible")
        }

        // Descontar stock
        producto.stock -= 1
        producto.fechaActualizacion = Date.from(Instant.now())
        productoRepository.save(producto)

        // Crear factura
        val factura = Factura(
            numeroFactura = UUID.randomUUID().toString(),
            fecha = Date.from(Instant.now())
        )

        // Crear pedido sin número
        val nuevoPedido = Pedido(
            numeroPedido = UUID.randomUUID().toString(),
            numeroProducto = producto.numeroProducto,
            usuario = username,
            articulo = producto.articulo,
            precioFinal = producto.precio,
            factura = factura
        )

        val pedidoGuardado = pedidoRepository.insert(nuevoPedido)

        logSistemaRepository.save(
            LogSistema(
                usuario = username,
                accion = "CREACIÓN PEDIDO",
                referencia = pedidoGuardado.numeroPedido ?: "SIN ID"
            )
        )

        return pedidoGuardado

    }



    fun findPedidosByUsuario(usuario: String): List<Pedido> =
        pedidoRepository.findPedidosByUsuario(usuario)

    fun findAll(): List<Pedido> = pedidoRepository.findAll()

    fun deletePedido(id: String) {
        val pedido = pedidoRepository.findById(id)
            .orElseThrow { NotFoundException("Pedido con id $id no encontrado") }

        // NOTA: Admin no tiene restricción de tiempo para cancelar

        val producto = productoRepository.findProductosBynumeroProducto(pedido.numeroProducto)
            .orElseThrow { NotFoundException("Producto con id ${pedido.numeroProducto} no encontrado") }

        producto.stock += 1
        producto.fechaActualizacion = Date.from(Instant.now())
        productoRepository.save(producto)

        pedidoRepository.deleteById(id)
    }



    fun deletePedidoSelf(id: String, usuario: String) {
        val pedido = pedidoRepository.findById(id)
            .orElseThrow { NotFoundException("Pedido con id $id no encontrado") }

        if (pedido.usuario != usuario) {
            throw UnauthorizedException("No tienes permiso para eliminar este pedido")
        }

        verificarPlazoCancelacion(pedido.fechaCreacion)

        val producto = productoRepository.findProductosBynumeroProducto(pedido.numeroProducto)
            .orElseThrow { NotFoundException("Producto con id ${pedido.numeroProducto} no encontrado") }

        producto.stock += 1
        producto.fechaActualizacion = Date.from(Instant.now())
        productoRepository.save(producto)

        pedidoRepository.deleteById(id)
    }

    private fun verificarPlazoCancelacion(fechaCreacion: Date) {
        val tresDiasEnMilis = Duration.ofDays(3).toMillis()
        val ahora = Instant.now().toEpochMilli()
        val fechaPedido = fechaCreacion.toInstant().toEpochMilli()

        if (ahora - fechaPedido > tresDiasEnMilis) {
            throw BadRequestException("No se puede cancelar el pedido: plazo de 3 días expirado")
        }
    }


}


