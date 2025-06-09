package com.es.TFG.service


import com.es.TFG.dto.UsuarioDTO
import com.es.TFG.dto.UsuarioRegisterDTO
import com.es.TFG.dto.UsuarioUpdateDTO
import com.es.TFG.error.exception.BadRequestException
import com.es.TFG.error.exception.ConflictException
import com.es.TFG.error.exception.NotFoundException
import com.es.TFG.error.exception.UnauthorizedException
import com.es.TFG.model.Usuario
import com.es.TFG.repository.UsuarioRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*
@Service
class UsuarioService : UserDetailsService {

    @Autowired
    private lateinit var usuarioRepository: UsuarioRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    // Constantes para validaciones
    companion object {
        // Constantes para validaciones
        private const val MIN_PASSWORD_LENGTH = 8
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\$")
        private val USERNAME_REGEX = Regex("^[a-zA-Z0-9._-]{3,20}\$")

        // Mensajes de error centralizados
        const val ERROR_USERNAME_VACIO = "El nombre de usuario no puede estar vacío"
        const val ERROR_EMAIL_VACIO = "El email no puede estar vacío"
        const val ERROR_PASSWORD_VACIO = "La contraseña no puede estar vacía"
        const val ERROR_USERNAME_EXISTE = "El nombre de usuario ya está registrado"
        const val ERROR_EMAIL_EXISTE = "El email ya está registrado"
        const val ERROR_PASSWORDS_NO_COINCIDEN = "Las contraseñas no coinciden"
        const val ERROR_ROL_NO_VALIDO = "Rol no válido. Debe ser USER o ADMIN"
        const val ERROR_FORMATO_USERNAME = "Nombre de usuario no válido. Debe tener entre 3 y 20 caracteres alfanuméricos"
        const val ERROR_FORMATO_EMAIL = "Formato de email no válido"
        const val ERROR_LONGITUD_PASSWORD = "La contraseña debe tener al menos $MIN_PASSWORD_LENGTH caracteres"
        const val ERROR_PASSWORD_NUMERO = "La contraseña debe contener al menos un número"
        const val ERROR_PASSWORD_CARACTER_ESPECIAL = "La contraseña debe contener al menos un carácter especial"
        const val ERROR_USUARIO_NO_ENCONTRADO = "Usuario no encontrado"
        const val ERROR_PASSWORD_ACTUAL_REQUERIDA = "Se requiere la contraseña actual para cambiar la contraseña"
        const val ERROR_PASSWORD_ACTUAL_INCORRECTA = "Contraseña actual incorrecta"
    }

    override fun loadUserByUsername(username: String): UserDetails {
        require(username.isNotBlank()) { ERROR_USERNAME_VACIO }

        val usuario = usuarioRepository.findByUsername(username)
            .orElseThrow { UnauthorizedException(ERROR_USUARIO_NO_ENCONTRADO) }

        return User.builder()
            .username(usuario.username)
            .password(usuario.password)
            .roles(usuario.roles)
            .build()
    }

    fun insertUser(usuarioInsertadoDTO: UsuarioRegisterDTO): UsuarioDTO {
        // Validaciones básicas
        require(usuarioInsertadoDTO.username.isNotBlank()) { ERROR_USERNAME_VACIO }
        require(usuarioInsertadoDTO.email.isNotBlank()) { ERROR_EMAIL_VACIO }
        require(usuarioInsertadoDTO.password.isNotBlank()) { ERROR_PASSWORD_VACIO }

        // Validaciones de formato
        validateUsername(usuarioInsertadoDTO.username)
        validateEmail(usuarioInsertadoDTO.email)
        validatePassword(usuarioInsertadoDTO.password)

        check(!usuarioRepository.existsByUsername(usuarioInsertadoDTO.username)) {
            ERROR_USERNAME_EXISTE
        }

        check(!usuarioRepository.existsByEmail(usuarioInsertadoDTO.email)) {
            ERROR_EMAIL_EXISTE
        }

        check(usuarioInsertadoDTO.password == usuarioInsertadoDTO.passwordRepeat) {
            ERROR_PASSWORDS_NO_COINCIDEN
        }

        check(usuarioInsertadoDTO.rol == null ||
                usuarioInsertadoDTO.rol in listOf("USER", "ADMIN")) {
            ERROR_ROL_NO_VALIDO
        }

        val usuario = Usuario(
            _id = null,
            username = usuarioInsertadoDTO.username.trim(),
            email = usuarioInsertadoDTO.email.trim().lowercase(),
            password = passwordEncoder.encode(usuarioInsertadoDTO.password),
            roles = usuarioInsertadoDTO.rol ?: "USER",
            fechacrea = Date.from(Instant.now()),
            direccion = usuarioInsertadoDTO.direccion
        )

        val usuarioGuardado = usuarioRepository.save(usuario)
        return toDTO(usuarioGuardado)
    }

