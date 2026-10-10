package br.com.fiap.lambda;

import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@Named("payment")
public class PaymentProcessLambda implements RequestHandler<InputModel, OutputModel> {

    @Inject
    SqsClient sqsClient;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "payment.queue.url")
    String queueUrl;

    @Override
    public OutputModel handleRequest(InputModel input, Context context) {
        String identificador = UUID.randomUUID().toString();
        String paymentData = toJson(input);

        SendMessageResponse response = sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(paymentData)
                .build());

        return new OutputModel()
                .setIdentificador(identificador)
                .setPaymentData(paymentData)
                .setMessageId(response.messageId());
    }

    private String toJson(InputModel input) {
        try {
            return objectMapper.writeValueAsString(input);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Pedido de pagamento inválido", e);
        }
    }
}
