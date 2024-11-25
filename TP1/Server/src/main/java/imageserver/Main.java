package imageserver;

import io.grpc.ServerBuilder;

public class Main {
    private static RegisterServer register;
    private static final int SVIP = 8080;

    public static void main(String[] args) {
        try{
            register = new RegisterServer();
            io.grpc.Server svc = ServerBuilder
                    .forPort(SVIP)
                    .addService(new ImageServer())
                    .build();
            svc.start();
            register.RegistServer(args[0], args[1], SVIP, 8500);
            System.out.println("Server started listening on " + 8080);
            svc.awaitTermination();
            svc.shutdown();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
