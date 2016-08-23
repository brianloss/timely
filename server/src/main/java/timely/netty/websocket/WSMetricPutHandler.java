package timely.netty.websocket;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import timely.api.model.Metric;
import timely.store.DataStore;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Component
@Scope(SCOPE_PROTOTYPE)
public class WSMetricPutHandler extends SimpleChannelInboundHandler<Metric> {

    private final DataStore dataStore;

    @Autowired
    public WSMetricPutHandler(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Metric m) throws Exception {
        this.dataStore.store(m);
    }

}
