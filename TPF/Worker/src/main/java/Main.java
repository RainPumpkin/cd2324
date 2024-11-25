import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.CancelCallback;
import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

public class Main {
    //RabbitMQ
    private static String ipBroker;

    //Spread
    private static String daemonIP;
    private static int daemonPort = 4803;
    private static AdvancedMessageHandling advancedMsgHandling;
    private static String filename;
    private static FileWriter writer;

    private static final String[] Queues = {"ALIMENTAR", "CASA"};

    public static void main(String[] args) throws IOException, SpreadException, TimeoutException {
        String spreadName;
        String queueName;
        if(args.length<=3){
            System.out.println("Missing parameters. Expected: [spreadName] [TypeId] [ipBroker] [daemonIP]");
            spreadName = "WorkerBOT";
            queueName = queueType(0);
        } else {
            spreadName = args[0];
            queueName = queueType(Integer.parseInt(args[1]));
            ipBroker = args[2];
            daemonIP = args[3];
        }

        //RabbitMQ
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(ipBroker); factory.setPort(5672);
        Connection rconnection = factory.newConnection();
        Channel channel = rconnection.createChannel();

        filename = queueName + "/" + spreadName + ".txt";

        String consumerTag = initSale(channel, queueName);

        //Spread
        SpreadConnection sconnection = new SpreadConnection();
        sconnection.connect(InetAddress.getByName(daemonIP), daemonPort, spreadName, false, true);
        SpreadGroup group=new SpreadGroup();
        group.join(sconnection, queueName);
        advancedMsgHandling = new AdvancedMessageHandling(sconnection, channel, spreadName, queueName);
        sconnection.add(advancedMsgHandling);

        Scanner scan = new Scanner(System.in);
        System.out.println(consumerTag+": waiting for messages or Press any key to finish");
        scan.nextLine();
        System.out.println("Terminating");
        sconnection.disconnect();
        rconnection.close();
    }
    public static void initFile(){
        System.out.println("Opening " + filename);
        File file = new File(filename);
        try {
            if (!file.createNewFile()){
                System.out.println("File exists already.");
                if(!file.delete()){
                    System.out.println("Failed delete of file.");
                    file.delete();
                    Thread.sleep(10000);
                }
                initFile();
                return;
            }
            System.out.println("file did not exist");
            writer = new FileWriter(file);
        } catch (IOException e) {
            System.out.println("ERROR CREATING FILE");
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static String initSale(Channel channel, String queueName){
        initFile();
        // Consumer handler to receive cancel receiving messages
        CancelCallback cancelCallback=(consumerTag)->{
            System.out.println("CANCEL Received! "+consumerTag);
        };

        DeliverCallback deliverCallbackWithoutAck = (consumerTag, delivery) -> {
            String recMessage = new String(delivery.getBody(), "UTF-8");

            String routingKey = delivery.getEnvelope().getRoutingKey();
            long deliverTag = delivery.getEnvelope().getDeliveryTag();
            System.out.println(consumerTag+": Message Received:" + routingKey+":"+recMessage);

            //void basicAck(long deliveryTag, boolean multiple) throws IOException;
            //void basicNack(long deliveryTag, boolean multiple, boolean requeue) throws IOException;
            if (recMessage.equals("nack")) channel.basicNack(deliverTag, false, true);
            else channel.basicAck(deliverTag,false);
            String category = routingKey.split("\\.")[1];
            writeSale(category, recMessage);
        };
        try {
            return channel.basicConsume(queueName, false, deliverCallbackWithoutAck, cancelCallback);
        } catch (IOException e) {
            System.out.println("Error in basic Consume");
            throw new RuntimeException(e);
        }
    }

    public static void writeSale(String category, String saleObj){
        Gson gson = new GsonBuilder().create();
        Sale sale = gson.fromJson(saleObj, Sale.class);
        try {
            writer.write(category + "|" + sale.toString() + "\n");
            writer.flush();
        } catch (IOException e) {
            System.out.println("Error writing");
            throw new RuntimeException(e);
        }
    }

    public static void closeSale(Channel channel, String queueName){
        System.out.println("Closing sales!");
        try {
            writer.flush();
            writer.close();
        } catch (IOException e) {
            System.out.println("ERROR IN CLOSESALE");
            throw new RuntimeException(e);
        }
        // Consumer handler to receive cancel receiving messages
        CancelCallback cancelCallback=(consumerTag)->{
            System.out.println("CANCEL Received! "+consumerTag);
        };

        DeliverCallback deliverCallbackWithoutAck = (consumerTag, delivery) -> {
            System.out.println("Doing resume. NACK");
            long deliverTag = delivery.getEnvelope().getDeliveryTag();
            //void basicNack(long deliveryTag, boolean multiple, boolean requeue) throws IOException;
            channel.basicNack(deliverTag, false, true);
        };
        try {
            channel.basicConsume(queueName, false, deliverCallbackWithoutAck, cancelCallback);
        } catch (IOException e) {
            System.out.println("Error in close sale");
            throw new RuntimeException(e);
        }
    }

    public static String queueType(int id){
        if (id >= Queues.length){
            System.out.println("Id too big, default to " + Queues[0]);
            id = 0;
        }
        return Queues[id];
    }
}