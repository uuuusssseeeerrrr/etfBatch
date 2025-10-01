package com.ietf.etfbatch.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class TokenCheckInterceptor : HandlerInterceptor {
    @Value("\${custom.batchToken}")
    lateinit var batchToken: String

    private val AUTH_TOKEN_HEADER = "Authorization"

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val token = request.getHeader(AUTH_TOKEN_HEADER)

        if (token == null || token.isBlank()) {
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.contentType = "application/json; charset=UTF-8"
            response.writer.write("""{"error": "Missing required header: $AUTH_TOKEN_HEADER"}""")

            return false
        } else if (!token.startsWith("Bearer ") || token.replace("Bearer", "").trim() != batchToken) {
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.contentType = "application/json; charset=UTF-8"
            response.writer.write("""{"error": "token Error: $AUTH_TOKEN_HEADER"}""")

            return false
        }

        return true
    }
}