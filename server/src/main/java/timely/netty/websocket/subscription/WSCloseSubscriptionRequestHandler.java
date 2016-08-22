package timely.netty.websocket.subscription;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import timely.api.request.subscription.CloseSubscription;
import timely.subscription.Subscription;
import timely.subscription.SubscriptionRegistry;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Component
@Scope(SCOPE_PROTOTYPE)
public class WSCloseSubscriptionRequestHandler extends SimpleChannelInboundHandler<CloseSubscription> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CloseSubscription close) throws Exception {
        Subscription s = SubscriptionRegistry.get().remove(close.getSubscriptionId());
        if (null != s) {
            s.close();
        }
    }

}
