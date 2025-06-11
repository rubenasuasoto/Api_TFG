package com.es.TFG.service

import com.es.TFG.model.Pedido
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
@Service
class EmailService(
    @Autowired private val javaMailSender: JavaMailSender
) {
    fun enviarConfirmacionPedido(destinatario: String, pedido: Pedido) {
        try {
            val message = javaMailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")

            helper.setFrom("tfgpruebaemail@gmail.com")
            helper.setTo(destinatario)
            helper.setSubject("🧾 Confirmación de tu pedido")

            val htmlContent = buildHtmlContenido(pedido)
            helper.setText(htmlContent, true)

            javaMailSender.send(message)
            println("✅ Correo HTML enviado a $destinatario")

        } catch (ex: Exception) {
            ex.printStackTrace()
            throw RuntimeException("❌ Error al enviar correo HTML")
        }
    }

    private fun buildHtmlContenido(pedido: Pedido): String {
        val productosHtml = pedido.detalles.joinToString(separator = "") { detalle ->
            """
            <tr>
                <td>${detalle.articulo}</td>
                <td style="text-align: right;">€${"%.2f".format(detalle.precio)}</td>
            </tr>
            """
        }

        return """
        <html>
            <body style="font-family: Arial, sans-serif; color: #333;">
                <h2>Gracias por tu pedido, ${pedido.usuario}!</h2>
                <p>Hemos recibido tu pedido correctamente.</p>
                
                <p><strong>Número de pedido:</strong> ${pedido.numeroPedido}</p>
                <p><strong>Fecha:</strong> ${pedido.fechaCreacion}</p>

                <table style="width: 100%; border-collapse: collapse; margin-top: 16px;">
                    <thead>
                        <tr>
                            <th style="text-align: left;">Producto</th>
                            <th style="text-align: right;">Precio</th>
                        </tr>
                    </thead>
                    <tbody>
                        $productosHtml
                    </tbody>
                </table>

                <p style="margin-top: 16px;"><strong>Total:</strong> €${"%.2f".format(pedido.precioFinal)}</p>
                <p><strong>Estado inicial:</strong> ${pedido.estado}</p>

                <hr>
                <p style="font-size: 0.9em;">Este es un correo automático de confirmación. No respondas a este mensaje.</p>
            </body>
        </html>
        """
    }
}
