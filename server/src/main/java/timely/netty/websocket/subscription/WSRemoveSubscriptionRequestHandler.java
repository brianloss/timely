package timely.netty.websocket.subscription;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import timely.api.request.subscription.RemoveSubscription;
import timely.subscription.Subscription;
import timely.subscription.SubscriptionRegistry;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Component
@Scope(SCOPE_PROTOTYPE)
public class WSRemoveSubscriptionRequestHandler extends SimpleChannelInboundHandler<RemoveSubscription> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RemoveSubscription remove) throws Exception {
        Subscription s = SubscriptionRegistry.get().get(remove.getSubscriptionId());
        if (null != s) {
            s.removeMetric(remove.getMetric());
        }
    }

}
