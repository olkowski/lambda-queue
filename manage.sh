#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$SCRIPT_DIR"

LAMBDA_FUNCTION_NAME="${LAMBDA_FUNCTION_NAME:-lambda-queue}"
AWS_REGION="${AWS_REGION:-us-east-1}"
LAMBDA_TIMEOUT="${LAMBDA_TIMEOUT:-15}"
LAMBDA_MEMORY_SIZE="${LAMBDA_MEMORY_SIZE:-512}"
HANDLER_NAME="${HANDLER_NAME:-payment}"
PAYMENT_QUEUE_URL="${PAYMENT_QUEUE_URL:-}"
PAYLOAD_FILE="${PAYLOAD_FILE:-$PROJECT_DIR/payload.json}"
ZIP_FILE="$PROJECT_DIR/target/function.zip"
DEFAULT_HANDLER="io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest"

log() {
  echo "[manage.sh] $*"
}

# AWS CLI do Windows nao entende caminhos /c/... do Git Bash
to_aws_path() {
  if command -v cygpath >/dev/null 2>&1; then
    cygpath -m "$1"
  else
    echo "$1"
  fi
}

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Erro: comando '$1' nao encontrado no PATH." >&2
    exit 1
  fi
}

require_role_arn() {
  if [[ -z "${LAMBDA_ROLE_ARN:-}" ]]; then
    echo "Erro: variavel LAMBDA_ROLE_ARN nao definida." >&2
    echo "Exemplo:" >&2
    echo "  export LAMBDA_ROLE_ARN='arn:aws:iam::123456789012:role/LambdaExecutionRole'" >&2
    exit 1
  fi
}

validate_handler_config() {
  case "$HANDLER_NAME" in
    greeting)
      ;;
    payment|poller)
      if [[ -z "$PAYMENT_QUEUE_URL" ]]; then
        echo "Erro: defina PAYMENT_QUEUE_URL para usar o handler '$HANDLER_NAME'." >&2
        exit 1
      fi
      ;;
    *)
      echo "Erro: HANDLER_NAME deve ser 'greeting', 'payment' ou 'poller'." >&2
      exit 1
      ;;
  esac
}

lambda_environment() {
  if [[ -n "$PAYMENT_QUEUE_URL" ]]; then
    printf 'Variables={HANDLER_NAME=%s,PAYMENT_QUEUE_URL=%s}' \
      "$HANDLER_NAME" "$PAYMENT_QUEUE_URL"
  else
    printf 'Variables={HANDLER_NAME=%s}' "$HANDLER_NAME"
  fi
}

build_zip() {
  if [[ ! -f "$PROJECT_DIR/mvnw" ]]; then
    echo "Erro: Maven Wrapper nao encontrado em $PROJECT_DIR/mvnw" >&2
    exit 1
  fi

  log "Gerando pacote da Lambda com Maven/Quarkus..."
  (
    cd "$PROJECT_DIR"
    tr -d '\r' < "$PROJECT_DIR/mvnw" |
      bash -c 'source /dev/stdin' "$PROJECT_DIR/mvnw" -q clean package -DskipTests
  )

  if [[ ! -f "$ZIP_FILE" ]]; then
    echo "Erro: arquivo nao encontrado em $ZIP_FILE" >&2
    echo "Verifique se o build Quarkus gerou target/function.zip." >&2
    exit 1
  fi

  log "Pacote pronto: $ZIP_FILE"
}

lambda_exists() {
  aws lambda get-function \
    --function-name "$LAMBDA_FUNCTION_NAME" \
    --region "$AWS_REGION" \
    >/dev/null 2>&1
}

create_lambda() {
  require_role_arn
  local zip_path
  zip_path="$(to_aws_path "$ZIP_FILE")"

  log "Criando funcao '$LAMBDA_FUNCTION_NAME' na regiao '$AWS_REGION'..."
  aws lambda create-function \
    --function-name "$LAMBDA_FUNCTION_NAME" \
    --runtime java21 \
    --handler "$DEFAULT_HANDLER" \
    --role "$LAMBDA_ROLE_ARN" \
    --zip-file "fileb://$zip_path" \
    --timeout "$LAMBDA_TIMEOUT" \
    --memory-size "$LAMBDA_MEMORY_SIZE" \
    --environment "$(lambda_environment)" \
    --region "$AWS_REGION"

  log "Funcao criada com sucesso."
}

