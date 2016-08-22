package timely.netty.http;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import timely.api.request.VersionRequest;
import timely.netty.Constants;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Component
@Scope(SCOPE_PROTOTYPE)
public class HttpVersionRequestHandler extends SimpleChannelInboundHandler<VersionRequest> implements TimelyHttpHandler {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, VersionRequest v) throws Exception {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                Unpooled.copiedBuffer(VersionRequest.VERSION.getBytes(StandardCharsets.UTF_8)));
        response.headers().set(Names.CONTENT_TYPE, Constants.TEXT_TYPE);
        response.headers().set(Names.CONTENT_LENGTH, response.content().readableBytes());
        sendResponse(ctx, response);
    }

}
