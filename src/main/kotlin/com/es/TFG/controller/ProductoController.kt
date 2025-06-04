package com.es.TFG.controller

import com.es.TFG.model.Producto
import com.es.TFG.service.ProductoService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/productos")
class ProductoController {

    @Autowired
    private lateinit var productoService: ProductoService

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun crearProducto(@RequestBody producto: Producto): ResponseEntity<Producto> {
        val nuevo = productoService.insertProducto(producto)
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo)
    }

    @GetMapping
    fun getAll(): ResponseEntity<List<Producto>> =
        ResponseEntity.ok(productoService.findAllProductos())

    @GetMapping("/{numeroProducto}")
    fun getBynumeroProducto(@PathVariable numeroProducto: String): ResponseEntity<Producto> =
        ResponseEntity.ok(productoService.findProductosBynumeroProducto(numeroProducto))

    @PutMapping("/{numeroProducto}")
    @PreAuthorize("hasRole('ADMIN')")
    fun update(
        @PathVariable numeroProducto: String,
        @RequestBody producto: Producto
    ): ResponseEntity<Producto> {
        val actualizado = productoService.updateProducto(numeroProducto, producto)
        return ResponseEntity.ok(actualizado)
    }

    @DeleteMapping("/{numeroProducto}")
    @PreAuthorize("hasRole('ADMIN')")
    fun delete(@PathVariable numeroProducto: String): ResponseEntity<Void> {
        productoService.deleteProducto(numeroProducto)
        return ResponseEntity.noContent().build()
    }
}
