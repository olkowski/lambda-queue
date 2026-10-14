package br.pro.em.lambda;

public class InputModel {

    private String descricao;
    private int valor;
    private String codigoTransacao;
    

    public InputModel() {
    }

    public InputModel(String descricao, int valor, String codigoTransacao) {
        this.descricao = descricao;
        this.valor = valor;
        this.codigoTransacao = codigoTransacao;
    }

    public InputModel(String descricao, int valor) {
        this.descricao = descricao;
        this.valor = valor;
        this.codigoTransacao = null;
    }

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
