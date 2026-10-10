package br.com.fiap.lambda;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

/**
 * Busca ativamente as mensagens da fila com receiveMessage (polling), sem trigger SQS.
 * Cada invocação lê até {@code payment.reader.max-messages} mensagens, loga, apaga e devolve o resultado.
 */
@Named("poller")
public class PaymentReader implements RequestHandler<JsonNode, List<OutputProcessamento>> {

    private static final Logger LOG = Logger.getLogger(PaymentReader.class);

    @Inject
    SqsClient sqsClient;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "payment.queue.url")
    String queueUrl;

    @ConfigProperty(name = "payment.reader.max-messages", defaultValue = "1")
    int maxMessages;

    @ConfigProperty(name = "payment.reader.wait-time-seconds", defaultValue = "5")
    int waitTimeSeconds;

    @Override
    public List<OutputProcessamento> handleRequest(JsonNode event, Context context) {
        List<Message> messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(maxMessages)
                .waitTimeSeconds(waitTimeSeconds)
                .build()).messages();

        LOG.infof("Mensagens recebidas da fila: %d", messages.size());

        List<OutputProcessamento> resultado = new ArrayList<>();
        for (Message message : messages) {
            LOG.infof("Mensagem recebida. messageId=%s, body=%s", message.messageId(), message.body());

            OutputProcessamento output = toOutput(message);
            LOG.infof("Pagamento processado. descricao=%s, valor=%s, codigoTransacao=%s",
                    output.getDescricao(), output.getValor(), output.getCodigoTransacao());
            resultado.add(output);

            // Sem trigger, a mensagem só sai da fila se for apagada explicitamente
            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build());
        }

        return resultado;
    }

    private OutputProcessamento toOutput(Message message) {
        try {
            InputModel input = objectMapper.readValue(message.body(), InputModel.class);
            return new OutputProcessamento()
                    .setDescricao(input.getDescricao())
                    .setValor(String.valueOf(input.getValor()))
                    .setCodigoTransacao(input.getCodigoTransacao())
                    .setStatus("PENDENTE");
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Mensagem inválida: " + message.messageId(), e);
        }
    }
}
