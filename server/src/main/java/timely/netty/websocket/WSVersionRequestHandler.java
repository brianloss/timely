package timely.netty.websocket;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import timely.api.request.VersionRequest;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Component
@Scope(SCOPE_PROTOTYPE)
public class WSVersionRequestHandler extends SimpleChannelInboundHandler<VersionRequest> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, VersionRequest msg) throws Exception {
        ctx.writeAndFlush(new TextWebSocketFrame(VersionRequest.VERSION));
    }

}
