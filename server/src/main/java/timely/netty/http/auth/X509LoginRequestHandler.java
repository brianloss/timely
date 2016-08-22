package timely.netty.http.auth;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.SslHandler;

import java.security.cert.X509Certificate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import org.springframework.stereotype.Component;
import timely.Configuration;
import timely.TimelyConfiguration;
import timely.api.request.auth.X509LoginRequest;
import timely.auth.AuthenticationService;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Component
@Scope(SCOPE_PROTOTYPE)
public class X509LoginRequestHandler extends TimelyLoginRequestHandler<X509LoginRequest> {

    @Autowired
    public X509LoginRequestHandler(TimelyConfiguration conf) {
        super(conf);
    }

    @Override
    protected Authentication authenticate(ChannelHandlerContext ctx, X509LoginRequest loginRequest) throws Exception {
        // If we are operating in 2 way SSL, then get the subjectDN from the
        // client certificate and perform the login process.
        SslHandler sslHandler = (SslHandler) ctx.channel().pipeline().get("ssl");
        if (null != sslHandler) {
            X509Certificate clientCert = (X509Certificate) sslHandler.engine().getSession().getPeerCertificates()[0];
            String subjectDN = AuthenticationService.extractDN(clientCert);
            PreAuthenticatedAuthenticationToken token = new PreAuthenticatedAuthenticationToken(subjectDN, clientCert);
            return AuthenticationService.getAuthenticationManager().authenticate(token);
        } else {
            throw new IllegalStateException("The expected SSL handler is not in the pipeline.");
        }
    }

}
