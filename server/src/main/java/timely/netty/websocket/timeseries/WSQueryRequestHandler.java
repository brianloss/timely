package timely.netty.websocket.timeseries;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import timely.api.request.timeseries.QueryRequest;
import timely.api.response.TimelyException;
import timely.netty.http.timeseries.HttpQueryRequestHandler;
import timely.store.DataStore;
import timely.util.JsonUtil;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Component
@Scope(SCOPE_PROTOTYPE)
public class WSQueryRequestHandler extends SimpleChannelInboundHandler<QueryRequest> {

    private static final Logger LOG = LoggerFactory.getLogger(HttpQueryRequestHandler.class);
    private final DataStore dataStore;

    @Autowired
    public WSQueryRequestHandler(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, QueryRequest msg) throws Exception {
        try {
            String response = JsonUtil.getObjectMapper().writeValueAsString(dataStore.query(msg));
            ctx.writeAndFlush(new TextWebSocketFrame(response));
        } catch (TimelyException e) {
            if (e.getMessage().contains("No matching tags")) {
                LOG.trace(e.getMessage());
            } else {
                LOG.error(e.getMessage(), e);
            }
            ctx.writeAndFlush(new CloseWebSocketFrame(1008, e.getMessage()));
        }
    }

}
