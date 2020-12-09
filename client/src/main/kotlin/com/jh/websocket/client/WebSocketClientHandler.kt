package com.jh.websocket.client

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.util.CharsetUtil
import rawhttp.core.*
import java.net.InetAddress
import java.net.URI
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.*
import kotlin.experimental.xor

class WebSocketClientHandler : ChannelInboundHandlerAdapter() {

    private var isConnected: Boolean = false

    private var remainingFrameSize = 0


    override fun channelActive(ctx: ChannelHandlerContext?) {
        val requestLine = RequestLine("GET", URI("ws://localhost:9999"), HttpVersion.HTTP_1_1)

        val headers = RawHttpHeaders.newBuilder()
                .with("Host", "localhost:9999")
                .with("Cache-Control", "no-cache")
                .with("Pragma", "no-cache")
                .with("Accept-Language", "en-US,en;q=0.9")
                .with("Connection", "Upgrade")
                .with("Upgrade", "websocket")
                .with("Sec-WebSocket-Key", Base64.getEncoder().encodeToString("CMPT471".toByteArray()))
                .with("User-Agent", "netty")
                .with("Origin", "http://www.websocket.org")
                .with("Sec-WebSocket-Version", "13")
                .build()

        val rawHttpRequest = RawHttpRequest(requestLine, headers, null, InetAddress.getLocalHost())

        ctx?.writeAndFlush(Unpooled.copiedBuffer(rawHttpRequest.toString().toByteArray()))
    }

    @Throws(Exception::class)
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        val inBuffer = msg as ByteBuf
        val received = inBuffer.toString(CharsetUtil.UTF_8)

        if (!isConnected) {
            initiateWebSocketHandShake(ctx, received)

            ctx.writeAndFlush(getRequestFrame("Hello Server"))
            return
        }

        processHeader(inBuffer)

        while (inBuffer.isReadable) {
            val decoded = inBuffer.readByte()
            print(decoded.toChar())
            remainingFrameSize--

            if(inBuffer.isReadable && remainingFrameSize == 0){
                processHeader(inBuffer.discardReadBytes())
            }
        }

        inBuffer.discardReadBytes()
    }


    override fun channelReadComplete(ctx: ChannelHandlerContext?) {
        ctx?.flush()
    }

    private fun initiateWebSocketHandShake(ctx: ChannelHandlerContext, received: String) {
        val rawHttp = RawHttp()
        val httpResponse = rawHttp.parseResponse(received)

        if (httpResponse.headers.get("Connection").first().toLowerCase() == "upgrade" && httpResponse.headers.get("Upgrade").contains("websocket")) {
            if (httpResponse.headers.get("Sec-WebSocket-Accept").first() == Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest((Base64.getEncoder().encodeToString("CMPT471".toByteArray()) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").toByteArray()))) {
                isConnected = true
            }
        }
    }

    private fun getRequestFrame(msg: String): ByteBuf {
        val firstByte: Byte = Integer.valueOf(129).toByte()

        var secondByte: Byte = Integer.valueOf(msg.length + 128).toByte()

        var lengthBytes = byteArrayOf()

        if (msg.length > 65535) {
            secondByte = Integer.valueOf(127).toByte()

            var lengthByteArray = msg.length.toBigInteger().toByteArray()
            var paddingByteArray = ByteArray(8 - lengthByteArray.size)

            lengthBytes = lengthByteArray + paddingByteArray

        } else if (msg.length > 125) {
            secondByte = Integer.valueOf(126).toByte()
            var lengthByteArray = msg.length.toBigInteger().toByteArray()
            var paddingByteArray = ByteArray(2 - lengthByteArray.size)

            lengthBytes = lengthByteArray + paddingByteArray

        }

        val maskingBytes = byteArrayOf((0..255).random().toByte(), (0..255).random().toByte(), (0..255).random().toByte(), (0..255).random().toByte())

        val bodyBytes = msg.toByteArray(Charset.defaultCharset())


        for (bodyIndex in bodyBytes.indices) {
            bodyBytes[bodyIndex] = bodyBytes[bodyIndex] xor maskingBytes[bodyIndex and 0x3]
        }

        return Unpooled.copiedBuffer(byteArrayOf(firstByte, secondByte) + lengthBytes + maskingBytes + bodyBytes)
    }


    private fun processHeader(inBuffer: ByteBuf){
        var dataSize = inBuffer.getUnsignedByte(1).toInt() - 128
        var shift: Int

        when (dataSize) {
            126 -> {
                dataSize = (inBuffer.getUnsignedByte(2).toInt() shl 1) + inBuffer.getUnsignedByte(3).toInt()
                shift = 2
            }
            127 -> {
                dataSize = (inBuffer.getUnsignedByte(2).toInt() shl (7 * 8)) +
                        (inBuffer.getUnsignedByte(3).toInt() shl (6 * 8)) +
                        (inBuffer.getUnsignedByte(4).toInt() shl (5 * 8)) +
                        (inBuffer.getUnsignedByte(5).toInt() shl (4 * 8)) +
                        (inBuffer.getUnsignedByte(6).toInt() shl (3 * 8)) +
                        (inBuffer.getUnsignedByte(7).toInt() shl (2 * 8)) +
                        (inBuffer.getUnsignedByte(8).toInt() shl (1 * 8)) +
                        inBuffer.getUnsignedByte(9).toInt()

                shift = 8
            }
            else -> {
                shift = 0
            }
        }

        remainingFrameSize = dataSize

        inBuffer.readBytes(4 + shift)
    }
}
