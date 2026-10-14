# delivery-system · Projeto Diamante 02

Sistema de pedidos de delivery com microsserviços (Java Advanced, 2º semestre).
Cenário: promoção relâmpago de sexta-feira às 20h, com estoque limitado, pagamento instável,
avaliações em massa e um assistente de IA que responde dúvidas sobre o cardápio.

## Integrantes

| Nome | RM |
|------|----|
| _Lucca Ramos Mussumecci_ | _RM562027_ |
| _Pedro Peres Benitez_ | _RM561792_ |


## Arquitetura

| Serviço | Porta | Responsabilidade |
|---|---|---|
| `eureka-server` | 8761 | Registro de serviços |
| `order-service` | 8080 | Cardápio, pedidos com lock pessimista, publicação de avaliações, assistente IA, rate limit |
| `payment-service` | 8081 / 8082 | Pagamento instável (~50% de erro 500), duas instâncias |
| `review-service` | 8083 | Consome avaliações, buffer em memória, flush a cada 5 s, ranking |
| RabbitMQ (Docker) | 5672 / 15672 | `delivery.exchange` → `reviews.new` → `reviews.queue` |

```
App React Native ──REST──▶ order-service ──LB + retry──▶ PAYMENT-SERVICE (8081 | 8082)
                              │   │
                              │   └──AMQP──▶ delivery.exchange ─reviews.new─▶ reviews.queue ──▶ review-service
                              └──ChatClient──▶ OpenAI ou modelo local
        (todos os serviços se registram no eureka-server pelo spring.application.name)
```

Stack: Java 25, Spring Boot 4.0.8, Spring Cloud 2025.1.3, Spring AI 2.0.1, Gradle 9.8.1 (wrapper incluso), H2, RabbitMQ 4.

## Pré-requisitos

- JDK 25 (se não estiver instalado, o Gradle baixa automaticamente via toolchain/foojay)
- Docker com Docker Compose
- Uma chave da OpenAI **ou** um modelo local (LM Studio / Ollama) para o assistente

## Configurando o assistente de IA (sem expor a chave)

A chave nunca vai para o repositório. Escolha uma das opções:

1. **Variável de ambiente**
   - Linux/macOS: `export OPENAI_API_KEY=sk-...`
   - PowerShell: `$env:OPENAI_API_KEY="sk-..."`
2. **Arquivo `.env`** (já está no `.gitignore`): copie `.env.example` para `order-service/.env` e preencha.

Para usar um **modelo local**, defina também `OPENAI_BASE_URL` (com `/v1` no final) e `OPENAI_MODEL`:

| Provedor | OPENAI_BASE_URL |
|---|---|
| OpenAI (padrão) | `https://api.openai.com/v1` |
| LM Studio | `http://localhost:1234/v1` |
| Ollama | `http://localhost:11434/v1` |

Sem chave configurada, todo o resto do sistema funciona normalmente; apenas `POST /assistant` responde 502.

## Como executar

Cada comando em um terminal separado, a partir da raiz do repositório (no Windows use `gradlew.bat`):

```bash
docker compose up -d                                                     # 1. RabbitMQ
./gradlew :eureka-server:bootRun                                         # 2. Eureka (aguarde subir)
./gradlew :payment-service:bootRun                                       # 3. Pagamento, instância 8081
./gradlew :payment-service:bootRun --args='--server.port=8082'           # 4. Pagamento, instância 8082
./gradlew :review-service:bootRun                                        # 5. Avaliações
./gradlew :order-service:bootRun                                         # 6. Pedidos
```

Também funciona de dentro de cada pasta: `cd payment-service && ../gradlew bootRun --args='--server.port=8082'`.

No PowerShell, o argumento da segunda instância fica: `.\gradlew.bat :payment-service:bootRun --args="--server.port=8082"`

Painéis: Eureka em http://localhost:8761 · RabbitMQ em http://localhost:15672 (guest/guest).

Testes automatizados: `./gradlew test`

## Contrato da API

### order-service :8080

| Método | Rota | Corpo | Respostas |
|---|---|---|---|
| GET | `/dishes` | — | 200 lista de Dish |
| GET | `/dishes/{id}` | — | 200 Dish · 404 |
| POST | `/orders` | `{"dishId": 1, "quantity": 2}` | 201 Order · 400 · 404 · 409 · 502 · 429 (rate limit) |
| GET | `/orders/{id}` | — | 200 Order · 404 |
| POST | `/reviews` | `{"dishId": 1, "rating": 5, "comment": "Great"}` | 202 sem corpo · 400 · 404 |
| POST | `/assistant` | `{"question": "Tem opção vegana?"}` | 200 `{"answer": "..."}` |

### review-service :8083

| Método | Rota | Resposta |
|---|---|---|
| GET | `/reviews/ranking` | 200 `[{"dishId":1,"dishName":"House Burger","average":4.6,"count":128}]` |

### payment-service :8081 / :8082 (interno)

`POST /payments` com `{"amount": 79.80}` → 200 `{"status":"APPROVED","instance":8081}` ou 500 (falha simulada).

Todos os erros seguem o formato `{"error": "Dish out of stock"}`.

## Como cada requisito foi atendido

