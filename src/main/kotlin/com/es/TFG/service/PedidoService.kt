package com.es.TFG.service

import com.es.Api_Rest_Segura2.error.exception.NotFoundException
import com.es.Api_Rest_Segura2.error.exception.UnauthorizedException
import com.es.TFG.dto.PedidoDTO
import com.es.TFG.model.Factura
import com.es.TFG.model.Pedido
import com.es.TFG.repository.PedidoRepository
import com.es.TFG.repository.ProductoRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Date
import java.util.UUID

@Service
class PedidoService {

    @Autowired
    private lateinit var pedidoRepository: PedidoRepository

    @Autowired
    private lateinit var productoRepository: ProductoRepository

    fun insertPedidoSelf(dto: PedidoDTO, username: String): Pedido {
        val producto = productoRepository.findById(dto.numeroProducto)
            .orElseThrow { NotFoundException("Producto con id ${dto.numeroProducto} no encontrado") }

        // Crear factura básica
        val factura = Factura(
            numeroFactura = UUID.randomUUID().toString(),
            fecha = Date.from(Instant.now())
        )

        val nuevoPedido = Pedido(
            numeroPedido = null,
            numeroProducto = producto.numeroProducto ?: dto.numeroProducto,
            usuario = username,
            articulo = producto.articulo,
            precioFinal = producto.precio,
            factura = factura
        )

        return pedidoRepository.insert(nuevoPedido)
    }

    fun findPedidosByUsuario(usuario: String): List<Pedido> =
        pedidoRepository.findPedidosByUsuario(usuario)

    fun findAll(): List<Pedido> = pedidoRepository.findAll()

    fun deletePedido(id: String) {
        if (!pedidoRepository.existsById(id)) {
            throw NotFoundException("Pedido con id $id no encontrado")
        }
        pedidoRepository.deleteById(id)
    }

    fun deletePedidoSelf(id: String, usuario: String) {
        val pedido = pedidoRepository.findById(id)
            .orElseThrow { NotFoundException("Pedido con id $id no encontrado") }

        if (pedido.usuario != usuario) {
            throw UnauthorizedException("No tienes permiso para eliminar este pedido")
        }

        pedidoRepository.deleteById(id)
    }
}
