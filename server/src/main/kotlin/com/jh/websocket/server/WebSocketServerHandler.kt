import com.jh.websocket.server.DataStore
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.util.CharsetUtil
import org.springframework.context.ApplicationContext
import rawhttp.core.RawHttp
import java.net.InetAddress
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.*
import kotlin.experimental.xor


class WebSocketServerHandler : ChannelInboundHandlerAdapter() {

    private var isConnected: Boolean = false
    private var isDataConnection = false
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

        //This will read the TCP buffer until designated length and process a new frame
        while (inBuffer.isReadable) {
            val decoded = inBuffer.readByte() xor currentFrameKey[currentFrameKeyIndex and 0x3].toByte()
            print(decoded.toChar())
            currentFrameKeyIndex++
            remainingFrameSize--

            if(inBuffer.isReadable && remainingFrameSize == 0){
                processHeader(inBuffer.discardReadBytes())
            }
        }

        inBuffer.discardReadBytes()
    }

    @Throws(Exception::class)
    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        if(isConnected){
            if(isDataConnection){
                DataStore.set.forEach {
                    ctx.write(getResponseFrame(it))
                }
            }else{
                ctx.write(getResponseFrame("Hi This is "+InetAddress.getLocalHost().hostName))
            }
        }
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

        isDataConnection = httpRequest.startLine.uri.path.contains("data",true)

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

    private fun getResponseFrame(msg: String): ByteBuf {
        val firstByte: Byte = Integer.valueOf(129).toByte()

        var secondByte: Byte = Integer.valueOf(msg.length).toByte()

        var lengthBytes = byteArrayOf()

        //Support for variable length size
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

        val bodyBytes = msg.toByteArray(Charset.defaultCharset())

        return Unpooled.copiedBuffer(byteArrayOf(firstByte, secondByte) + lengthBytes + bodyBytes)
    }


    private fun processHeader(inBuffer: ByteBuf){
        var dataSize = inBuffer.getUnsignedByte(1).toInt() - 128
        var shift: Int

        //Support for variable length size, if second byte - 128 = 126 then is uses next 2 bytes if it's 127 it uses next 8 bytes as size field
        //data field needs to be shifted accordingly
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
