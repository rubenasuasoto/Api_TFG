package com.es.TFG.controller

import com.es.Api_Rest_Segura2.error.exception.UnauthorizedException
import com.es.TFG.dto.EstadoDTO
import com.es.TFG.dto.PedidoDTO
import com.es.TFG.model.Pedido
import com.es.TFG.service.PedidoService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/pedidos")
class PedidoController {

    @Autowired
    private lateinit var pedidoService: PedidoService

    @PostMapping("/self")
    fun crearPedidoSelf(
        @RequestBody dto: PedidoDTO,
        authentication: Authentication
    ): ResponseEntity<Pedido> {
        val currentUsername = authentication.name
        val pedido = pedidoService.insertPedidoSelf(dto, currentUsername)
        return ResponseEntity(pedido, HttpStatus.CREATED)
    }
    @GetMapping("/self")
    fun getMisPedidos(): ResponseEntity<List<Pedido>> {
        val username = SecurityContextHolder.getContext().authentication.name
        val pedidos = pedidoService.findPedidosByUsuario(username)
        return ResponseEntity.ok(pedidos)
    }

    @DeleteMapping("/self/{id}")
    fun deletePedidoSelf(@PathVariable id: String): ResponseEntity<Void> {
        val username = SecurityContextHolder.getContext().authentication.name
        pedidoService.deletePedidoSelf(id, username)
        return ResponseEntity.noContent().build()
    }

    // ADMIN

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun getAll(): ResponseEntity<List<Pedido>> =
        ResponseEntity.ok(pedidoService.findAll())

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun deletePedido(@PathVariable id: String): ResponseEntity<Void> {
        pedidoService.deletePedido(id)
        return ResponseEntity.noContent().build()
    }
    // Obtener pedido por ID (solo admin)
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun getPedidoById(@PathVariable id: String): ResponseEntity<Pedido> {
        val pedido = pedidoService.findById(id)
        return ResponseEntity.ok(pedido)
    }

    // Crear pedido como admin
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun createPedidoAdmin(@RequestBody pedido: Pedido): ResponseEntity<Pedido> {
        val nuevoPedido = pedidoService.insertPedidoAdmin(pedido)
        return ResponseEntity(nuevoPedido, HttpStatus.CREATED)
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun updatePedidoEstado(
        @PathVariable id: String,
        @RequestBody nuevoEstado: EstadoDTO
    ): ResponseEntity<Pedido> {
        val actualizado = pedidoService.updateEstadoPedido(id, nuevoEstado.estado)
        return ResponseEntity.ok(actualizado)
    }

}
