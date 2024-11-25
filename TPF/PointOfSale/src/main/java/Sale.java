import java.time.LocalDateTime;
import java.util.Random;

public class Sale {
    public String data;
    public String codigoProduto;
    public String nomeProduto;
    public double quantidade;
    public double precoUnitario;
    public double total;
    public int iva;

    public Sale(String codigoProduto,
                String nomeProduto,
                double quantidade,
                double precoUnitario,
                int iva){
        LocalDateTime dt = LocalDateTime.now();

        this.data = dt.toString();
        this.codigoProduto = codigoProduto;
        this.nomeProduto = nomeProduto;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario; //com iva já aplicado
        this.iva = iva;
        this.total = precoUnitario * quantidade;
        System.out.println("Total = " + total);
    }

    public Sale(String codigoProduto,
                String nomeProduto,
                double precoUnitario,
                boolean kilo){
        LocalDateTime dt = LocalDateTime.now();
        Random rd = new Random();

        this.data = dt.toString();
        this.codigoProduto = codigoProduto;
        this.nomeProduto = nomeProduto;
        if(kilo) this.quantidade = 0.5 + rd.nextInt(400)*0.01;
        else this.quantidade = 1 + rd.nextInt(10);
        this.precoUnitario = precoUnitario; //com iva já aplicado
        this.iva = rd.nextInt(24);
        this.total = precoUnitario * quantidade;
    }
}