| Critério | Onde está |
|---|---|
| **Eureka** | `eureka-server` com `@EnableEurekaServer`; os outros três serviços se registram pelo `spring.application.name`. `instance-id` inclui a porta, então as duas instâncias de pagamento aparecem separadas. |
| **Load balance** | `HttpClientConfig`: `RestTemplate` com `@LoadBalanced`. `PaymentClient` chama `http://PAYMENT-SERVICE/payments` (nenhum `localhost` entre serviços). O `payment-service` devolve e loga a porta (`[instance 8081] ...`), então a alternância aparece nos logs. |
| **Retry** | `PaymentClient` é um bean separado com `@Retryable` (Spring Framework 7) e `@EnableResilientMethods` em `HttpClientConfig`. `maxRetries=4`, `delay=200ms`, `multiplier=2` (backoff exponencial), `jitter=100ms`, `maxDelay=2000ms`, configuráveis em `application.yml`. Se todas as tentativas falharem, `OrderService` lança `PaymentFailedException` → 502, e a transação faz rollback (estoque intacto, nenhum pedido salvo). |
| **Race condition** | `DishRepository.findByIdForUpdate` com `@Lock(PESSIMISTIC_WRITE)` dentro de `OrderService.placeOrder` com `@Transactional`. O estoque só é baixado depois do pagamento aprovado. Coberto por `OrderConcurrencyTests` (50 threads → exatamente 10 pedidos, estoque 0). |
| **Mensageria** | `order-service/config/RabbitConfig`: `TopicExchange delivery.exchange`, fila durável `reviews.queue`, routing key `reviews.new` e o `Binding`, todos explícitos. Mensagem JSON (`dishId`, `dishName`, `rating`, `comment`). `POST /reviews` responde 202 sem gravar no banco. |
| **Backpressure** | `ReviewListener` (`@RabbitListener`) só acumula no `ReviewBuffer` (`ConcurrentHashMap` por prato com soma e quantidade). `ReviewFlushScheduler` (`@Scheduled` a cada 5 s) grava o acumulado na entidade `ReviewSummary` (H2) em uma transação e limpa o buffer; se a gravação falhar, o lote volta para o buffer. `GET /reviews/ranking` lê do banco. |
| **Spring AI** | `ChatService` cria o `ChatClient` a partir do `ChatClient.Builder`, com system message definindo o papel (atendente, respostas curtas em português, recusa educada fora do tema). O cardápio atual (nome, preço, estoque e descrição) é lido do banco a cada pergunta e enviado no prompt. |
| **Docker e contrato** | `docker-compose.yml` com `rabbitmq:4-management` (5672 e 15672). Rotas, campos e status seguem exatamente o enunciado. |
| **Bônus: rate limit** | `OrderRateLimitFilter` + `TokenBucket` (implementação própria): no máximo 20 `POST /orders` por segundo; o excedente recebe 429 com `{"error": "..."}`. |

## Roteiro de demonstração

**Race condition** (50 pedidos simultâneos no prato 1):

```bash
./scripts/race-test.sh          # Linux/macOS/Git Bash
pwsh ./scripts/race-test.ps1    # PowerShell 7
```

Resultado: exatamente 10 × `201` e estoque final 0. Com o rate limit ligado, das 50 requisições
simultâneas só 20 passam pelo limitador (10 × 201 e 10 × 409) e as outras 30 recebem 429. Para ver o
cenário puro (10 × 201 e 40 × 409), suba o order-service com `RATE_LIMIT_ENABLED=false`.
Para repetir o teste, reinicie o order-service (o H2 é em memória e o estoque volta para 10).

**Load balance e retry**: faça alguns `POST /orders` em pratos com estoque e observe os logs:
o order-service mostra cada tentativa e a instância que aprovou; os dois terminais do payment-service
alternam `[instance 8081]` e `[instance 8082]`.

**Pagamento fora do ar → 502**: pare as duas instâncias de pagamento, faça um pedido e confira
que a resposta é 502 e que `GET /dishes/{id}` mostra o estoque intacto.

**Avaliações em massa**:

```bash
./scripts/reviews-burst.sh
pwsh ./scripts/reviews-burst.ps1
```

Todas respondem 202 imediatamente; o log do review-service mostra um único flush a cada 5 s
(`Flush: 300 reviews of 5 dishes saved to the database`) e o ranking aparece ordenado pela média.

**Assistente**:

```bash
curl -X POST http://localhost:8080/assistant -H "Content-Type: application/json" \
  -d '{"question": "Tem prato vegetariano até R$ 40?"}'
curl -X POST http://localhost:8080/assistant -H "Content-Type: application/json" \
  -d '{"question": "Quem ganhou a Copa de 2002?"}'
```

A primeira resposta cita o Garden Bowl (R$ 34,90); a segunda recusa com educação e volta ao cardápio.

## Solução de problemas

- **Serviço registrado com IP errado no Eureka** (comum com VPN ou adaptadores do Docker/WSL):
  adicione `spring.cloud.inetutils.preferred-networks=192.168` (ou a faixa da sua rede) no
  `application.yml` do serviço.
- **A instância 8082 demora a receber chamadas**: o cache do Eureka e do LoadBalancer leva alguns
  segundos (configurado para ~5 s) para enxergar uma instância nova.
- **`POST /reviews` responde 503**: o RabbitMQ não está no ar; rode `docker compose up -d`.
