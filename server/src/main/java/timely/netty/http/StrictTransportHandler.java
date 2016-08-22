package timely.netty.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import timely.Configuration;
import timely.TimelyConfiguration;
import timely.api.response.StrictTransportResponse;
import timely.api.response.TimelyException;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Component
@Scope(SCOPE_PROTOTYPE)
public class StrictTransportHandler extends SimpleChannelInboundHandler<StrictTransportResponse> {

    public static final String HSTS_HEADER_NAME = "Strict-Transport-Security";
    private String hstsMaxAge = "max-age=";

    @Autowired
    public StrictTransportHandler(TimelyConfiguration conf) {
        hstsMaxAge = "max-age=" + conf.getHttp().getStrictTransportMaxAge();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, StrictTransportResponse msg) throws Exception {
        TimelyException e = new TimelyException(HttpResponseStatus.NOT_FOUND.code(),
                "Returning HTTP Strict Transport Security response", null, null);
        e.addResponseHeader(HSTS_HEADER_NAME, hstsMaxAge);
        // Don't call sendHttpError from here, throw an error instead and let
        // the exception handler catch it.
        throw e;
    }

}
