package me.xuqu.palmx.net.http3.queue;


import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import me.xuqu.palmx.net.http3.command.QueueCommand;

import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

// 参考 PendingWriteQueue
public class PalmxWriteQueue extends BatchExecutorQueue<QueueCommand> {

    public PalmxWriteQueue() {
    }

    public PalmxWriteQueue(int chunkSize) {
        super(chunkSize);
    }

    public ChannelFuture enqueue(QueueCommand command) {
        return this.enqueueFuture(command, command.getChannel().eventLoop());
    }

    public ChannelFuture enqueueFuture(QueueCommand command, Executor executor) {
        ChannelPromise promise = command.getPromise();
        if (promise == null) {
            Channel ch = command.getChannel();
            promise = ch.newPromise();
            command.setPromise(promise);
        }
        super.enqueue(command, executor);
        return promise;
    }

    @Override
    protected void prepare(QueueCommand item) {
        try {
            Channel channel = item.getChannel();
            item.run(channel);
        } catch (CompletionException e) {
            item.getPromise().tryFailure(e.getCause());
        }
    }

    @Override
    protected void flush(QueueCommand item) {
        try {
            Channel channel = item.getChannel();
            item.run(channel);
            channel.flush();
        } catch (CompletionException e) {
            item.getPromise().tryFailure(e.getCause());
        }
    }
}
