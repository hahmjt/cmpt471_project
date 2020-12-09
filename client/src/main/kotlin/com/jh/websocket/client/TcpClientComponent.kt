package com.jh.websocket.client

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInitializer
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import org.springframework.stereotype.Component
import rawhttp.core.HttpVersion
import rawhttp.core.RawHttpHeaders
import rawhttp.core.RawHttpRequest
import rawhttp.core.RequestLine
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URI
import java.util.*
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy


@Component
class TcpClientComponent {
    val group: EventLoopGroup = NioEventLoopGroup()

    @PostConstruct
    fun postConstruct() {
        try {
            val clientBootstrap = Bootstrap()
            clientBootstrap.group(group)
            clientBootstrap.channel(NioSocketChannel::class.java)
            clientBootstrap.handler(object : ChannelInitializer<SocketChannel>() {
                @Throws(Exception::class)
                override fun initChannel(socketChannel: SocketChannel) {
                    socketChannel.pipeline().addLast(WebSocketClientHandler())
                }
            })

            clientBootstrap.connect("localhost", 9999).sync()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    @PreDestroy
    fun preDestroy() {


        group.shutdownGracefully(1, 10, TimeUnit.SECONDS).sync()
    }

}
