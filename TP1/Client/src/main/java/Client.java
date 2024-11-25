import clientregisterservice.ClientRegisterServiceGrpc;
import clientregisterservice.*;

import clientserverservice.ClientServerServiceGrpc;
import clientserverservice.*;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Client {

    private static final int MAXBYTES = 32000;

    static class ImageReceivingObserver implements StreamObserver<Image>{

        boolean first;
        DataOutputStream data;
        boolean completed;

        ImageReceivingObserver(){
            first = true;
            completed = false;
        }

        @Override
        public void onNext(Image image) {
            if (first){
                String name = image.getName();
                File file = new File(name);
                try {
                    if (!file.createNewFile()) System.out.println("File exists already.");
                } catch (IOException e) {
                    System.out.println("ERROR CREATING FILE");
                    throw new RuntimeException(e);
                }
                try {
                    data = new DataOutputStream(new FileOutputStream(file));
                } catch (FileNotFoundException e) {
                    System.out.println("Error in stream for file writing creating");
                    throw new RuntimeException(e);
                }
                first = false;
            }
            try {
                data.write(image.getData().toByteArray());
            } catch (IOException e) {
                System.out.println("Error in writing");
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            throwable.printStackTrace();
        }

        @Override
        public void onCompleted() {
            try {
                data.close();
                System.out.println("File received completly");
                completed = true;
            } catch (IOException e) {
                System.out.println("Error on onCompleted of ImageReceivingObserver");
                throw new RuntimeException(e);
            }
        }
    }

    static class ImageIdObserver implements StreamObserver<ImageId> {

        public boolean received = false;
        int svId;

        ImageIdObserver(int svId){
            this.svId = svId;
        }
        @Override
        public void onNext(ImageId imageId) {
            // Recebe o ID da imagem processada
            System.out.println("Image to process, ImageId: " + imageId.getId());

            // Guardar informação de imagem e servidor
            imageList.add(imageId);
            serverWithImage.add(serverList.get(svId));
        }

        @Override
        public void onError(Throwable throwable) {
            throwable.printStackTrace();
        }

        @Override
        public void onCompleted() {
            // Finalização da comunicação
            System.out.println("Image sent successfully");
            received = true;
        }
    }



    private static String svcIP = "localhost";
    private static int svcPort = 8500;

    private static ManagedChannel serverChannel;
    private static ClientRegisterServiceGrpc.ClientRegisterServiceBlockingStub registerBlockingStub;
    private static ClientServerServiceGrpc.ClientServerServiceStub serverStub;
    private static ClientServerServiceGrpc.ClientServerServiceBlockingStub serverBlockingStub;

    private static ArrayList<Server> serverList = new ArrayList<>(); // Lista de Servers conhecidos

    private static ArrayList<ImageId> imageList = new ArrayList<>(); // Lista id de imagens
    private static ArrayList<Server> serverWithImage = new ArrayList<>(); // Lista de Servers com imagens



    public static void main(String[] args) {
        try {
            if (args.length == 2) {
                svcIP = args[0];
                svcPort = Integer.parseInt(args[1]);
            }
            System.out.println("Connect to Register " + svcIP + ":" + svcPort);
            ManagedChannel channel = ManagedChannelBuilder.forAddress(svcIP, svcPort).usePlaintext().build();
            registerBlockingStub = ClientRegisterServiceGrpc.newBlockingStub(channel);

            ImageId toCheck;
            boolean exit = true;
            int imId;
            while (exit) {
                switch (menu()) {
                    case 1:     // getServer();
                        // obter servidor
                        Server sv = registerBlockingStub.getServer(Nothing.newBuilder().build());
                        System.out.println("Server " + sv.getIp() + ":" + sv.getPort());

                        // Guardar informação na lista de servidores conhecidos
                        if(serverList.contains(sv)){
                            System.out.println("O Servidor já era conhecido");
                        } else {
                            serverList.add(sv);
                            System.out.println("O Servidor foi adicionado à lista de conhecidos");
                        }

                        break;

                    case 2:     //processImage();
                        // Escolha e coneção ao servidor
                        int svId;
                        do{
                            svId = serverChoice();
                        }while (svId != -1 && !checkServerStatus(svId));
                        if(svId==-1) break;

                        serverStub = ClientServerServiceGrpc.newStub(serverChannel);
                        sendImage(svId);

                        break;

                    case 3:     //checkImageStatus();
                        // Verificar se não existem imagens enviadas
                        if(imageList.isEmpty()){
                            System.out.println("Não existem imagens enviadas");
                            break;
                        }

                        // Escolher Imagem e verificar se o servidor ainda está disponivel.
                        imId = imageChoice();
                        if(!checkServerStatus(serverList.indexOf(serverWithImage.get(imId)))) break;
                        serverBlockingStub = ClientServerServiceGrpc.newBlockingStub(serverChannel);

                        // Obter estado da imagem
                        toCheck = ImageId.newBuilder().setId(imageList.get(imId).getId()).build();
                        ImageStatus imageStatus = serverBlockingStub.checkImageStatus(toCheck);
                        System.out.println("Image status: " + imageStatus.getStatus());

                        break;

                    case 4:     //getProcessedImage();
                        // Verificar se não existem imagens enviadas
                        if(imageList.isEmpty()){
                            System.out.println("Não existem imagens enviadas");
                            break;
                        }

                        // Escolher Imagem e verificar se o servidor ainda está disponivel.
                        imId = imageChoice();
                        if(!checkServerStatus(serverList.indexOf(serverWithImage.get(imId)))) break;
                        serverStub = ClientServerServiceGrpc.newStub(serverChannel);

                        // Obter imagem processada
                        toCheck = ImageId.newBuilder().setId(imageList.get(imId).getId()).build();
                        ImageReceivingObserver obs = new ImageReceivingObserver();
                        serverStub.getProcessedImage(toCheck, obs);
                        while(!obs.completed){
                            System.out.println("Awaiting completion");
                            Thread.sleep(1000);
                        }
                        System.out.println("Processed image received");
                        break;

                    case 5: //send keywords
                        // Verificar se não existem imagens enviadas
                        if(imageList.isEmpty()){
                            System.out.println("Não existem imagens enviadas");
                            break;
                        }

                        // Escolher Imagem e verificar se o servidor ainda está disponivel.
                        imId = imageChoice();
                        if(!checkServerStatus(serverList.indexOf(serverWithImage.get(imId)))) break;
                        serverBlockingStub = ClientServerServiceGrpc.newBlockingStub(serverChannel);

                        Keywords keys = askKeywords(imId);
                        Confirmation conf = serverBlockingStub.keywordList(keys);
                        if(conf.getValid()){
                            System.out.println("Keyword List accepted");
                        } else {
                            System.out.println("Error in Keyword List" + "\n" + conf.getError());
                        }
                        break;

                    case 99:    //terminar
                        exit = false;

                    default:
                        break;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static int menu() {
        int op;
        Scanner scan = new Scanner(System.in);
        do {
            System.out.println("\n    MENU");
            System.out.println(" 1 - Case 1 - Obter a localização de um servidor");
            System.out.println(" 2 - Case 2 - Enviar uma ou mais imagens para processar");
            System.out.println(" 3 - Case 3 - Verificar se a imagem já foi processada");
            System.out.println(" 4 - Case 4 - Obter imagem processada");
            System.out.println(" 5 - Case 5 - Enviar keywords a serem marcadas numa imagem processada");
            System.out.println("99 - Exit");
            System.out.println("\nChoose an Option?");
            op = scan.nextInt();
        } while (!((op >= 1 && op <= 5) || op == 99));
        return op;
    }

    private static int serverChoice() {
        if (serverList.isEmpty()){
            System.out.println("Nenhum servidor disponivel. Por favor obtenha primeiro um servidor.");
            return -1;
        }
        int op;
        Scanner scan = new Scanner(System.in);
        System.out.println();
        System.out.println("SERVER CHOICE");
        for (int i = 0; i < serverList.size(); i++) {
            System.out.println("Escolha " + i + " Server " + serverList.get(i).getIp() + ":" + serverList.get(i).getPort());
        }
        do{
            System.out.println("\nChoose an Option?");
            op = scan.nextInt();
        }while (!(op >= 0 && op <= serverList.size()-1));
        return op;
    }

    private static void serverConnect(Server sv){
        if (serverChannel != null) {
            serverChannel.shutdown();
            try {
                while (!serverChannel.awaitTermination(1000, TimeUnit.MILLISECONDS)) ;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        String svIP = sv.getIp();
        int svPort = sv.getPort();
        System.out.println("Connect to Server " + svIP + ":" + svPort);
        serverChannel = ManagedChannelBuilder.forAddress(svIP, svPort).usePlaintext().build();
    }

    private static int imageChoice() {
        int op;
        Scanner scan = new Scanner(System.in);
        System.out.println("\nIMAGE CHOICE");
        for (int i = 0; i < imageList.size(); i++) {
            System.out.println("Escolha " + i + ": Image " + imageList.get(i).getId() + ", Server " +
                    serverWithImage.get(i).getIp() + ":" + serverWithImage.get(i).getPort());
        }
        do{
            System.out.println("\nChoose an Option?");
            op = scan.nextInt();
        }while (!(op >= 0 && op <= imageList.size()-1));
        return op;
    }

    private static Keywords askKeywords(int imId) {
        String imgId = imageList.get(imId).getId();
        System.out.println("Escreva, separadas por espaços, as palavras a serem marcadas na imagem");
        Scanner scanner = new Scanner(System.in);
        String toSplit = scanner.nextLine();
        String[] words = toSplit.split(" ");
        return Keywords.newBuilder().setId(imgId).addAllKeywords(Arrays.asList(words)).build();
    }

    private static void sendImage(int svId) throws IOException {

        // StreamObserver
        ImageIdObserver imageIdObserver = new ImageIdObserver(svId);

        // Escolher imagem
        Scanner scan = new Scanner(System.in);
        System.out.println("\nEscreva o nome da Imagem que pretende processar (incluindo .png/.jpg ou outro)");
        String nameImage = scan.nextLine();

        // Verifica se a imagem existe
        File imageFile = Path.of(nameImage).toFile();
        System.out.println(imageFile.getAbsolutePath());
        if(!imageFile.exists()){
            System.out.println("ERROR: File not found. Returning to Menu");
            return;
        }

        // Envia cada linha da imagem como um bloco para o servidor
        StreamObserver<Image> imageObserver = serverStub.processImage(imageIdObserver);
        DataInputStream dt = new DataInputStream(new FileInputStream(imageFile));
        byte[] read;
        do {
            read = dt.readNBytes(MAXBYTES);
            Image image = Image.newBuilder()
                    .setName(imageFile.getName())
                    .setData(ByteString.copyFrom(read))
                    .build();
            imageObserver.onNext(image);
        } while (read.length >= MAXBYTES);
        dt.close();

        // Completa o envio da imagem
        imageObserver.onCompleted();

        // Espera pelo ImageId
        while (!imageIdObserver.received){
            System.out.println("Waiting for ImageId");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println("ImageId obtained. Upload sucessfull");
    }

    private static boolean checkServerStatus(int svid){
        serverConnect(serverList.get(svid));
        ClientServerServiceGrpc.ClientServerServiceBlockingStub stub = ClientServerServiceGrpc.newBlockingStub(serverChannel);
        try{
            Nada nd = stub.ping(Nada.newBuilder().build());
            return true;
        } catch (Exception exception){
            System.out.println("O servidor ficou indisponivel. Serão removidas todos os pedidos para o mesmo");
            Nothing nd = registerBlockingStub.reportBadServer(serverList.get(svid));
            int id = serverWithImage.indexOf(serverList.get(svid));
            while(id!=-1){
                imageList.remove(id);
                serverWithImage.remove(id);
                id = serverWithImage.indexOf(serverList.get(svid));
            }
            serverList.remove(svid);
            return false;
        }
    }
}
