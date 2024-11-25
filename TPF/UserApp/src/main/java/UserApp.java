import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import usermanagerservice.Resume;
import usermanagerservice.Type;
import usermanagerservice.UserManagerServiceGrpc;

import java.util.*;
import java.io.*;

import java.text.SimpleDateFormat;
import java.util.Date;



public class UserApp {
    private static String managerIP = "localhost";
    private static int managerPort = 8080;
    private static ManagedChannel channel;
    private static UserManagerServiceGrpc.UserManagerServiceStub stub;
    static String fileName = "";

    public static void main(String[] args){
        try {
            if (args.length == 1) {
                managerIP = args[0];
                System.out.println(managerIP);
            }
            // Cria o canal gRPC para se conectar ao Manager Server
            channel = ManagedChannelBuilder
                    .forAddress(managerIP, managerPort)
                    .usePlaintext()
                    .build();

            // Cria o stub do serviço ManagerWorkerService
            stub = UserManagerServiceGrpc.newStub(channel);

            System.out.println("ManagerServer Connected");

            // Cria um streamObserver para receber os resumes
            StreamObserver<Resume> resumeObserver = new StreamObserver<>() {
                FileOutputStream fileOutputStream;
                @Override
                public void onNext(Resume resume) {
                    try {
                        // Verifica se é o primeiro resume recebido
                        // Nome do Ficheiro: (Type, Time, resume.getName)
                        fileOutputStream = new FileOutputStream(fileName + resume.getName());

                        System.out.println("FILENAME UNDER ME");
                        System.out.println(fileName + resume.getName());
                        // Escreve os dados do resume no ficheiro
                        fileOutputStream.write(resume.getData().toByteArray());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onError(Throwable t) {
                    t.printStackTrace();
                }
                @Override
                public void onCompleted() {
                    try {
                        // Fecha o ficheiro
                        if (fileOutputStream != null) {
                            fileOutputStream.close();
                        }
                        System.out.println("Resume recebido e guardado com sucesso!");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };

            boolean exit = true;
            while (exit) {
                switch (menu()) {
                    case 1:     // Obter Resumo;
                        // Escolha do tipo de Resumo
                        Type resumeType = Type.newBuilder()
                                .setType(typeChoice())
                                .build();
                        System.out.println("Será obtido um Resumo Tipo " + resumeType.getType());

                        // Preparar Nome do Ficheiro
                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                        String timeName  = dateFormat.format(new Date());
                        fileName = resumeType.getType() + "_" + timeName + "_";
                        fileName = fileName.replaceAll(":","-").replaceAll(" ", "-");

                        // Cria o stream de solicitação usando o stub do serviço ManagerWorkerService
                        stub.getResume(resumeType, resumeObserver);

                        // Aguarda alguns segundos para receber o resume (ou pode aguardar por um evento ou qualquer outra ação)
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        break;
                    case 99:    //terminar
                        // Fecha o canal gRPC
                        channel.shutdown();
                        exit = false;
                    default:
                        break;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static int menu() { // Menu
        int op;
        Scanner scan = new Scanner(System.in);
        do {
            System.out.println("\n    MENU");
            System.out.println(" 1 - Obter Resumo");
            System.out.println("99 - Exit");
            System.out.println("\nChoose an Option?");
            op = scan.nextInt();
        } while (!((op >= 1 && op <= 5) || op == 99));
        return op;
    }

    private static String typeChoice() {    // Choice a Type to resume

        int op;
        String[] choice = {"ALIMENTAR", "CASA"};
        Scanner scan = new Scanner(System.in);
        do{
            System.out.println("\n    RESUME TYPE CHOICE");
            System.out.println(" 0 - ALIMENTAR");
            System.out.println(" 1 - CASA");
            System.out.println("\nChoose an Option?");
            op = scan.nextInt();
        } while (!(op >= 0 && op <= 1));
        return choice[op];
    }
}
