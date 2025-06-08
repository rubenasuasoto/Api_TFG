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


    override fun loadUserByUsername(username: String?): UserDetails {
        val usuario: Usuario = usuarioRepository
            .findByUsername(username!!)
            .orElseThrow {
                UnauthorizedException("$username no existente")
            }

        return User.builder()
            .username(usuario.username)
            .password(usuario.password)
            .roles(usuario.roles)
            .build()
    }

    fun insertUser(usuarioInsertadoDTO: UsuarioRegisterDTO) : UsuarioDTO? {
        // comprobar que ningun campo esta vacio
        if (usuarioInsertadoDTO.username.isBlank() ||
            usuarioInsertadoDTO.email.isBlank() ||
            usuarioInsertadoDTO.password.isBlank() ||
            usuarioInsertadoDTO.passwordRepeat.isBlank()) {
            throw BadRequestException("uno o mas campos vacios")
        }
        // comprobar que no existe el nombre del usuario
        if(usuarioRepository.findByUsername(usuarioInsertadoDTO.username).isPresent) {
            throw ConflictException("Usuario ${usuarioInsertadoDTO.username} ya está registrado")
        }

        // comprobar que ambas passwords sean iguales
        if(usuarioInsertadoDTO.password != usuarioInsertadoDTO.passwordRepeat) {
            throw BadRequestException("Las contrasenias no coinciden")
        }

        // Comprobar el ROL
        if(usuarioInsertadoDTO.rol != null && usuarioInsertadoDTO.rol != "USER" && usuarioInsertadoDTO.rol != "ADMIN" ) {
            throw BadRequestException("ROL: ${usuarioInsertadoDTO.rol} incorrecto")
        }
        //Comprobar el email
        if(usuarioRepository.findByEmail(usuarioInsertadoDTO.email).isPresent) {
            throw ConflictException("El email ${usuarioInsertadoDTO.email} ya está registrado")
        }

        val usuario = Usuario(
            null,
            username = usuarioInsertadoDTO.username,
            email = usuarioInsertadoDTO.email,
            password = passwordEncoder.encode(usuarioInsertadoDTO.password),
            roles = usuarioInsertadoDTO.rol,
            fechacrea = Date.from(Instant.now()),
            direccion = usuarioInsertadoDTO.direccion



        )
        usuarioRepository.insert(usuario)

      return UsuarioDTO(
          email = usuario.email,
          username = usuario.username,
          rol = usuario.roles
      )





    }

    // Métodos self
    fun getUserByUsername(username: String): UsuarioDTO {
        val usuario = usuarioRepository.findByUsername(username)
            .orElseThrow { NotFoundException("Usuario no encontrado") }

        return toDTO(usuario)
    }

    fun updateUserSelf(username: String, dto: UsuarioUpdateDTO): UsuarioDTO {
        val usuario = usuarioRepository.findByUsername(username)
            .orElseThrow { NotFoundException("Usuario no encontrado") }

        // Validar contraseña actual si se quiere cambiar la contraseña
        if (dto.newPassword != null) {
            if (!passwordEncoder.matches(dto.currentPassword, usuario.password)) {
                throw BadRequestException("Contraseña actual incorrecta")
            }
        }

        val updatedUsuario = usuario.copy(
            email = dto.email ?: usuario.email,
            password = dto.newPassword?.let { passwordEncoder.encode(it) } ?: usuario.password,
            direccion = dto.direccion ?: usuario.direccion
        )

        return toDTO(usuarioRepository.save(updatedUsuario))
    }

    fun deleteUserSelf(username: String) {
        usuarioRepository.deleteByUsername(username)
    }

    // Métodos admin
    fun getAllUsers(): List<UsuarioDTO> {
        return usuarioRepository.findAll().map { toDTO(it) }
    }

    fun updateUserAdmin(username: String, dto: UsuarioUpdateDTO): UsuarioDTO {
        val usuario = usuarioRepository.findByUsername(username)
            .orElseThrow { NotFoundException("Usuario no encontrado") }

        val updatedUsuario = usuario.copy(
            email = dto.email ?: usuario.email,
            password = dto.newPassword?.let { passwordEncoder.encode(it) } ?: usuario.password,
            roles = dto.rol ?: usuario.roles,
            direccion = dto.direccion ?: usuario.direccion
        )

        return toDTO(usuarioRepository.save(updatedUsuario))
    }

    fun deleteUserAdmin(username: String) {
        usuarioRepository.deleteByUsername(username)
    }
    fun isAdmin(username: String): Boolean {
        val usuario = usuarioRepository.findByUsername(username)
            .orElseThrow { NotFoundException("Usuario no encontrado") }
        return usuario.roles?.contains("ADMIN") ?: false
    }

    // Método auxiliar para convertir a DTO
    private fun toDTO(usuario: Usuario): UsuarioDTO {
        return UsuarioDTO(
            username = usuario.username,
            email = usuario.email,
            rol = usuario.roles,

        )
    }
}
