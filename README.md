# lambda-queue

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

> POC de estudos: Lambda que recebe um pedido de pagamento e o publica em uma fila AWS SQS.

## PaymentProcessLambda

`PaymentProcessLambda` (handler `payment`) recebe um `InputModel`, publica o JSON dele na fila SQS e devolve um `OutputModel`.

Entrada (`InputModel`):

```json
{ "descricao": "Compra de teste", "valor": 100, "codigoTransacao": "TX-001" }
```

Saída (`OutputModel`):

```json
{ "identificador": "<uuid>", "paymentData": "{\"descricao\":...}", "messageId": "<id da mensagem no SQS>" }
```

### Configuração

| Variável de ambiente | Propriedade | Descrição | Padrão |
|---|---|---|---|
| `HANDLER_NAME` | `quarkus.lambda.handler` | Handler a executar: `payment` (produtora) ou `poller` (consumidora via `receiveMessage`) | `payment` |
| `PAYMENT_QUEUE_URL` | `payment.queue.url` | URL da fila SQS de destino | `http://localhost:4566/000000000000/payment-queue` |

Para usar o pagamento, defina `HANDLER_NAME=payment` e `PAYMENT_QUEUE_URL`. A região e as credenciais seguem a cadeia padrão da AWS (`aws configure`, variáveis `AWS_*` ou a role da Lambda). A role da Lambda precisa da permissão `sqs:SendMessage`.

Dependências adicionadas no `pom.xml`: `quarkus-amazon-sqs`, `url-connection-client` e `quarkus-jackson` (versões gerenciadas pelo `quarkus-amazon-services-bom`).

### Criando uma fila no AWS SQS (forma simples)

