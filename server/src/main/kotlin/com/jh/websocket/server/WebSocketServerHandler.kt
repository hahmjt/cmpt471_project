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
    private var isConnected: Boolean = false
    private var remainingFrameSize = 0
    private var currentFrameKey: ShortArray = shortArrayOf()
    private var currentFrameKeyIndex: Int = 0

    @Throws(Exception::class)
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        val inBuffer = msg as ByteBuf
        val received = inBuffer.toString(CharsetUtil.UTF_8)

        if (!isConnected) {
            initiateWebSocketHandShake(ctx, received)
            return
        }

        if (remainingFrameSize == 0) {
            processHeader(inBuffer)
        }


        while (inBuffer.isReadable) {
            val decoded = inBuffer.readByte() xor currentFrameKey[currentFrameKeyIndex and 0x3].toByte()
            print(decoded.toChar())
            currentFrameKeyIndex++
            remainingFrameSize--

            if(inBuffer.isReadable && remainingFrameSize == 0){
                processHeader(inBuffer.discardReadBytes())
            }
        }
    }

    @Throws(Exception::class)
    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        ctx.flush()
        this.isConnected = true
    }

    @Throws(Exception::class)
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }


    private fun initiateWebSocketHandShake(ctx: ChannelHandlerContext, received: String) {
        val rawHttp = RawHttp()
        val httpRequest = rawHttp.parseRequest(received)

        if (httpRequest.headers.get("Connection").contains("Upgrade") && httpRequest.headers.get("Upgrade").contains("websocket")) {

            val response = ("HTTP/1.1 101 Switching Protocols\r\n"
                    + "Connection: Upgrade\r\n"
                    + "Upgrade: websocket\r\n"
                    + "Sec-WebSocket-Accept: "
                    + Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest((httpRequest.headers.get("Sec-WebSocket-Key").first() + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").toByteArray()))
                    + "\r\n\r\n")

            ctx.write(Unpooled.copiedBuffer(response, CharsetUtil.UTF_8))
        }
    }

    fun getBit(value: Int, position: Int): Int {
        return (value shr position) and 1;
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

        currentFrameKey = shortArrayOf(inBuffer.getUnsignedByte(2 + shift), inBuffer.getUnsignedByte(3 + shift), inBuffer.getUnsignedByte(4 + shift), inBuffer.getUnsignedByte(5 + shift))
        currentFrameKeyIndex = 0

        remainingFrameSize = dataSize

        inBuffer.readBytes(6 + shift)
    }
}
