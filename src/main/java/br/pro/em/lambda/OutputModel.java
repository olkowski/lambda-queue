package br.pro.em.lambda;

public class OutputModel {

    private String identificador;
    private String paymentData; 
    private String messageId;
    public String getIdentificador() {
        return identificador;
    }

    public OutputModel setIdentificador(String identificador) {
        this.identificador = identificador;
        return this;
    }

    public String getPaymentData() {
        return paymentData;
    }

    public OutputModel setPaymentData(String paymentData) {
        this.paymentData = paymentData;
        return this;
    }

    public String getMessageId() {
        return messageId;
    }

    public OutputModel setMessageId(String messageId) {
        this.messageId = messageId;
        return this;
    }
    
    
}
