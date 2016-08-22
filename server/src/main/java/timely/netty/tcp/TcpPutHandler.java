package timely.netty.tcp;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import timely.api.model.Metric;
import timely.netty.Constants;
import timely.store.DataStore;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Component
@Scope(SCOPE_PROTOTYPE)
public class TcpPutHandler extends SimpleChannelInboundHandler<Metric> {

    private static final Logger LOG = LoggerFactory.getLogger(TcpPutHandler.class);
    private static final String LOG_ERR_MSG = "Error storing put metric: {}";
    private static final String ERR_MSG = "Error storing put metric: ";
    private final DataStore store;

    @Autowired
    public TcpPutHandler(DataStore store) {
        this.store = store;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Metric msg) throws Exception {
        LOG.trace("Received {}", msg);
        try {
            store.store(msg);
        } catch (Exception e) {
            LOG.error(LOG_ERR_MSG, msg, e);
            ChannelFuture cf = ctx.writeAndFlush(Unpooled.copiedBuffer((ERR_MSG + e.getMessage() + "\n")
                    .getBytes(StandardCharsets.UTF_8)));
            if (!cf.isSuccess()) {
                LOG.error(Constants.ERR_WRITING_RESPONSE, cf.cause());
            }
        }
    }

}
