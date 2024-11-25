import clientregisterservice.*;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;

public class ClientRegister extends ClientRegisterServiceGrpc.ClientRegisterServiceImplBase {
    private ArrayList<ServerLocal> servers;
    private int idx;

    public ClientRegister(ArrayList<ServerLocal> servers) {
        this.servers = servers;
        idx = -1;
    }

    @Override
    public void getServer(Nothing request, StreamObserver<Server> responseObserver) {
        System.out.println("Get Server called.");
        while (servers.isEmpty()){
            System.out.println("No server, we wait.");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        idx++;
        if (idx >= servers.size()) idx = 0;
        System.out.println("Round-Robin idx = " + idx);
        ServerLocal aux = servers.get(idx);
        Server res = Server.newBuilder().setIp(aux.ip).setPort(aux.port).build();
        System.out.println("Server sent:" + aux.ip + ":" + aux.port);
        responseObserver.onNext(res);
        responseObserver.onCompleted();
    }

    @Override
    public void reportBadServer(Server request, StreamObserver<Nothing> responseObserver) {
        System.out.println("Bad Server reported. IP:" + request.getIp() + " Port:" + request.getPort());
        ServerLocal aux = new ServerLocal(request.getIp(), request.getPort());
        int idx = servers.indexOf(aux);
        boolean rem = servers.remove(aux);
        Nothing res = Nothing.newBuilder().build();
        responseObserver.onNext(res);
        responseObserver.onCompleted();
    }
}
