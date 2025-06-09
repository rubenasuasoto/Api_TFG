package com.es.TFG.dto


import com.es.TFG.model.Direccion


data class UsuarioRegisterDTO(
    val username: String,
    val email: String,
    val password: String,
    val passwordRepeat: String,
    val rol: String?,
    val direccion: Direccion
)
