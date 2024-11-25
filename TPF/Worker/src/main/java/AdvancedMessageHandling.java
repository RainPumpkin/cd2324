import spread.*;

import com.rabbitmq.client.Channel;

public class AdvancedMessageHandling implements AdvancedMessageListener {
    private final String myName;
    private final SpreadConnection connection;
    private final Channel channel;
    private final String tipo;
    private boolean leaderExists;
    private String leaderName;
    private boolean amILeader;
    private boolean first = true;

    public AdvancedMessageHandling(SpreadConnection connection, Channel channel, String myName, String tipo) {
        this.channel = channel;
        this.connection = connection;
        this.myName = myName;
        this.tipo = tipo;
        leaderExists = false;
        leaderName = null;
        amILeader = false;
    }

    @Override
    public void regularMessageReceived(SpreadMessage spreadMessage) {
        String message = new String(spreadMessage.getData());
        System.out.println("Regular ThreadID:" + Thread.currentThread().getId());
        System.out.println("The message is: " + new String(spreadMessage.getData()));
        // StartResume/{exchangeName}/{bindingkey}
        if(message.contains("StartResume")){
            System.out.println("RESUME ANNOUNCED");
            System.out.println(message);
            Main.closeSale(channel, tipo);
            //elect a member if there is no leader
            if(!leaderExists){
                doElection(spreadMessage.getMembershipInfo().getMembers());
            } else {
                //do resume stuff
                if(amILeader){
                    String tipo = spreadMessage.getGroups()[0].toString();
                    String[] keys = message.split("/");
                    String exchange = keys[1];
                    String bindingKey = keys[2];
                    mergeAndSend(tipo, exchange, bindingKey);
                }
            }
        } else
            if (message.contains("GetLeader") && amILeader){
            System.out.println("GetLeader");
            sendMessage("Leader=" + myName);
        } else
            if (message.contains("Leader=")){
            System.out.println("Leader");
            leaderName = message.split("=")[1];
            leaderExists = true;
        } else
            if (message.contains("Finished")){
            System.out.println("finished");
            resumeOperations();
        }
    }

    @Override
    public void membershipMessageReceived(SpreadMessage spreadMessage) {
        System.out.println("MemberShip ThreadID:" + Thread.currentThread().getId());
        MembershipInfo info = spreadMessage.getMembershipInfo();
        if (info.isSelfLeave()) {
            System.out.println("Left group:"+info.getGroup().toString());
        } else {
            SpreadGroup[] members = info.getMembers();
            if(!leaderExists && members.length==1){
                System.out.println("First, and leader");
                leaderExists = true;
                leaderName = myName;
                amILeader = true;
                first = false;
            } else if(first){
                sendMessage("GetLeader");
                first = false;
            }else if(!info.getGroup().toString().equals(leaderName)) {
                System.out.println("Leader has left us.");
                leaderExists = false; leaderName = null;
            }
            System.out.println("members of belonging group:" + info.getGroup().toString());
            for (int i = 0; i < members.length; ++i) {
                System.out.print(members[i] + "\n");
            }
            System.out.println();
        }
    }

    public void sendMessage(String txtMessage) {
        SpreadMessage msg = new SpreadMessage();
        msg.setSafe();
        msg.addGroup(tipo);
        msg.setData(txtMessage.getBytes());
        try {
            connection.multicast(msg);
        } catch (SpreadException e) {
            System.out.println("Failed to send message");
            throw new RuntimeException(e);
        }
    }

    public void doElection(SpreadGroup[] members){
        System.out.println("doElection");
        if(Leader.checkIfLeader(members, myName)){
            sendMessage("Leader=" + myName);
        }
    }

    public void mergeAndSend(String tipo, String exchange, String bindingKey){
        Leader leader = new Leader(exchange, bindingKey, channel);
        leader.merge(tipo);
        sendMessage("Finished");
    }

    public void resumeOperations(){
        System.out.println("Resuming Operations");
        Main.initSale(channel, tipo);
        System.out.println("Resume complete.");
    }
}