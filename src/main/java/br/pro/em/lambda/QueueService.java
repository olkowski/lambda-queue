package br.pro.em.lambda;

import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;

@ApplicationScoped
public class QueueService {

    private SqsClient sqsClient;

    @ConfigProperty(name = "sqs.queue.url")
    String queueUrl;

    public QueueService() {
        this.sqsClient = SqsClient.builder()
                .region(Region.US_EAST_1)
                .build();
    }

    public SendMessageResponse sendMessage(String messageBody) {
        SendMessageResponse response = sqsClient.sendMessage(m -> m.queueUrl(queueUrl).messageBody(messageBody));
        return response;        
    }

    public Message receiveMessage() {
        List<Message> messages = sqsClient.receiveMessage(r -> r.queueUrl(queueUrl).maxNumberOfMessages(1)).messages();
        if (messages.size() > 0) {
            sqsClient.deleteMessage(d -> d.queueUrl(queueUrl).receiptHandle(messages.get(0).receiptHandle()));
            return messages.get(0);
        }
        return null;
    }
}
