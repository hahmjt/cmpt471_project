package com.jh.websocket.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean


@SpringBootApplication
class ServerApplication

fun main(args: Array<String>) {
	runApplication<ServerApplication>(*args)
}


@Bean
fun readyEventApplicationListener(): ApplicationListener<ApplicationReadyEvent?> {
	return ApplicationListener<ApplicationReadyEvent?> {
		println("######################READY")
	}
}