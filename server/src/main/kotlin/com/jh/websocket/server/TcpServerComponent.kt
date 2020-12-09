package com.jh.websocket.server

import WebSocketServerHandler
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy


@Component
class TcpServerComponent {

    val log: Logger = LoggerFactory.getLogger(this.javaClass)

    val group: EventLoopGroup = NioEventLoopGroup()

    @PostConstruct
    fun postConstruct() {
        log.debug("Start TCP Server")

        try {
            val serverBootstrap = ServerBootstrap()
            serverBootstrap.group(group)
            serverBootstrap.channel(NioServerSocketChannel::class.java)
            serverBootstrap.localAddress(InetSocketAddress("localhost", 9999))
            serverBootstrap.childHandler(object : ChannelInitializer<SocketChannel>() {
                @Throws(Exception::class)
                override fun initChannel(socketChannel: SocketChannel) {
                    socketChannel.pipeline().addLast(WebSocketServerHandler())
                }
            })
            serverBootstrap.bind().sync()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @PreDestroy
    fun preDestroy() {

        log.debug("End TCP Server")

        group.shutdownGracefully(1, 10, TimeUnit.SECONDS).sync()
    }
}
