import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class PointOfSale {

    private static final String[] FRUTAS = {"maça", "pera", "banana", "ananás", "pessego", "diospiro", "kiwi"};
    private static final String[] LACTICINIOS = {"ovos", "leite de vaca", "leite de soja"};
    private static final String[] CARNES = {"peito de frango", "pernas de frango", "frango do campo inteiro", "carne picada", "bife de vaca", "bife de peru"};
    private static final String[] CASA = {"limpa-vidros", "pano limpar pó", "lençol", "capa de almofada pequena",
            "almofada pequena", "capa de almofada média", "almofada média", "capa de almofada grande", "almofada grande",
            "esfregona", "filtro de aspirador", "vaso pequeno", "vaso médio", "vaso grande"};

    private static String IP_BROKER;
    private static String EXCHANGE_NAME = "ExgSales";
    private static String ALIMENTAR_KEY = "ALIMENTAR.";
    private static String CASA_KEY = "CASA.";

    public static void main(String[] args) {
        if(args.length > 0) {
            IP_BROKER = args[0];
        }
        else {
            System.out.println("Missing arguments, please specify broker IP address");
            System.exit(0);
        }
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(IP_BROKER);
            factory.setPort(5672);
            Connection c = factory.newConnection();
            Channel channel = c.createChannel();
            channel.addReturnListener(new ReturnedMessage());
            Scanner s = new Scanner(System.in);
            while(true){
                switch (menu()){
                    case 1:
                        sale(channel, s, ALIMENTAR_KEY);
                        break;
                    case 2:
                        sale(channel, s, CASA_KEY);
                        break;
                    case 3:
                        multipleSales(channel, s, ALIMENTAR_KEY);
                        break;
                    case 4:
                        multipleSales(channel, s, CASA_KEY);
                        break;
                    case 99:
                        channel.close();
                        c.close();
                        return;
                    default:
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int menu() {
        int op;
        Scanner scan = new Scanner(System.in);
        do {
            System.out.println();
            System.out.println("    MENU");
            System.out.println(" 1 - Registar produto alimentar");
            System.out.println(" 2 - Registar produto de casa");
            System.out.println(" 3 - Enviar multiplos produtos Alimentares");
            System.out.println(" 4 - Enviar multiplos produtos de Casa");
            System.out.println("99 - Exit");
            System.out.println();
            System.out.println("Choose an Option?");
            op = scan.nextInt();
        } while (!((op >= 1 && op <= 4) || op == 99));
        return op;
    }

    private static void sale(Channel channel, Scanner s, String key) throws IOException {
        System.out.println("Preencha a informação do produto");
        System.out.print("Nome: ");
        String nomeProduto = s.nextLine();
        System.out.print("Subcategoria: ");
        String subcategoria = s.nextLine();
        System.out.print("Código: ");
        String codigoProduto = s.nextLine();
        System.out.print("Quantidade: ");
        double quantidade = s.nextDouble();
        System.out.print("Preço por unidade: ");
        double precoUnitario = s.nextDouble();
        System.out.print("Iva aplicado ao produto: ");
        int iva = s.nextInt();
        Sale sale = new Sale(codigoProduto, nomeProduto, quantidade, precoUnitario, iva);
        channel.basicPublish(EXCHANGE_NAME, key + subcategoria, true, null, toBytes(sale));
    }

    private static byte[] toBytes(Sale sale){
        Gson gson = new GsonBuilder().create();
        String jsonString = gson.toJson(sale);
        System.out.println(jsonString);
        return jsonString.getBytes(StandardCharsets.UTF_8);
    }

    private static void multipleSales(Channel channel, Scanner s, String key) throws IOException {
        System.out.println("Serão geradas 10 vendas.");
        Random rd = new Random();
        ArrayList<Sale> sales = new ArrayList<>();
        if (key.equals(ALIMENTAR_KEY)){
            sales.add(new Sale("teste", FRUTAS[rd.nextInt(FRUTAS.length)], rd.nextInt(15)*0.25, true));
            sales.add(new Sale("teste", FRUTAS[rd.nextInt(FRUTAS.length)], rd.nextInt(15)*0.25, true));
            sales.add(new Sale("teste", FRUTAS[rd.nextInt(FRUTAS.length)], rd.nextInt(15)*0.25,  true));
            sales.add(new Sale("teste", FRUTAS[rd.nextInt(FRUTAS.length)], rd.nextInt(15)*0.25, true));
            sales.add(new Sale("teste", FRUTAS[rd.nextInt(FRUTAS.length)], rd.nextInt(15)*0.25, true));
            sales.add(new Sale("teste", LACTICINIOS[rd.nextInt(LACTICINIOS.length)], rd.nextInt(20)*0.25, false));
            sales.add(new Sale("teste", LACTICINIOS[rd.nextInt(LACTICINIOS.length)], rd.nextInt(20)*0.25, false));
            sales.add(new Sale("teste", CARNES[rd.nextInt(CARNES.length)], rd.nextInt(100)*0.25+6, true));
            sales.add(new Sale("teste", CARNES[rd.nextInt(CARNES.length)], rd.nextInt(100)*0.25+6, true));
            sales.add(new Sale("teste", CARNES[rd.nextInt(CARNES.length)], rd.nextInt(100)*0.25+6, true));
        } else {
            for (int i = 0; i < 10; i++) {
                sales.add(new Sale("teste", CASA[rd.nextInt(CASA.length)], (rd.nextInt(20)+1)*5-0.01, false));
            }
        }
        for (Sale sal: sales) {
            channel.basicPublish(EXCHANGE_NAME, key + "teste", true, null, toBytes(sal));
        }
    }
}