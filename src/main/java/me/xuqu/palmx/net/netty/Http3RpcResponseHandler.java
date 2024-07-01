package me.xuqu.palmx.net.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.incubator.codec.http3.*;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.common.PalmxConstants;
import me.xuqu.palmx.exception.RpcInvocationException;
import me.xuqu.palmx.net.RpcInvocation;
import me.xuqu.palmx.net.RpcMessage;
import me.xuqu.palmx.net.RpcResponse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class Http3RpcResponseHandler extends Http3RequestStreamInboundHandler {

    public static final Map<Integer, Promise<Object>> map = new ConcurrentHashMap<>();

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
//        log.error("Caught a exception", cause);
//        ctx.close().syncUninterruptibly();
    }


    @Override
    protected void channelRead(
            ChannelHandlerContext ctx, Http3HeadersFrame frame) {
        ReferenceCountUtil.release(frame);
    }

    @Override
    protected void channelRead(
            ChannelHandlerContext ctx, Http3DataFrame frame) {
        String string = frame.content().toString(CharsetUtil.UTF_8);
        RpcResponse rpcResponse = MessageCodecHelper.decodeResponse2(string);
        // 从缓存中移除该序列号的 Promise
        Promise<Object> promise = map.remove(rpcResponse.getSequenceId());
        if (promise != null) {
            if (rpcResponse.getStatus() == PalmxConstants.NETTY_RPC_RESPONSE_STATUS_OK) {
                promise.setSuccess(rpcResponse.getData());
            } else {
                promise.setFailure(new RpcInvocationException(rpcResponse.getMessage()));
            }
        }
        ReferenceCountUtil.release(frame);
    }

    private static Http3HeadersFrame getDefaultHttp3HeadersFrame(int length) {
        Http3HeadersFrame headersFrame = new DefaultHttp3HeadersFrame();
        headersFrame.headers().status("200");
        headersFrame.headers().add("server", "netty");
        headersFrame.headers().addInt("content-length", length);
        return headersFrame;
    }

    @Override
    protected void channelInputClosed(ChannelHandlerContext ctx) {
    }
}
