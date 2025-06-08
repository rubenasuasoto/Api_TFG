package com.es.TFG.controller



import com.es.Api_Rest_Segura2.dto.LoginUsuarioDTO
import com.es.TFG.dto.UsuarioDTO
import com.es.TFG.dto.UsuarioRegisterDTO
import com.es.TFG.error.exception.UnauthorizedException
import com.es.TFG.dto.UsuarioUpdateDTO
import com.es.TFG.service.TokenService
import com.es.TFG.service.UsuarioService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/usuarios")
class UsuarioController {

    @Autowired
    private lateinit var authenticationManager: AuthenticationManager

    @Autowired
    private lateinit var tokenService: TokenService

    @Autowired
    private lateinit var usuarioService: UsuarioService

    @PostMapping("/register")
    fun insert(
        httpRequest: HttpServletRequest,
        @RequestBody usuarioRegisterDTO: UsuarioRegisterDTO
    ): ResponseEntity<UsuarioDTO>? {


        val usuarioGuardado = usuarioService.insertUser(usuarioRegisterDTO)



        return ResponseEntity(usuarioGuardado, HttpStatus.CREATED)

    }

    @PostMapping("/login")
    fun login(@RequestBody usuario: LoginUsuarioDTO) : ResponseEntity<Any>? {

        val authentication: Authentication
        try {
            authentication = authenticationManager.authenticate(UsernamePasswordAuthenticationToken(usuario.username, usuario.password))
        } catch (e: AuthenticationException) {
            throw UnauthorizedException("Credenciales incorrectas")
        }

        // SI PASAMOS LA AUTENTICACIÓN, SIGNIFICA QUE ESTAMOS BIEN AUTENTICADOS
        // PASAMOS A GENERAR EL TOKEN
        var token = tokenService.generarToken(authentication)

        return ResponseEntity(mapOf("token" to token), HttpStatus.CREATED)
    }
    // Endpoints para usuarios autenticados (self)
    @GetMapping("/self")
    fun getSelf(authentication: Authentication): ResponseEntity<UsuarioDTO> {
        return ResponseEntity.ok(usuarioService.getUserByUsername(authentication.name))
    }

    @PutMapping("/self")
    fun updateSelf(
        authentication: Authentication,
        @RequestBody dto: UsuarioUpdateDTO
    ): ResponseEntity<UsuarioDTO> {
        return ResponseEntity.ok(usuarioService.updateUserSelf(authentication.name, dto))
    }

    @DeleteMapping("/self")
    fun deleteSelf(authentication: Authentication): ResponseEntity<Void> {
        usuarioService.deleteUserSelf(authentication.name)
        return ResponseEntity.noContent().build()
    }

    // Endpoints para admin
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun getAll(): ResponseEntity<List<UsuarioDTO>> {
        return ResponseEntity.ok(usuarioService.getAllUsers())
    }

    @GetMapping("/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    fun getByUsername(@PathVariable username: String): ResponseEntity<UsuarioDTO> {
        return ResponseEntity.ok(usuarioService.getUserByUsername(username))
    }

    @PutMapping("/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateUser(
        @PathVariable username: String,
        @RequestBody dto: UsuarioUpdateDTO
    ): ResponseEntity<UsuarioDTO> {
        return ResponseEntity.ok(usuarioService.updateUserAdmin(username, dto))
    }

    @DeleteMapping("/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteUser(@PathVariable username: String): ResponseEntity<Void> {
        usuarioService.deleteUserAdmin(username)
        return ResponseEntity.noContent().build()
    }
    @GetMapping("/check-admin")
    fun checkAdmin(authentication: Authentication): ResponseEntity<Map<String, Boolean>> {
        val isAdmin = usuarioService.isAdmin(authentication.name)
        return ResponseEntity.ok(mapOf("isAdmin" to isAdmin))
    }
}
