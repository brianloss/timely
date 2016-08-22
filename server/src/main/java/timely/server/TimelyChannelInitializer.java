package timely.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import org.springframework.context.ApplicationContext;

public class TimelyChannelInitializer extends ChannelInitializer<SocketChannel> {

    public interface PipelineBuilder {

        void build(Channel channel);
    }

    private final ApplicationContext applicationContext;
    private final String pipelineBuilderName;

    public TimelyChannelInitializer(ApplicationContext applicationContext, String pipelineBuilderName) {
        this.applicationContext = applicationContext;
        this.pipelineBuilderName = pipelineBuilderName;
    }

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        PipelineBuilder pipelineBuilder = applicationContext.getBean(pipelineBuilderName, PipelineBuilder.class);
        pipelineBuilder.build(socketChannel);
    }
}
