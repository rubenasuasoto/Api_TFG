package com.es.TFG.service

import com.es.TFG.model.Pedido
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import com.sendgrid.*
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.Content
import com.sendgrid.helpers.mail.objects.Email

@Service
class EmailService {

    private val sendGridApiKey = System.getenv("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") // O usa @Value si prefieres

    fun enviarConfirmacionPedido(destinatario: String, pedido: Pedido) {
        val from = Email("tfgpruebaemail@gmail.com") // debe estar verificado en SendGrid
        val to = Email(destinatario)
        val subject = "Confirmación de pedido"
        val content = Content("text/plain", "Tu pedido ha sido recibido: ${pedido.numeroPedido}")
        val mail = Mail(from, subject, to, content)

        val sg = SendGrid(sendGridApiKey)
        val request = Request()

        try {
            request.method = Method.POST
            request.endpoint = "mail/send"
            request.body = mail.build()
            val response = sg.api(request)

            println("Status Code: ${response.statusCode}")
            println("Response Body: ${response.body}")
        } catch (ex: Exception) {
            ex.printStackTrace()
            throw RuntimeException("Error enviando correo con SendGrid")
        }
    }
}
