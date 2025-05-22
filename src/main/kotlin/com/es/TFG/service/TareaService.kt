package com.es.Api_Rest_Segura2.service

import com.es.Api_Rest_Segura2.dto.TareaDTO
import com.es.Api_Rest_Segura2.error.exception.BadRequestException
import com.es.Api_Rest_Segura2.error.exception.NotFoundException
import com.es.Api_Rest_Segura2.error.exception.UnauthorizedException
import com.es.Api_Rest_Segura2.model.Tarea
import com.es.Api_Rest_Segura2.repository.TareaRepository
import com.es.Api_Rest_Segura2.repository.UsuarioRepository
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*
@Service
class TareaService {
    @Autowired
    private lateinit var tareaRepository: TareaRepository
    @Autowired
    private lateinit var usuarioRepository: UsuarioRepository


    fun insertTarea(tareas: Tarea): Tarea {
        // comprobar que ningun campo esta vacio
        if (tareas.titulo!!.isBlank()||
            tareas.descripcion!!.isBlank() ||
            tareas.usuario!!.isBlank()) {
            throw BadRequestException("uno o mas campos vacios")
        }
        // comprobar que  existe el nombre del usuario
        if(!usuarioRepository.findByUsername(tareas.usuario!!).isPresent) {
            throw NotFoundException("Usuario ${tareas.usuario} no existe ")
        }


        val tarea = Tarea(
            null,
            titulo = tareas.titulo,
            descripcion = tareas.descripcion,
            estado = "PENDIENTE",
            usuario = tareas.usuario ,
            fechaActualizacion =Date.from(Instant.now()) ,
            fechaCreacion =Date.from(Instant.now())



        )
        return tareaRepository.insert(tarea)


    }
    fun insertTareaSelf(tareas: TareaDTO, username: String): Tarea {

        // comprobar que ningun campo esta vacio
        if (tareas.titulo.isBlank()||
            tareas.descripcion.isBlank()
                ) {
            throw BadRequestException("uno o mas campos vacios")
        }

        val tarea = Tarea(
            null,
            titulo = tareas.titulo,
            descripcion = tareas.descripcion,
            estado = "PENDIENTE",
            usuario = username ,
            fechaActualizacion =Date.from(Instant.now()) ,
            fechaCreacion =Date.from(Instant.now())



        )
        return tareaRepository.insert(tarea)


    }
    fun findAllTareas(): List<Tarea> {

        return tareaRepository.findAll() }


    fun findTareasByUser(username: String): List<Tarea> {
        if (!usuarioRepository.existsByUsername(username)) {
            throw NotFoundException("Usuario con id $username no encontrado")
        }
        return tareaRepository.findTareasByUsuario(username)
    }
    fun updateEstado(id: String, tareaActualizada: Tarea): Tarea {

        // Comprobar el estado
        if(tareaActualizada.estado != null && tareaActualizada.estado != "PENDIENTE" && tareaActualizada.estado != "HECHA" ) {
            throw BadRequestException("Estado: ${tareaActualizada.estado} incorrecto")
        }

        val tarea = tareaRepository.findById(id)
            .orElseThrow { NotFoundException("Tarea con id $id no encontrada") }


        tarea.estado= tareaActualizada.estado
        tarea.fechaActualizacion = Date.from(Instant.now())




        return tareaRepository.save(tarea)
    }
    fun updateEstadoSelf(id: String, tareaActualizada: Tarea,usuario: String): Tarea {

        // Comprobar el estado
        if(tareaActualizada.estado != null && tareaActualizada.estado != "PENDIENTE" && tareaActualizada.estado != "HECHA" ) {
            throw BadRequestException("Estado: ${tareaActualizada.estado} incorrecto")
        }


        val tarea = tareaRepository.findById(id)
            .orElseThrow { NotFoundException("Tarea con id $id no encontrada") }

        if (tarea.usuario != usuario){
            throw UnauthorizedException("No tienes permisos para modificar esta tarea ")
        }

        tarea.estado= tareaActualizada.estado
        tarea.fechaActualizacion = Date.from(Instant.now())




        return tareaRepository.save(tarea)
    }

    fun deleteTarea(id: String) {
        if (!tareaRepository.existsById(id)) {
            throw NotFoundException("Tarea con id $id no encontrada")
        }
        tareaRepository.deleteById(id)
    }
    fun deleteTareaSelf(id: String,usuario: String) {

        val tarea = tareaRepository.findById(id)
            .orElseThrow { NotFoundException("Tarea con id $id no encontrada") }
        if (tarea.usuario != usuario){
            throw UnauthorizedException("No tienes permisos para eliminar esta tarea ")
        }
        tareaRepository.deleteById(id)
    }
    fun isUserOwner(tareaId:String, usuario: String): Boolean{
        return tareaRepository.findTareasById(tareaId).first().usuario == usuario
    }
}

