public class Sale {
    public String data;
    public String codigoProduto;
    public String nomeProduto;
    public double quantidade;
    public double precoUnitario;
    public double total;
    public int iva;

    public Sale(String data,
                String codigoProduto,
                String nomeProduto,
                double quantidade,
                double precoUnitario,
                double total,
                int iva){
        this.data = data;
        this.codigoProduto = codigoProduto;
        this.nomeProduto = nomeProduto;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario; //com iva já aplicado
        this.iva = iva;
        this.total = total;
    }

    public String toString(){
        String quant = String.format("%.4f", quantidade);
        String precoUni = String.format("%.4f", precoUnitario);
        String tudo = String.format("%.4f", total);
        return data + "|" + nomeProduto + "|" + codigoProduto + "|" + quant + "|" + precoUni + "|" + iva  + "|" + tudo;
    }
}
