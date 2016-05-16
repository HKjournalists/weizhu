package com.weizhu.server.conn;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.util.ReferenceCountUtil;

import com.weizhu.proto.WeizhuProtos;

@Sharable
class PacketCodec extends ChannelDuplexHandler {
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg0) throws Exception {
		if (!(msg0 instanceof ByteBuf)) {
			ctx.fireChannelRead(msg0);
			return;
		}
		
		final ByteBuf msg = (ByteBuf) msg0;
		
		try {
			ctx.fireChannelRead(WeizhuProtos.SocketUpPacket.PARSER.parseFrom(new ByteBufInputStream(msg)));
		} finally {
			ReferenceCountUtil.release(msg);
		}
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg0, ChannelPromise promise) throws Exception {
		if (!(msg0 instanceof WeizhuProtos.SocketDownPacket)) {
			ctx.write(msg0, promise);
			return;
		}
		
		final WeizhuProtos.SocketDownPacket msg = (WeizhuProtos.SocketDownPacket) msg0;
        int length = msg.getSerializedSize();
        
		ByteBuf buf = ctx.alloc().ioBuffer(length + 4);
		buf.writeInt(length);
		msg.writeTo(new ByteBufOutputStream(buf));
		
		ctx.write(buf, promise);
	}
}
