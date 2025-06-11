package com.es.TFG.dto


import com.es.TFG.model.Direccion
import java.time.Instant
import java.util.Date


data class UsuarioRegisterDTO(
    val username: String,
    val email: String,
    val password: String,
    val passwordRepeat: String,
    val rol: String?,
    val fechacrea: Date = Date.from(Instant.now()),
    val direccion: Direccion
)
