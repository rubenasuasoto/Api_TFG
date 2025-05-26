package com.es.TFG.service

import com.es.TFG.model.Pedido
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

@Service
class EmailService {

    @Autowired
    private lateinit var javaMailSender: JavaMailSender

    fun enviarConfirmacionPedido(destinatario: String, pedido: Pedido) {
        val mensaje = SimpleMailMessage()
        mensaje.setTo(destinatario)
        mensaje.setSubject("Confirmación de Pedido ${pedido.numeroPedido}")
        mensaje.setText(
            """
            Hola ${pedido.usuario},
            
            Tu pedido ha sido registrado con éxito.

            Detalles:
            - Artículo: ${pedido.articulo}
            - Precio: ${pedido.precioFinal}
            - Fecha: ${pedido.factura.fecha}

            Gracias por tu compra.

            """.trimIndent()
        )
        javaMailSender.send(mensaje)
    }
}
