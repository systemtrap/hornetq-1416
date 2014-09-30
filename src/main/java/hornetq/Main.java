package hornetq;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.impl.ConfigurationImpl;
import org.hornetq.core.remoting.impl.netty.NettyAcceptorFactory;
import org.hornetq.core.server.HornetQServer;
import org.hornetq.core.server.HornetQServers;

public class Main {
    public Main() {
        System.out.println("Main");
    }

    public static void main(String[] args) throws Exception {
        Configuration configuration = new ConfigurationImpl();

        configuration.setJournalDirectory("target/data/journal");
        configuration.setPersistenceEnabled(false);
        configuration.setSecurityEnabled(false);

        TransportConfiguration transpConf = new TransportConfiguration(
                NettyAcceptorFactory.class.getName());

        configuration.getAcceptorConfigurations().add(transpConf);

        // Step 2. Create and start the server
        HornetQServer server = HornetQServers.newHornetQServer(configuration);
        server.start();
    }
}
