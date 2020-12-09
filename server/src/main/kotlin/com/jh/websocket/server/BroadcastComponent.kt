package com.jh.websocket.server

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.net.DatagramPacket
import java.net.DatagramSocket


@Component
class BroadcastComponent {

    private var socket: DatagramSocket? = null
    private var running = false
    private val buf = ByteArray(256)

    @Async
    fun listen() {
        println("Running UDP Server")

        socket = DatagramSocket(15000)

        running = true
        while (running) {  //Listen on UDP broadcast
            var packet = DatagramPacket(buf, buf.size)
            socket!!.receive(packet)

            println("Got new Data, Adding it to DataStore")
            DataStore.set.add(String(packet.data, 0, packet.length))
        }
        socket!!.close()
    }

    fun stop(){
        running = false

    }


}
