import io.grpc.ServerBuilder;
import spread.SpreadException;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class main {

    private static final int port = 4803;

    private static String dameonIp;
    private static String brokerIp;
    private static final int brokerPort = 5672;

    private static final String ExchangeName = "ExchangeName";

    public static void main(String[] args) throws InterruptedException, IOException, SpreadException, TimeoutException {
        if(args.length >= 2) {
            dameonIp = args[0];
            brokerIp = args[1];
        } else {
            System.out.println("Missing arguments, please specify the daemonIp and brokerIp");
            System.exit(0);
        }
        ManageServer server = new ManageServer(dameonIp, port, brokerIp, brokerPort, ExchangeName);
        io.grpc.Server svc = ServerBuilder
                .forPort(8080)
                .addService(server)
                .build();
        svc.start();
        svc.awaitTermination();
        server.serverClosed();
        svc.shutdown();
    }
}
