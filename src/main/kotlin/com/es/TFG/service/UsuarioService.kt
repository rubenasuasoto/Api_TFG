package com.es.TFG.service


import com.es.TFG.dto.UsuarioDTO
import com.es.TFG.dto.UsuarioRegisterDTO
import com.es.TFG.dto.UsuarioUpdateDTO
import com.es.TFG.error.exception.BadRequestException
import com.es.TFG.error.exception.NotFoundException
import com.es.TFG.error.exception.UnauthorizedException
import com.es.TFG.model.Usuario
import com.es.TFG.repository.UsuarioRepository
import org.slf4j.LoggerFactory
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
    @Autowired
    private lateinit var externalApiService: ExternalApiService

    companion object {
        // Constantes para validaciones
        private const val MIN_PASSWORD_LENGTH = 8
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
        private val USERNAME_REGEX = Regex("^[a-zA-Z0-9._-]{3,20}$")


        // Mensajes de error
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
        const val ERROR_NO_AUTORIZADO = "No autorizado para esta acción"

        private val log = LoggerFactory.getLogger(UsuarioService::class.java)
    }

    override fun loadUserByUsername(username: String): UserDetails {
        log.debug("Cargando usuario por username: $username")
        require(username.isNotBlank()) { ERROR_USERNAME_VACIO }

        val usuario = usuarioRepository.findByUsername(username)
            .orElseThrow {
                log.warn("Usuario no encontrado: $username")
                UnauthorizedException(ERROR_USUARIO_NO_ENCONTRADO)
            }

        return User.builder()
            .username(usuario.username)
            .password(usuario.password)
            .roles(usuario.roles)
            .build()
    }

    // Métodos de registro y autenticación
    fun insertUser(usuarioInsertadoDTO: UsuarioRegisterDTO): UsuarioDTO {
        log.info("Registrando nuevo usuario: ${usuarioInsertadoDTO.username}")
        try {
            validateUsuarioRegisterDTO(usuarioInsertadoDTO)
/*
            // Comprobar la provincia
            val datosProvincias = externalApiService.obtenerProvinciasDesdeApi()
            var cpro: String = ""
            if(datosProvincias != null) {
                if(datosProvincias.data != null) {
                    val provinciaEncontrada = datosProvincias.data.stream().filter {
                        it.PRO == usuarioInsertadoDTO.direccion.provincia.uppercase()
                    }.findFirst().orElseThrow {
                        BadRequestException("Provincia ${usuarioInsertadoDTO.direccion.provincia} no encontrada")
                    }
                    cpro = provinciaEncontrada.CPRO
                }
            }

            // Comprobar el municipio
            val datosMunicipios = externalApiService.obtenerMunicipiosDesdeApi(cpro)
            if(datosMunicipios != null) {
                if(datosMunicipios.data != null) {
                    datosMunicipios.data.stream().filter {
                        it.DMUN50 == usuarioInsertadoDTO.direccion.municipio.uppercase()
                    }.findFirst().orElseThrow {
                        BadRequestException("Municipio ${usuarioInsertadoDTO.direccion.municipio} incorrecto")
                    }
                }
            }
*/
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
            log.info("Usuario registrado exitosamente: ${usuario.username}")
            return toDTO(usuarioGuardado)
        } catch (ex: Exception) {
            log.error("Error al registrar usuario: ${ex.message}", ex)
            throw ex
        }
    }

    // Métodos para usuarios autenticados (self)
    fun getUserByUsername(username: String): Usuario {
        log.debug("Obteniendo usuario: $username")
        validateUsername(username)
        val usuario = usuarioRepository.findByUsername(username)
            .orElseThrow {
                log.warn("Usuario no encontrado: $username")
                NotFoundException(ERROR_USUARIO_NO_ENCONTRADO)
            }
        return usuario
    }

    fun updateUserSelf(username: String, dto: UsuarioUpdateDTO): UsuarioDTO {
        log.info("Actualizando usuario self: $username")
        try {
            validateUsername(username)
            val usuario = usuarioRepository.findByUsername(username)
                .orElseThrow {
                    log.warn("Usuario no encontrado para actualización: $username")
                    NotFoundException(ERROR_USUARIO_NO_ENCONTRADO)
                }

            validateUsuarioUpdateDTO(dto, usuario)

            val updatedUsuario = usuario.copy(
                email = dto.email?.trim()?.lowercase() ?: usuario.email,
                password = dto.newPassword?.let { passwordEncoder.encode(it) } ?: usuario.password,
                direccion = dto.direccion?: usuario.direccion
            )

            return toDTO(usuarioRepository.save(updatedUsuario)).also {
                log.info("Usuario actualizado exitosamente: $username")
            }
        } catch (ex: Exception) {
            log.error("Error al actualizar usuario $username: ${ex.message}", ex)
            throw ex
        }
    }

    fun deleteUserSelf(username: String) {
        log.info("Eliminando usuario self: $username")
        try {
            validateUsername(username)
            if (!usuarioRepository.existsByUsername(username)) {
                log.warn("Intento de eliminar usuario no existente: $username")
                throw NotFoundException(ERROR_USUARIO_NO_ENCONTRADO)
            }
            usuarioRepository.deleteByUsername(username)
            log.info("Usuario eliminado exitosamente: $username")
        } catch (ex: Exception) {
            log.error("Error al eliminar usuario $username: ${ex.message}", ex)
            throw ex
        }
    }

    // Métodos para administradores
    fun getAllUsers(): List<UsuarioDTO> {
        log.debug("Obteniendo todos los usuarios")
        return usuarioRepository.findAll().map { toDTO(it) }.also {
            log.debug("Encontrados ${it.size} usuarios")
        }
    }

    fun updateUserAdmin(username: String, dto: UsuarioUpdateDTO): UsuarioDTO {
        log.info("Actualizando usuario como admin: $username")
        try {
            validateUsername(username)
            val usuario = usuarioRepository.findByUsername(username)
                .orElseThrow {
                    log.warn("Usuario no encontrado para actualización admin: $username")
                    NotFoundException(ERROR_USUARIO_NO_ENCONTRADO)
                }

            validateUsuarioUpdateDTO(dto, usuario, true)

            val updatedUsuario = usuario.copy(
                email = dto.email?.trim()?.lowercase() ?: usuario.email,
                password = dto.newPassword?.let { passwordEncoder.encode(it) } ?: usuario.password,
                roles = dto.rol ?: usuario.roles,
                direccion = dto.direccion?: usuario.direccion
            )

            return toDTO(usuarioRepository.save(updatedUsuario)).also {
                log.info("Usuario actualizado por admin exitosamente: $username")
            }
        } catch (ex: Exception) {
            log.error("Error al actualizar usuario $username como admin: ${ex.message}", ex)
            throw ex
        }
    }

    fun deleteUserAdmin(username: String) {
        log.info("Eliminando usuario como admin: $username")
        try {
            validateUsername(username)
            if (!usuarioRepository.existsByUsername(username)) {
                log.warn("Intento de admin de eliminar usuario no existente: $username")
                throw NotFoundException(ERROR_USUARIO_NO_ENCONTRADO)
            }
            usuarioRepository.deleteByUsername(username)
            log.info("Usuario eliminado por admin exitosamente: $username")
        } catch (ex: Exception) {
            log.error("Error al eliminar usuario $username como admin: ${ex.message}", ex)
            throw ex
        }
    }

    fun isAdmin(username: String): Boolean {
        log.debug("Verificando si es admin: $username")
        return usuarioRepository.findByUsername(username)
            .orElseThrow {
                log.warn("Usuario no encontrado al verificar admin: $username")
                NotFoundException(ERROR_USUARIO_NO_ENCONTRADO)
            }
            .roles!!.contains("ADMIN").also {
                log.debug("Usuario $username es admin: $it")
            }
    }

    // Métodos de validación privados
    private fun validateUsuarioRegisterDTO(dto: UsuarioRegisterDTO) {
        require(dto.username.isNotBlank()) { ERROR_USERNAME_VACIO }
        require(dto.email.isNotBlank()) { ERROR_EMAIL_VACIO }
        require(dto.password.isNotBlank()) { ERROR_PASSWORD_VACIO }

        validateUsername(dto.username)
        validateEmail(dto.email)
        validatePassword(dto.password)

        check(!usuarioRepository.existsByUsername(dto.username)) { ERROR_USERNAME_EXISTE }
        check(!usuarioRepository.existsByEmail(dto.email)) { ERROR_EMAIL_EXISTE }
        check(dto.password == dto.passwordRepeat) { ERROR_PASSWORDS_NO_COINCIDEN }
        check(dto.rol == null || dto.rol in listOf("USER", "ADMIN")) { ERROR_ROL_NO_VALIDO }
    }

    private fun validateUsuarioUpdateDTO(dto: UsuarioUpdateDTO, usuario: Usuario, isAdmin: Boolean = false) {
        // Validación de contraseña
        if (dto.newPassword != null) {
            require(dto.currentPassword != null) { ERROR_PASSWORD_ACTUAL_REQUERIDA }
            require(passwordEncoder.matches(dto.currentPassword, usuario.password)) {
                ERROR_PASSWORD_ACTUAL_INCORRECTA
            }
            validatePassword(dto.newPassword)
        }

        // Validación de email
        dto.email?.let {
            validateEmail(it)
            check(!usuarioRepository.existsByEmailAndUsernameNot(it, usuario.username)) {
                ERROR_EMAIL_EXISTE
            }
        }

        // Validación de rol (solo para admin)
        if (isAdmin) {
            dto.rol?.let {
                check(it in listOf("USER", "ADMIN")) { ERROR_ROL_NO_VALIDO }
            }
        }
    }

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
            rol = usuario.roles,

                )
            }

    }
