package timely.netty.http.auth;

import io.netty.channel.ChannelHandlerContext;

import org.springframework.context.annotation.Scope;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import org.springframework.stereotype.Component;
import timely.TimelyConfiguration;
import timely.api.request.auth.BasicAuthLoginRequest;
import timely.auth.AuthenticationService;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Component
@Scope(SCOPE_PROTOTYPE)
public class BasicAuthLoginRequestHandler extends TimelyLoginRequestHandler<BasicAuthLoginRequest> {

    public BasicAuthLoginRequestHandler(TimelyConfiguration conf) {
        super(conf);
    }

    @Override
    protected Authentication authenticate(ChannelHandlerContext ctx, BasicAuthLoginRequest msg)
            throws AuthenticationException {
        // Perform the login process using username/password
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(msg.getUsername(),
                msg.getPassword());
        return AuthenticationService.getAuthenticationManager().authenticate(token);
    }

}
