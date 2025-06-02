package com.es.TFG.controller

import com.es.TFG.dto.PedidoDTO
import com.es.TFG.model.Pedido
import com.es.TFG.service.PedidoService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/pedidos")
class PedidoController {

    @Autowired
    private lateinit var pedidoService: PedidoService

    @PostMapping("/self")
    fun crearPedidoSelf(@RequestBody dto: PedidoDTO ): ResponseEntity<Pedido> {
        val username = SecurityContextHolder.getContext().authentication.name
        val pedido = pedidoService.insertPedidoSelf(dto, username)
        return ResponseEntity.status(HttpStatus.CREATED).body(pedido)
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
}
