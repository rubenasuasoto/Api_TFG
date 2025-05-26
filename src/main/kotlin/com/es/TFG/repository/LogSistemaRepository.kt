package com.es.TFG.repository

import com.es.TFG.model.LogSistema
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface LogSistemaRepository : MongoRepository<LogSistema, String>
