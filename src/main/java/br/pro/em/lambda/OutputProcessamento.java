package br.pro.em.lambda;

public class OutputProcessamento {

    private String descricao;
    private String valor; 
    private String codigoTransacao;
    private String status = "PENDENTE";

    public OutputProcessamento() {
    }

    public OutputProcessamento(String identificador, String paymentData, String messageId, String status) {
        this.descricao = identificador;
        this.valor = paymentData;
        this.codigoTransacao = messageId;        
    }

    public String getDescricao() {
        return descricao;
    }

    public OutputProcessamento setDescricao(String identificador) {
        this.descricao = identificador;
        return this;
    }

    public String getValor() {
        return valor;
    }

    public OutputProcessamento setValor(String paymentData) {
        this.valor = paymentData;
        return this;
    }   

    public String getCodigoTransacao() {
        return codigoTransacao;
    }   

    public OutputProcessamento setCodigoTransacao(String messageId) {
        this.codigoTransacao = messageId;
        return this;
    }

    public String getStatus() {
        return status;
    }   
    
}
