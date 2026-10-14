package br.pro.em.lambda;

import java.util.UUID;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named("paymentprocess")
public class PaymentProcessLambda implements RequestHandler<InputModel, OutputModel>  {

    @Inject
    private QueueService queueService;

    @Override
    public OutputModel handleRequest(InputModel input, Context context) {
        String codigoTransacao = UUID.randomUUID().toString();
        var response = queueService.sendMessage(input.getDescricao() + " - " + input.getValor() + " - " + codigoTransacao);
        
        OutputModel output = new OutputModel();
        output.setIdentificador(codigoTransacao);
        output.setPaymentData(input.getDescricao() + " - " + input.getValor());
        output.setMessageId(response.messageId());

        return output;
    }
    
}
