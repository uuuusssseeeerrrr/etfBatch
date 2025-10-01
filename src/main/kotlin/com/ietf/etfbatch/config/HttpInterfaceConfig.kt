package com.ietf.etfbatch.config

import com.ietf.etfbatch.httpInf.KisInterface
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory


@Configuration
class HttpInterfaceConfig {
    @Value("\${custom.kis.key}")
    lateinit var KIS_KEY: String

    @Value("\${custom.kis.secret}")
    lateinit var KIS_SECRET: String

    @Bean
    fun kisInterface(): KisInterface {
        val restClient = RestClient.builder()
            .baseUrl("https://openapi.koreainvestment.com:9443") // 필요하다면 기본 헤더 등 설정
            .defaultHeaders({ headers ->
                headers.set("Content-Type", "application/json; charset=utf-8")
                headers.set("appkey", KIS_KEY)
                headers.set("appsecret", KIS_SECRET)
            })
            .build()

        val httpServiceProxyFactory = HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build()

        return httpServiceProxyFactory.createClient(KisInterface::class.java)
    }
}