update_lambda() {
  require_role_arn
  local zip_path
  zip_path="$(to_aws_path "$ZIP_FILE")"

  log "Atualizando codigo da funcao '$LAMBDA_FUNCTION_NAME'..."
  aws lambda update-function-code \
    --function-name "$LAMBDA_FUNCTION_NAME" \
    --zip-file "fileb://$zip_path" \
    --region "$AWS_REGION" \
    >/dev/null

  aws lambda wait function-updated \
    --function-name "$LAMBDA_FUNCTION_NAME" \
    --region "$AWS_REGION"

  log "Atualizando configuracao da funcao '$LAMBDA_FUNCTION_NAME'..."
  aws lambda update-function-configuration \
    --function-name "$LAMBDA_FUNCTION_NAME" \
    --runtime java21 \
    --handler "$DEFAULT_HANDLER" \
    --role "$LAMBDA_ROLE_ARN" \
    --timeout "$LAMBDA_TIMEOUT" \
    --memory-size "$LAMBDA_MEMORY_SIZE" \
    --environment "$(lambda_environment)" \
    --region "$AWS_REGION" \
    >/dev/null

  aws lambda wait function-updated \
    --function-name "$LAMBDA_FUNCTION_NAME" \
    --region "$AWS_REGION"

  log "Funcao atualizada com sucesso."
}

invoke_lambda() {
  require_command aws
  local output_file="$PROJECT_DIR/response.json"
  local payload_aws_path
  payload_aws_path="$(to_aws_path "$PAYLOAD_FILE")"
  local output_aws_path
  output_aws_path="$(to_aws_path "$output_file")"

  if [[ ! -f "$PAYLOAD_FILE" ]]; then
    echo "Erro: payload nao encontrado em $PAYLOAD_FILE" >&2
    exit 1
  fi

  log "Invocando '$LAMBDA_FUNCTION_NAME' com '$PAYLOAD_FILE'..."
  aws lambda invoke \
    --function-name "$LAMBDA_FUNCTION_NAME" \
    --payload "fileb://$payload_aws_path" \
    --region "$AWS_REGION" \
    "$output_aws_path" \
    >/dev/null

  log "Resposta gravada em $output_file"
}

delete_lambda() {
  require_command aws

  log "Removendo funcao '$LAMBDA_FUNCTION_NAME'..."
  aws lambda delete-function \
    --function-name "$LAMBDA_FUNCTION_NAME" \
    --region "$AWS_REGION"

  log "Funcao removida."
}

usage() {
  cat <<EOF
Uso: ./manage.sh <comando>

Comandos:
  build   - Gera target/function.zip
  deploy  - Faz build e cria/atualiza a Lambda
  invoke  - Invoca a Lambda usando PAYLOAD_FILE (padrao: payload.json)
  delete  - Remove a Lambda
  help    - Exibe esta ajuda

Variaveis de ambiente:
  LAMBDA_ROLE_ARN      (obrigatoria para deploy)
  LAMBDA_FUNCTION_NAME (padrao: lambda-queue)
  AWS_REGION           (padrao: us-east-1)
  LAMBDA_TIMEOUT       (padrao: 15)
  LAMBDA_MEMORY_SIZE   (padrao: 512)
  HANDLER_NAME         (greeting, payment ou poller; padrao: payment)
  PAYMENT_QUEUE_URL    (obrigatoria em payment/poller)
  PAYLOAD_FILE         (caminho usado por invoke; padrao: payload.json)

Runtime Lambda: java21 (alinhado ao Java 21 configurado no pom.xml).
EOF
}

main() {
  local command="${1:-help}"

  case "$command" in
    build)
      build_zip
      ;;
    deploy)
      require_command aws
      validate_handler_config
      build_zip
      if lambda_exists; then
        update_lambda
      else
        create_lambda
      fi
      ;;
    invoke)
      invoke_lambda
      ;;
    delete)
      delete_lambda
      ;;
    help|--help|-h)
      usage
      ;;
    *)
      echo "Comando invalido: $command" >&2
      usage
      exit 1
      ;;
  esac
}

main "$@"
