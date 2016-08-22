package timely.netty.websocket.timeseries;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import timely.api.request.timeseries.MetricsRequest;
import timely.api.response.timeseries.MetricsResponse;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Component
@Scope(SCOPE_PROTOTYPE)
public class WSMetricsRequestHandler extends SimpleChannelInboundHandler<MetricsRequest> {

    private final MetricsResponse response;

    @Autowired
    public WSMetricsRequestHandler(MetricsResponse response) {
        this.response = response;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MetricsRequest m) throws Exception {
        ctx.writeAndFlush(response.toWebSocketResponse("application/json"));
    }

}
