package com.es.TFG

import com.es.Api_Rest_Segura2.security.RSAKeysProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(RSAKeysProperties::class)
class TfgApplication

fun main(args: Array<String>) {
	runApplication<TfgApplication>(*args)
}
