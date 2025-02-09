package me.xuqu.palmx.net.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.incubator.codec.http3.Http3DataFrame;
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import io.netty.incubator.codec.http3.Http3RequestStreamInboundHandler;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.common.PalmxConstants;
import me.xuqu.palmx.exception.RpcInvocationException;
import me.xuqu.palmx.net.RpcResponse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class Http3RpcResponseHandler extends Http3RequestStreamInboundHandler {

    public static final Map<Integer, Promise<Object>> map = new ConcurrentHashMap<>();

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Caught a exception", cause);
        ctx.close().syncUninterruptibly();
    }


    @Override
    protected void channelRead(
            ChannelHandlerContext ctx, Http3HeadersFrame frame) {
//        log.info("this is head {}", ctx);
        ReferenceCountUtil.release(frame);
    }

    @Override
    protected void channelRead(
            ChannelHandlerContext ctx, Http3DataFrame frame) {
//        log.info("this is body {}", ctx);
        RpcResponse rpcResponse = MessageCodecHelper.decodeResponse(frame.content());
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

    @Override
    protected void channelInputClosed(ChannelHandlerContext ctx) {
    }
}
