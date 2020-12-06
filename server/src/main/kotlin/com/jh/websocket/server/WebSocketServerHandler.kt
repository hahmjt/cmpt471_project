import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.util.CharsetUtil
import rawhttp.core.RawHttp
import java.security.MessageDigest
import java.util.*
import kotlin.experimental.xor


class WebSocketServerHandler : ChannelInboundHandlerAdapter() {
    @Throws(Exception::class)
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        val inBuffer = msg as ByteBuf
        val received = inBuffer.toString(CharsetUtil.UTF_8)

        val rawHttp = RawHttp()

        val decoded = ByteArray(6)
        val encoded = byteArrayOf(132.toByte())
        val key = byteArrayOf(214.toByte(), 206.toByte(), 240.toByte(), 144.toByte())
        for (i in encoded.indices) {
            decoded[i] = (encoded[i] xor key[i and 0x3]) as Byte
        }

        decoded[0].toChar()




        val httpRequest = rawHttp.parseRequest(received)

        println("Server received: $httpRequest")

        println("IsWebSocket ${httpRequest.headers.get("Connection").contains("Upgrade") && httpRequest.headers.get("Upgrade").contains("websocket")}")

        val response = ("HTTP/1.1 101 Switching Protocols\r\n"
                + "Connection: Upgrade\r\n"
                + "Upgrade: websocket\r\n"
                + "Sec-WebSocket-Accept: "
                + Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest((httpRequest.headers.get("Sec-WebSocket-Key").first() + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").toByteArray()))
                + "\r\n\r\n")



        ctx.write(Unpooled.copiedBuffer(response, CharsetUtil.UTF_8))
    }

    @Throws(Exception::class)
    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        ctx.flush()
    }

    @Throws(Exception::class)
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }
}