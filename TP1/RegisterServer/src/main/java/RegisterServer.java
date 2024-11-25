import io.grpc.stub.StreamObserver;
import registerserverservice.*;

import java.util.ArrayList;


public class RegisterServer extends RegisterServerServiceGrpc.RegisterServerServiceImplBase {
    private ArrayList<ServerLocal> servers;

    public RegisterServer(ArrayList<ServerLocal> servers) {
        this.servers=servers;
    }

    @Override
    public void registerServer(Server request, StreamObserver<Confirmation> responseObserver) {
        System.out.println("Resgistering server " + request.getIp() + ":" + request.getPort());
        ServerLocal aux = new ServerLocal(request.getIp(), request.getPort());
        servers.add(aux);
        Confirmation conf = Confirmation.newBuilder().setRegister(true).build();
        responseObserver.onNext(conf);
        responseObserver.onCompleted();
    }

    @Override
    public void unregister(Server request, StreamObserver<Confirmation> responseObserver) {
        System.out.println("Unresgistering server " + request.getIp() + ":" + request.getPort());
        ServerLocal aux = new ServerLocal(request.getIp(), request.getPort());
        int idx = servers.indexOf(aux);
        boolean rem = servers.remove(aux);
        Confirmation conf = Confirmation.newBuilder().setRegister(rem).build();
        responseObserver.onNext(conf);
        responseObserver.onCompleted();
    }
}
