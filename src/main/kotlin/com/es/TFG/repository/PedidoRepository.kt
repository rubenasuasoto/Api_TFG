package com.es.TFG.repository

import com.es.TFG.model.Pedido
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface PedidoRepository : MongoRepository<Pedido, String> {
    fun findPedidosByUsuario(usuario: String): List<Pedido>
    fun findByNumeroPedido(numeroPedido: String): Optional<Pedido>

}
