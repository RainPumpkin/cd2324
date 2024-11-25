import com.google.protobuf.ByteString;
import com.rabbitmq.client.*;
import io.grpc.stub.StreamObserver;
import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadMessage;
import usermanagerservice.UserManagerServiceGrpc;
import usermanagerservice.Resume;
import usermanagerservice.Type;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

public class ManageServer extends UserManagerServiceGrpc.UserManagerServiceImplBase {
    private ReentrantLock lock;
    private final SpreadConnection connection;
    Channel channel;
    private List<Request> Requests = new LinkedList();
    private final String ExchangeName;
    private final int MAXBYTES = 32 * 1024;

    public ManageServer(String address, int port, String IpBroker, int BrokerPort, String ExchangeName) throws IOException, SpreadException, TimeoutException {
        this.ExchangeName = ExchangeName;
        lock = new ReentrantLock();
        // Spread connection
        connection = new SpreadConnection();
        connection.connect(InetAddress.getByName(address), port, "", false, false);
        // rabbitMq connection
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(IpBroker);
        factory.setPort(BrokerPort);
        Connection con = factory.newConnection();
        channel = con.createChannel();
        createExchanger();
    }

    public void serverClosed() throws IOException, TimeoutException {
        destroyExchanger();
        channel.close();
    }

    public void getResume(Type request, StreamObserver<Resume> reply){
        System.out.println("Entrou no Get Resume");
        try {
            Request r = enterRequest();
            while (!getToken(r)) {
                Thread.sleep(1000);
            }
            String key = "Client" + Math.floor(Math.random() * 10000);
            String data = "StartResume/"+ExchangeName+"/"+key;
            SpreadMessage msg = new SpreadMessage();
            msg.setSafe();
            msg.addGroup(request.getType());
            msg.setData(data.getBytes());
            enterMQ(key, key, reply, r);
            connection.multicast(msg);
        } catch (SpreadException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Saiu do Get Resume");
    }

    private void createExchanger() throws IOException {
        channel.exchangeDeclare(ExchangeName, BuiltinExchangeType.DIRECT, true);
    }

    private void destroyExchanger() throws IOException {
        channel.exchangeDelete(ExchangeName);
    }

    private void enterMQ(String queueName, String routingKey, StreamObserver<Resume> reply, Request r) throws IOException {
        System.out.println("entered entermq");
        channel.queueDeclare(queueName, true, false, false, null);
        channel.queueBind(queueName, ExchangeName, routingKey);
        CancelCallback cancelCallback=(consumerTag)->{
            System.out.println("CANCEL Received! " + consumerTag);
            channel.queueUnbind(queueName, ExchangeName, routingKey);
        };

        DeliverCallback deliverCallbackWithoutAck = (consumerTag, delivery) -> {
            System.out.println("entered callback");
            String recMessage = new String(delivery.getBody(), "UTF-8");  //File name on body
            long deliverTag=delivery.getEnvelope().getDeliveryTag();

            if (recMessage.equals("nack"))
                channel.basicNack(deliverTag, false, true);
            else channel.basicAck(deliverTag,false);
            sendFile(reply, recMessage);
            r.releaseToken();
            Requests.remove(r);
            channel.queueUnbind(queueName, ExchangeName, routingKey);
            System.out.println("left callback");
        };
        channel.basicConsume(queueName, false, deliverCallbackWithoutAck, cancelCallback);
        System.out.println("left entermq");
    }

    private void sendFile(StreamObserver<Resume> reply, String path) {
        System.out.println(path);
        System.out.println(path + "/resume.txt");
        try {
            File f = Path.of(path + "/resume.txt").toFile();
            DataInputStream dt = new DataInputStream(new FileInputStream(f));
            byte[] read;
            while (true){
                read = dt.readNBytes(MAXBYTES);
                reply.onNext(
                        Resume.newBuilder()
                                .setData(ByteString.copyFrom(read))
                                .setName("resume.txt")
                                .build()
                );
                if (read.length<MAXBYTES) break;
            }
            dt.close();
            reply.onCompleted();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Request enterRequest() {
        lock.lock();
        try {
            Request r = new Request();
            Requests.add(r);
            return r;
        } finally {
            lock.unlock();
        }
    }

    private boolean getToken(Request r) {
        lock.lock();
        try {
            for (Request request : Requests) {
                if (request.getHasToken())
                    return false;
            }
            if(Requests.indexOf(r) != 0) return false;
            r.receiveToken();
            return true;
        } finally {
            lock.unlock();
        }
    }
}