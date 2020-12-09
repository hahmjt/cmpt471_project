package com.jh.websocket.server

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.*
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy


@Service
class BroadcastService(private val broadcastComponent: BroadcastComponent) {

    @PostConstruct
    fun postConstruct() {
        broadcastComponent.listen()
    }

    @Scheduled(fixedRate = 30000L)
    fun generateData(){
        val uuid = UUID.randomUUID().toString()

        //Broadcast to let other servers know and add it to their datastore
        broadcast(uuid, InetAddress.getByName("255.255.255.255"))
        DataStore.set.add(uuid)
    }


    fun broadcast(
            broadcastMessage: String, address: InetAddress?) {

        println("Broadcasting New Data")

        var socket = DatagramSocket()
        socket.broadcast = true
        val buffer = broadcastMessage.toByteArray()
        val packet = DatagramPacket(buffer, buffer.size, address, 15000)
        socket.send(packet)
        socket.close()
    }


    @PreDestroy
    fun preDestroy() {
        broadcastComponent.stop()
    }
}
