package timely.netty.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import timely.api.request.VersionRequest;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Component
@Scope(SCOPE_PROTOTYPE)
public class TcpVersionHandler extends SimpleChannelInboundHandler<VersionRequest> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, VersionRequest v) throws Exception {
        final ByteBuf response = ctx.alloc().buffer();
        response.writeBytes(v.getVersion().getBytes(StandardCharsets.UTF_8));
        ctx.writeAndFlush(response);
    }

}
