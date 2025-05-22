package com.es.Api_Rest_Segura2.controller

import com.es.Api_Rest_Segura2.dto.TareaDTO
import com.es.Api_Rest_Segura2.model.Tarea
import com.es.Api_Rest_Segura2.service.TareaService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/tareas")
class TareaController {
    @Autowired
    private lateinit var authenticationManager: AuthenticationManager


    @Autowired
    private lateinit var tareaService: TareaService

    @PostMapping("/self")
    fun altaSelf(
        @RequestBody tareas: TareaDTO
    ): ResponseEntity<Tarea> {
        val currentUsername = SecurityContextHolder.getContext().authentication.name
        val newTarea = tareaService.insertTareaSelf(tareas, currentUsername)
        return ResponseEntity(newTarea, HttpStatus.CREATED)
    }
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun registrartarea(@RequestBody tarea: Tarea): ResponseEntity<Tarea> {
        val nuevoTarea = tareaService.insertTarea(tarea)
        return ResponseEntity.status(201).body(nuevoTarea)
    }
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun getAllTareas(): ResponseEntity<List<Tarea>> {
        val tareas = tareaService.findAllTareas()
        return ResponseEntity.ok(tareas)
    }

    @GetMapping("/self")
    fun getSelfTareas(): ResponseEntity<List<Tarea>> {
        val currentUsername = SecurityContextHolder.getContext().authentication.name
        val tareas = tareaService.findTareasByUser(currentUsername)
        return ResponseEntity.ok(tareas)
    }


    /**
     * Endpoint para actualizar una tarea por ID
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateEstado(
        @RequestBody tarea: Tarea,
        @PathVariable id: String
    ): ResponseEntity<Tarea> {
        val updatedTarea = tareaService.updateEstado(id, tarea)
        return ResponseEntity.ok(updatedTarea)
    }
    @PutMapping("/self/{id}")
    fun updateEstadoSelf(
        @RequestBody tarea: Tarea,
        @PathVariable id: String
    ): ResponseEntity<Tarea> {
        val currentUsername = SecurityContextHolder.getContext().authentication.name
        val updatedTarea = tareaService.updateEstadoSelf(id, tarea,currentUsername)
        return ResponseEntity.ok(updatedTarea)
    }

    /**
     * Endpoint para eliminar una tarea por ID
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteTarea(
        @PathVariable id: String
    ): ResponseEntity<Void> {
        tareaService.deleteTarea(id)
        return ResponseEntity.noContent().build()
    }
    @DeleteMapping("/self/{id}")
    fun deleteTareaSelf(
        @PathVariable id: String
    ): ResponseEntity<Void> {
        val currentUsername = SecurityContextHolder.getContext().authentication.name
        tareaService.deleteTareaSelf(id,currentUsername)
        return ResponseEntity.noContent().build()
    }

}