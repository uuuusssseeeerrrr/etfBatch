package com.ietf.etfbatch

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class EtfBatchApplication

fun main(args: Array<String>) {
    runApplication<EtfBatchApplication>(*args)
}