Pré-requisito: [AWS CLI](https://aws.amazon.com/cli/) configurado (`aws configure`).

```shell script
# cria uma fila Standard (padrão)
aws sqs create-queue --queue-name payment-queue --region us-east-1

# a saída traz a QueueUrl; para consultá-la depois:
aws sqs get-queue-url --queue-name payment-queue --region us-east-1
```

Copie a `QueueUrl` retornada (ex.: `https://sqs.us-east-1.amazonaws.com/123456789012/payment-queue`) e use em `PAYMENT_QUEUE_URL`.

Pelo console: **SQS → Create queue → tipo Standard → nome `payment-queue` → Create queue**, mantendo as demais opções padrão.

Para conferir se a mensagem chegou:

```shell script
aws sqs receive-message --queue-url <QueueUrl> --region us-east-1
```

Para apagar a fila ao final do estudo:

```shell script
aws sqs delete-queue --queue-url <QueueUrl> --region us-east-1
```

### Executando localmente

Defina as variáveis de ambiente conforme o terminal (valem apenas para a sessão atual):

PowerShell:

```powershell
$env:HANDLER_NAME="payment"
$env:PAYMENT_QUEUE_URL="<QueueUrl>"
./mvnw quarkus:dev
```

Git Bash:

```shell script
export HANDLER_NAME=payment
export PAYMENT_QUEUE_URL="<QueueUrl>"
./mvnw quarkus:dev
```

Com o dev mode ativo, invoque o handler em `http://localhost:8081`:

```shell script
curl -X POST http://localhost:8081 -H "Content-Type: application/json" -d '{"descricao":"Compra de teste","valor":100,"codigoTransacao":"TX-001"}'
```

Alternativa sem conta AWS: use o [LocalStack](https://localstack.cloud/) (`docker run -p 4566:4566 localstack/localstack`) e crie a fila com `aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name payment-queue`. Nesse caso, adicione `quarkus.sqs.endpoint-override=http://localhost:4566` e use credenciais/região fictícias.

### Deploy na AWS com `manage.sh`

O script `manage.sh` gera o pacote, cria ou atualiza a Lambda (runtime `java21`) e a invoca.

**Pré-requisito:** o AWS CLI precisa estar instalado e autenticado. Rode `aws configure` e informe `AWS Access Key ID`, `AWS Secret Access Key`, região (ex.: `us-east-1`) e formato de saída (`json`). Confirme o login com `aws sts get-caller-identity`.

**Antes do deploy é obrigatório configurar as variáveis de ambiente abaixo:**

| Variável | Obrigatória | Descrição | Padrão |
|---|---|---|---|
| `LAMBDA_ROLE_ARN` | Sim | ARN da role de execução da Lambda (precisa de `AWSLambdaBasicExecutionRole` e `sqs:SendMessage` na fila) | - |
| `HANDLER_NAME` | Não | `payment` ou `poller` | `payment` |
| `PAYMENT_QUEUE_URL` | Sim, se `HANDLER_NAME=payment` | URL da fila SQS | - |
| `AWS_REGION` | Não | Região da Lambda (use a mesma da fila) | `us-east-1` |
| `LAMBDA_FUNCTION_NAME` | Não | Nome da função | `lambda-queue` |
| `LAMBDA_TIMEOUT` / `LAMBDA_MEMORY_SIZE` | Não | Timeout (s) / memória (MB) | `15` / `512` |
| `PAYLOAD_FILE` | Não | Payload usado pelo `invoke` | `payload.json` |

Git Bash:

```shell script
export LAMBDA_ROLE_ARN='arn:aws:iam::<id-da-conta>:role/<nome-da-role>'
export HANDLER_NAME=payment
export PAYMENT_QUEUE_URL='https://sqs.us-east-1.amazonaws.com/<id-da-conta>/payment-queue'
bash manage.sh deploy
```

PowerShell (o script é bash; se `bash` estiver no PATH, ele herda as variáveis definidas na sessão):

```powershell
$env:LAMBDA_ROLE_ARN="arn:aws:iam::<id-da-conta>:role/<nome-da-role>"
$env:HANDLER_NAME="payment"
$env:PAYMENT_QUEUE_URL="https://sqs.us-east-1.amazonaws.com/<id-da-conta>/payment-queue"
bash manage.sh deploy
```

Comandos: `bash manage.sh build | deploy | invoke | delete`. Se `LAMBDA_ROLE_ARN` não estiver definida, o `deploy` falha com erro.

### Permissão da role para enviar mensagens ao SQS

A role de execução (`LAMBDA_ROLE_ARN`) precisa da permissão `sqs:SendMessage` na fila. Sem ela, o teste da Lambda falha com:

```
User: arn:aws:sts::<id-da-conta>:assumed-role/LambdaExecutionRole/lambda-queue is not authorized to perform: sqs:sendmessage on resource: arn:aws:sqs:us-east-1:<id-da-conta>:<nome-da-fila> (Status Code: 403)
```

Para corrigir, adicione uma policy inline à role (ajuste o nome da role, a região, o id da conta e o nome da fila).

AWS CLI (Git Bash):

```shell script
aws iam put-role-policy \
  --role-name LambdaExecutionRole \
  --policy-name PaymentQueueSendMessage \
  --policy-document '{
    "Version": "2012-10-17",
    "Statement": [{
      "Effect": "Allow",
      "Action": [
        "sqs:SendMessage",
        "sqs:ReceiveMessage",
        "sqs:DeleteMessage",
        "sqs:GetQueueAttributes"
      ],
      "Resource": "arn:aws:sqs:us-east-1:<id-da-conta>:<nome-da-fila>"
    }]
  }'
```

> **Atenção:** `put-role-policy` com o mesmo `--policy-name` **substitui** a policy existente. Mantenha todas as ações (envio e leitura) em uma única policy, ou use nomes diferentes; caso contrário a Lambda produtora perde o `sqs:SendMessage`.

Pelo console: **IAM → Roles → (sua role) → Add permissions → Create inline policy → JSON**, cole o mesmo documento (`Version` e `Statement`), dê um nome e salve.

A permissão vale em poucos segundos; basta repetir o teste da Lambda, sem novo deploy. O usuário do `aws configure` precisa ter `iam:PutRolePolicy`. Confira também se `PAYMENT_QUEUE_URL` aponta para a mesma fila usada no `Resource` da policy.
## PaymentReader (polling com receiveMessage)

`PaymentReader` (handler `poller`) busca as mensagens ativamente com `sqsClient.receiveMessage`, sem trigger SQS. Cada invocação lê até `payment.reader.max-messages` mensagens (padrão 1), com long polling de `payment.reader.wait-time-seconds` (padrão 5), loga, gera o `OutputProcessamento` e apaga a mensagem com `deleteMessage`.

Para rodar **automaticamente**, algo precisa invocá-la periodicamente: uma regra agendada do EventBridge (intervalo mínimo de 1 minuto).

### Deploy do poller

```shell script
export LAMBDA_ROLE_ARN='arn:aws:iam::<id-da-conta>:role/<nome-da-role>'
export HANDLER_NAME=poller
export LAMBDA_FUNCTION_NAME=lambda-poller
export PAYMENT_QUEUE_URL='https://sqs.us-east-1.amazonaws.com/<id-da-conta>/<nome-da-fila>'
bash manage.sh deploy
```

A role precisa de `sqs:ReceiveMessage` e `sqs:DeleteMessage` na fila (veja a policy da seção "Permissão da role" acima, que já inclui `ReceiveMessage`, `DeleteMessage` e `GetQueueAttributes`). Sem elas o erro é `not authorized to perform: sqs:receivemessage`.

### Agendamento (trigger EventBridge)

Pelo console:
1. **Lambda → lambda-poller → Add trigger**.
2. **Select a source:** `EventBridge (CloudWatch Events)`.
3. **Create a new rule**, nome `payment-poller-schedule`, tipo **Schedule expression**, valor `rate(1 minute)`.
4. Deixe habilitado e clique em **Add**.

Pela CLI:

```shell script
aws events put-rule --name payment-poller-schedule \
  --schedule-expression "rate(1 minute)" --region us-east-1

aws lambda add-permission --function-name lambda-poller \
  --statement-id payment-poller-schedule --action lambda:InvokeFunction \
  --principal events.amazonaws.com \
  --source-arn arn:aws:events:us-east-1:<id-da-conta>:rule/payment-poller-schedule \
  --region us-east-1

aws events put-targets --rule payment-poller-schedule --region us-east-1 \
  --targets "Id=1,Arn=arn:aws:lambda:us-east-1:<id-da-conta>:function:lambda-poller"
```

Os logs ficam em `/aws/lambda/lambda-poller`. Para parar o agendamento, desabilite a regra em **EventBridge → Rules → payment-poller-schedule → Disable** (ou `aws events disable-rule --name payment-poller-schedule`). O usuário do `aws configure` precisa de permissões `events:*` e `lambda:AddPermission`.

## Passo a passo pelo console AWS (fila, permissões e trigger)

Alternativa visual aos comandos da CLI. Use sempre a mesma região (ex.: `us-east-1`).

### 1. Criar a fila SQS

1. Console AWS → **Simple Queue Service (SQS)** → **Create queue**.
2. **Type:** `Standard`. **Name:** `payment-queue` (ou `PaymentProcessorQueue`).
3. Mantenha as demais opções padrão e clique em **Create queue**.
4. Na página da fila, copie a **URL** (usada em `PAYMENT_QUEUE_URL`) e o **ARN** (usado na policy).

### 2. Dar permissões à role da Lambda

1. Console AWS → **IAM → Roles** → abra a role da Lambda (ex.: `LambdaExecutionRole`).
2. **Add permissions → Create inline policy → aba JSON** e cole (troque região, conta e nome da fila):

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Action": [
      "sqs:SendMessage",
      "sqs:ReceiveMessage",
      "sqs:DeleteMessage",
      "sqs:GetQueueAttributes"
    ],
    "Resource": "arn:aws:sqs:us-east-1:<id-da-conta>:<nome-da-fila>"
  }]
}
```

3. **Next**, informe o nome `PaymentQueueAccess` e clique em **Create policy**.
4. Garanta também a policy gerenciada `AWSLambdaBasicExecutionRole` (logs no CloudWatch).

> Se já existir uma policy inline, edite-a e inclua todas as ações nela (ou use nomes diferentes), para não perder `sqs:SendMessage`.

### 3. Configurar as variáveis de ambiente da Lambda

**Lambda → (sua função) → Configuration → Environment variables → Edit** e defina:

| Função | `HANDLER_NAME` | `PAYMENT_QUEUE_URL` |
|---|---|---|
| Produtora (`lambda-queue`) | `payment` | URL da fila |
| Consumidora (`lambda-poller`) | `poller` | URL da fila |

Em **Runtime settings → Edit**, o handler deve ser `io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest`.

### 4. Agendar a consumidora (trigger)

1. **Lambda → lambda-poller → Add trigger**.
2. **Source:** `EventBridge (CloudWatch Events)` → **Create a new rule**.
3. **Rule name:** `payment-poller-schedule`; **Rule type:** `Schedule expression`; **Expression:** `rate(1 minute)`.
4. Clique em **Add**. Confira em **Configuration → Triggers** que o estado é `Enabled`.
5. Para parar, vá em **EventBridge → Rules → payment-poller-schedule → Disable**.

### 5. Testar

1. Na produtora, aba **Test**, evento: `{"descricao":"Compra de teste","valor":100,"codigoTransacao":"TX-001"}` → **Test**.
2. Em **SQS → sua fila → Send and receive messages**, veja a mensagem em **Messages available**.
3. Em até 1 minuto a `lambda-poller` roda; veja o log em **CloudWatch → Log groups → `/aws/lambda/lambda-poller`**. A mensagem é apagada da fila após o processamento.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/lambda-queue-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Related Guides

- AWS Lambda ([guide](https://quarkus.io/guides/aws-lambda)): Write AWS Lambda functions

## Provided Code

### Amazon Lambda Integration example

This example contains a Quarkus Greeting Lambda ready for Amazon.

[Related guide section...](https://quarkus.io/guides/aws-lambda)


