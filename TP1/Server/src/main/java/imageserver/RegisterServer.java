package imageserver;

import registerserverservice.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class RegisterServer  {
    private static ManagedChannel channel;

    private static RegisterServerServiceGrpc.RegisterServerServiceBlockingStub blockingStub;

    public void RegistServer(String svcIp, String RegisterIp, int svcPort, int registerPort) {
        try {
            System.out.println("Entered RegistServer");
            channel = ManagedChannelBuilder.forAddress(RegisterIp, registerPort)
                    .usePlaintext()
                    .build();
            blockingStub = RegisterServerServiceGrpc.newBlockingStub(channel);
            Confirmation c = blockingStub.registerServer(
                    Server.newBuilder()
                            .setIp(svcIp)
                            .setPort(svcPort)
                            .build()
            );
            System.out.println(c.getRegister());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
