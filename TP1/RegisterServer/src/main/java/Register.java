import io.grpc.ServerBuilder;

import java.util.ArrayList;

public class Register {
    private static int svcPort = 8500;
    private static ArrayList<ServerLocal> servers;
    public static void main(String[] args) {
        servers = new ArrayList<>();

        try {
            if (args.length > 0) svcPort = Integer.parseInt(args[0]);
            io.grpc.Server svc = ServerBuilder
                    .forPort(svcPort)
                    .addService(new ClientRegister(servers))
                    .addService(new RegisterServer(servers))
                    .build();
            svc.start();
            System.out.println("Server started, listening on " + svcPort);
            //Scanner scan = new Scanner(System.in);
            //scan.nextLine();
            svc.awaitTermination();
            svc.shutdown();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
