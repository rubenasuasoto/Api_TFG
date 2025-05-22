package com.es.Api_Rest_Segura2.error


import com.es.Api_Rest_Segura2.error.exception.BadRequestException
import com.es.Api_Rest_Segura2.error.exception.ConflictException
import com.es.Api_Rest_Segura2.error.exception.NotFoundException
import com.es.Api_Rest_Segura2.error.exception.UnauthorizedException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import java.lang.NumberFormatException
import javax.naming.AuthenticationException

@ControllerAdvice
class APIExceptionHandler {

    @ExceptionHandler(AuthenticationException::class, UnauthorizedException::class) // Las "clases" (excepciones) que se quieren controlar
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    fun handleAuthentication(request: HttpServletRequest, e: Exception) : ErrorRespuesta {
        e.printStackTrace()
        return ErrorRespuesta(e.message!!, request.requestURI)
    }

    @ExceptionHandler(Exception::class, NullPointerException::class) // Las "clases" (excepciones) que se quieren controlar
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    fun handleGeneric(request: HttpServletRequest, e: Exception) : ErrorRespuesta {
        e.printStackTrace()
        return ErrorRespuesta(e.message!!, request.requestURI)
    }
    @ExceptionHandler(
        IllegalArgumentException::class
        , NumberFormatException::class
        , BadRequestException::class) // Las "clases" (excepciones) que se quieren controlar
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    fun handleBadRequest(request: HttpServletRequest, e: Exception) : ErrorRespuesta {
        return ErrorRespuesta(e.message!!, request.requestURI)
    }
    @ExceptionHandler(ConflictException::class) // Las "clases" (excepciones) que se quieren controlar
    @ResponseStatus(HttpStatus.CONFLICT)
    @ResponseBody
    fun handleConflict(request: HttpServletRequest, e: Exception) : ErrorRespuesta {
        return ErrorRespuesta(e.message!!, request.requestURI)
    }
    @ExceptionHandler(NotFoundException::class) // Las "clases" (excepciones) que se quieren controlar
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    fun handleNotFound(request: HttpServletRequest, e: Exception) : ErrorRespuesta {
        return ErrorRespuesta(e.message!!, request.requestURI)
    }
}