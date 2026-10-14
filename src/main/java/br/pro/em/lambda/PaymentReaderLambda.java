package br.pro.em.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import software.amazon.awssdk.services.sqs.model.Message;

@Named("paymentreader")
public class PaymentReaderLambda implements RequestHandler<Object, OutputProcessamento> {

    @Inject
    private QueueService queueService;

    @Override
    public OutputProcessamento handleRequest(Object o, Context context) {
        
        LambdaLogger logger = context.getLogger();
        
        Message message = queueService.receiveMessage();
        if (message == null) {
            return null;
        }


        logger.log("Message received: " + message.body());
        var processed = message.body();
        logger.log("Message processed: " + processed);

        var msgSplit = processed.split(" - ");
        var descricao = msgSplit[0];
        var valor = msgSplit[1];
        var codigoTransacao = msgSplit[2];

        OutputProcessamento output = new OutputProcessamento();        
        output.setDescricao(descricao);
        output.setValor(valor);
        output.setCodigoTransacao(codigoTransacao);

        return output;
    }
    
}
