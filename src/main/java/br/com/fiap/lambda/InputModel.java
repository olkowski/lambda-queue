package br.com.fiap.lambda;

public class InputModel {

    private String descricao;
    private int valor;
    private String codigoTransacao;

    public String getDescricao() {
        return descricao;
    }

    public InputModel setDescricao(String descricao) {
        this.descricao = descricao;
        return this;
    }

    public int getValor() {
        return valor;
    }

    public InputModel setValor(int valor) {
        this.valor = valor;
        return this;
    }

    public String getCodigoTransacao() {
        return codigoTransacao;
    }

    public InputModel setCodigoTransacao(String codigoTransacao) {
        this.codigoTransacao = codigoTransacao;
        return this;
    }
}
