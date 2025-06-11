package com.es.TFG.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class TokenService {
    @Autowired
    private lateinit var jwtEncoder: JwtEncoder

    fun generarToken(authentication: Authentication): String {
        val roles = authentication.authorities
            .joinToString(" ") { it.authority }

        val claims = JwtClaimsSet.builder()
            .issuer("self")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
            .subject(authentication.name)
            .claim("roles", roles) // Asegúrate que los roles están incluidos
            .build()

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).tokenValue
    }
}