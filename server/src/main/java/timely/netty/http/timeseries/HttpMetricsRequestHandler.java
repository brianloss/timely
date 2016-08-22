package timely.netty.http.timeseries;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpHeaders.Names;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import timely.api.request.timeseries.MetricsRequest;
import timely.api.response.timeseries.MetricsResponse;
import timely.netty.http.TimelyHttpHandler;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Component
@Scope(SCOPE_PROTOTYPE)
public class HttpMetricsRequestHandler extends SimpleChannelInboundHandler<MetricsRequest> implements TimelyHttpHandler {

    private final MetricsResponse response;

    @Autowired
    public HttpMetricsRequestHandler(MetricsResponse response) {
        this.response = response;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MetricsRequest msg) throws Exception {
        String acceptHeader = msg.getRequestHeaders().get(Names.ACCEPT);
        sendResponse(ctx, response.toHttpResponse(acceptHeader));
    }

}
