package com.es.TFG.dto

import com.es.TFG.model.Direccion

data class UsuarioUpdateDTO(
    val currentPassword: String?, // Solo necesario para cambios de contraseña en self
    val newPassword: String?,
    val email: String?,
    val rol: String?, // Solo editable por admin
    val direccion: Direccion?
)