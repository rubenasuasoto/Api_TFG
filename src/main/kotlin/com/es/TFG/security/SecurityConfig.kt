package com.es.TFG.security

import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {
    @Autowired
    private lateinit var rsaKeys: RSAKeysProperties
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                auth
                    // Endpoints públicos
                    .requestMatchers(HttpMethod.POST, "/usuarios/login").permitAll()
                    .requestMatchers(HttpMethod.POST, "/usuarios/register").permitAll()
                    .requestMatchers(HttpMethod.GET, "/productos").permitAll()
                    .requestMatchers(HttpMethod.GET, "/productos/search").permitAll()
                    .requestMatchers(HttpMethod.GET, "/productos/{numeroProducto}").permitAll()
                    // Endpoints de usuario (self)
                    .requestMatchers(HttpMethod.GET, "/usuarios/self").authenticated()
                    .requestMatchers(HttpMethod.PUT, "/usuarios/self").authenticated()
                    .requestMatchers(HttpMethod.DELETE, "/usuarios/self").authenticated()

                    // Endpoints de admin para usuarios
                    .requestMatchers(HttpMethod.GET, "/usuarios").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/usuarios/{username}").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/usuarios/{username}").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/usuarios/{username}").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/usuarios/check-admin").authenticated()
                    // Endpoints de productos (admin)
                    .requestMatchers(HttpMethod.POST, "/productos").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/productos/{numeroProducto}").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/productos/{numeroProducto}").hasRole("ADMIN")

                    // Endpoints de pedidos (self)
                    .requestMatchers(HttpMethod.POST, "/pedidos/self").authenticated()
                    .requestMatchers(HttpMethod.GET, "/pedidos/self").authenticated()
                    .requestMatchers(HttpMethod.DELETE, "/pedidos/self/{numeroPedido}").authenticated()

                    // Endpoints de pedidos (admin)
                    .requestMatchers(HttpMethod.GET, "/pedidos").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.POST, "/pedidos").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/pedidos/{numeroPedido}").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/pedidos/{numeroPedido}").hasRole("ADMIN")

                    // Cualquier otra petición requiere autenticación
                    .anyRequest().authenticated()
            }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt(Customizer.withDefaults())
            }
            .build()
    }

    @Bean
    fun passwordEncoder() : PasswordEncoder{
        return BCryptPasswordEncoder()
    }
    @Bean
    fun authenticationManager(authenticationConfiguration: AuthenticationConfiguration  ): AuthenticationManager {
        return authenticationConfiguration.authenticationManager
    }
    @Bean
    fun jwtEncoder():JwtEncoder{
        val jwk: JWK = RSAKey.Builder(rsaKeys.publicKey).privateKey(rsaKeys.privateKey).build()
        val jwks : JWKSource<SecurityContext> = ImmutableJWKSet(JWKSet(jwk))
        return NimbusJwtEncoder(jwks)
    }
    @Bean
    fun jwtDecoder():JwtDecoder {
        return  NimbusJwtDecoder.withPublicKey(rsaKeys.publicKey).build()
    }
}
