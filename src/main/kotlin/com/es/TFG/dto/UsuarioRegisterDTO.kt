package com.es.Api_Rest_Segura2.dto


import com.es.Api_Rest_Segura2.model.Direccion


data class UsuarioRegisterDTO(
    val username: String,
    val email: String,
    val password: String,
    val passwordRepeat: String,
    val rol: String?,
    val direccion: Direccion?
)