    // Métodos self
    fun getUserByUsername(username: String): UsuarioDTO {
        validateUsername(username)
        val usuario = usuarioRepository.findByUsername(username)
            .orElseThrow { NotFoundException("Usuario no encontrado") }
        return toDTO(usuario)
    }

    fun updateUserSelf(username: String, dto: UsuarioUpdateDTO): UsuarioDTO {
        validateUsername(username)
        val usuario = usuarioRepository.findByUsername(username)
            .orElseThrow { NotFoundException("Usuario no encontrado") }

        // Validación de contraseña actual si se cambia la contraseña
        if (dto.newPassword != null) {
            require(dto.currentPassword != null) { ERROR_PASSWORD_ACTUAL_REQUERIDA }
            require(passwordEncoder.matches(dto.currentPassword, usuario.password)) {
                ERROR_PASSWORD_ACTUAL_INCORRECTA
            }
            validatePassword(dto.newPassword)
        }

        // Validación de email si se cambia
        dto.email?.let {
            validateEmail(it)
            check(!usuarioRepository.existsByEmailAndUsernameNot(it, username)) {
                ERROR_EMAIL_EXISTE
            }
        }
        val updatedUsuario = usuario.copy(
            email = dto.email?.trim()?.lowercase() ?: usuario.email,
            password = dto.newPassword?.let { passwordEncoder.encode(it) } ?: usuario.password,
            direccion = dto.direccion ?: usuario.direccion
        )

        return toDTO(usuarioRepository.save(updatedUsuario))
    }

    // Métodos admin
    fun updateUserAdmin(username: String, dto: UsuarioUpdateDTO): UsuarioDTO {
        validateUsername(username)
        val usuario = usuarioRepository.findByUsername(username)
            .orElseThrow { NotFoundException("Usuario no encontrado") }

        // Validaciones adicionales para admin
        dto.rol?.let {
            check(it in listOf("USER", "ADMIN")) { "Rol no válido" }
        }

        dto.email?.let { email ->
            validateEmail(email)
            check(!usuarioRepository.existsByEmailAndUsernameNot(email, username)) {
                "El email ya está registrado por otro usuario"
            }
        }

        dto.newPassword?.let { validatePassword(it) }

        val updatedUsuario = usuario.copy(
            email = dto.email?.trim()?.lowercase() ?: usuario.email,
            password = dto.newPassword?.let { passwordEncoder.encode(it) } ?: usuario.password,
            roles = dto.rol ?: usuario.roles,
            direccion = dto.direccion ?: usuario.direccion
        )

        return toDTO(usuarioRepository.save(updatedUsuario))
    }

    // Métodos de validación privados
    private fun validateUsername(username: String) {
        require(username.matches(USERNAME_REGEX)) { ERROR_FORMATO_USERNAME }
    }

    private fun validateEmail(email: String) {
        require(email.matches(EMAIL_REGEX)) { ERROR_FORMATO_EMAIL }
    }

    private fun validatePassword(password: String) {
        require(password.length >= MIN_PASSWORD_LENGTH) { ERROR_LONGITUD_PASSWORD }
        require(password.any { it.isDigit() }) { ERROR_PASSWORD_NUMERO }
        require(password.any { !it.isLetterOrDigit() }) { ERROR_PASSWORD_CARACTER_ESPECIAL }
    }

    private fun toDTO(usuario: Usuario): UsuarioDTO {
        return UsuarioDTO(
            username = usuario.username,
            email = usuario.email,
            rol = usuario.roles
        )
    }
}
