package com.es.TFG.repository


import com.es.TFG.model.Usuario
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface UsuarioRepository : MongoRepository<Usuario, String> {

    fun findByUsername(username: String?) : Optional<Usuario>
    fun findByEmail(email: String) : Optional<Usuario>
    fun existsByUsername(username: String): Boolean
}