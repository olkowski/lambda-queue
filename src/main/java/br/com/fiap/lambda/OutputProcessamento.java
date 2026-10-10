package br.com.fiap.lambda;

public class OutputProcessamento {

    private String descricao;
    private String valor;
    private String codigoTransacao;
    private String status;

    public String getDescricao() {
        return descricao;
    }

    public OutputProcessamento setDescricao(String descricao) {
        this.descricao = descricao;
        return this;
    }

    public String getValor() {
        return valor;
    }

    public OutputProcessamento setValor(String valor) {
        this.valor = valor;
        return this;
    }

    public String getCodigoTransacao() {
        return codigoTransacao;
    }

    public OutputProcessamento setCodigoTransacao(String codigoTransacao) {
        this.codigoTransacao = codigoTransacao;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public OutputProcessamento setStatus(String status) {
        this.status = status;
        return this;
    }
}
