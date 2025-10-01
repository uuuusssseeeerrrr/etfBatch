package com.ietf.etfbatch.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(private val tokenCheckInterceptor: TokenCheckInterceptor) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        // 모든 경로("/**")에 인터셉터를 등록합니다.
        registry.addInterceptor(tokenCheckInterceptor)
            .addPathPatterns("/**") // 모든 경로에 적용
    }
}