# Wallet Service - Distributed/AWS Extension

This branch extends the original wallet transfer assignment without changing the
core controller -> service -> repository/domain transaction model.

## Components

- PostgreSQL / Amazon RDS: transactional source of truth.
- Flyway: database migrations.
- Idempotency: unique transfer idempotency key plus request parameter validation.
- Pessimistic wallet locks: safe concurrent transfers.
- Outbox: wallet transaction and event persisted atomically.
- Kafka: asynchronous transfer events.
- Kafka consumer: builds the DynamoDB read model.
- Redis: cache-aside cache for wallet balances.
- DynamoDB: read-optimized transfer history.
- Docker Compose: local PostgreSQL, Redis, Kafka, DynamoDB Local and wallet service.
- Kubernetes: local/EKS deployment manifests.
- ECR: Docker image registry.
- EKS: Kubernetes deployment target.

AWS SDK v2's DynamoDB Enhanced Client is used for the read model.

## Request flow

```text
POST /wallet/v1/transfers
        |
        v
WalletController
        |
        v
WalletService
        |
        +--> idempotency lookup
        |
        v
TransferTransactionService @Transactional
        |
        +--> lock wallet_1/wallet_2 in deterministic order
        +--> update balances
        +--> write transfer
        +--> write debit/credit ledger
        +--> write outbox_events
        |
        v
PostgreSQL COMMIT
        |
        v
OutboxPublisher
        |
        v
Kafka wallet.transfer.completed
        |
        v
TransferCompletedConsumer
        |
        v
DynamoDB read model
```

## Why the outbox exists

Do not make the REST transaction depend directly on a Kafka network call.

Without an outbox:

```text
DB COMMIT succeeds
Kafka publish fails
=> DB says SUCCESS but no event exists
```

With the outbox:

```text
wallet + transfer + ledger + outbox
             |
             +-- one PostgreSQL transaction
```

The publisher can retry Kafka delivery. The consumer's DynamoDB `putItem` uses
deterministic keys, so replaying the same transfer event is idempotent.

The publisher is deliberately at-least-once. A crash between Kafka acknowledgement
and marking the outbox row as PUBLISHED can produce a duplicate Kafka event. The
consumer safely overwrites the same DynamoDB read-model item.

## Redis

`GET /wallet/v1/{walletId}/balance` uses cache-aside:

1. Redis lookup.
2. On cache miss, PostgreSQL lookup.
3. Store balance in Redis for 60 seconds.
4. Successful transfer evicts both source and destination wallet cache entries.
5. Redis failure falls back to PostgreSQL.

Redis is an optimization; PostgreSQL remains the source of truth.

## DynamoDB read model

`GET /wallet/v1/{walletId}/transfers?limit=20`

reads from DynamoDB rather than PostgreSQL.

Partition key:

```text
walletId
```

Sort key:

```text
occurredAt#transferId#direction
```

The Kafka consumer writes two records per completed transfer:

- DEBIT for the source wallet.
- CREDIT for the destination wallet.

This intentionally demonstrates CQRS/read-model projection.

## Local startup

Prerequisites:

- Docker Desktop
- Java 21 for non-container Maven execution
- Maven 3.9+

Run:

```bash
./scripts/local-up.sh
```

or:

```bash
docker compose up -d --build
```

Then:

```text
API:     http://localhost:8080
Swagger: http://localhost:8080/swagger-ui.html
Health:  http://localhost:8080/actuator/health
```

Example transfer:

```bash
curl -X POST http://localhost:8080/wallet/v1/transfers   -H 'Content-Type: application/json'   -d '{
    "idempotencyKey": "demo-001",
    "fromWalletId": "wallet_1",
    "toWalletId": "wallet_2",
    "amount": 100.00
  }'
```

Balance:

```bash
curl http://localhost:8080/wallet/v1/wallet_1/balance
```

DynamoDB-backed history:

```bash
curl http://localhost:8080/wallet/v1/wallet_1/transfers
```

There can be a short eventual-consistency delay between the transfer response and
the DynamoDB history because Kafka is asynchronous.

## AWS deployment

The intended low-cost learning topology is:

```text
EKS
 |
 +-- wallet-service
 +-- Kafka (single replica, learning setup)
 +-- Redis (single replica, learning setup)
 |
 +-- Amazon RDS PostgreSQL
 +-- Amazon DynamoDB
```

Create the DynamoDB table:

```bash
aws dynamodb create-table   --table-name wallet-transfer-read-model   --billing-mode PAY_PER_REQUEST   --attribute-definitions       AttributeName=walletId,AttributeType=S       AttributeName=sortKey,AttributeType=S   --key-schema       AttributeName=walletId,KeyType=HASH       AttributeName=sortKey,KeyType=RANGE   --region eu-west-2
```

Create an EKS cluster:

```bash
eksctl create cluster -f aws/eksctl.yaml
```

Build and push the image:

```bash
export AWS_ACCOUNT_ID=<account-id>
export AWS_REGION=eu-west-2
./scripts/build-push-ecr.sh
```

Create the Kubernetes secret and deploy:

```bash
export ECR_IMAGE=<account-id>.dkr.ecr.eu-west-2.amazonaws.com/wallet-service:0.1.0
export RDS_URL=jdbc:postgresql://<rds-endpoint>:5432/walletdb
export RDS_USERNAME=wallet
export RDS_PASSWORD=<password>

./scripts/deploy-eks.sh
```

Then:

```bash
kubectl -n wallet get pods
kubectl -n wallet get service wallet-service
```

The LoadBalancer service will receive an AWS load balancer address.

## AWS security note

`k8s/secret.example.yaml` and the deployment script use AWS access keys only
to keep the first learning deployment simple. Do not use long-lived access keys
for a production deployment.

The next hardening step should be EKS Pod Identity or IRSA with a narrowly scoped
DynamoDB IAM policy.

## EKS cost warning

EKS is not a completely free service. The EKS cluster control plane and worker
resources are billed separately from RDS, load balancers, etc. Check the current
AWS pricing and your account's free-tier eligibility before creating resources.

For learning, destroy the cluster and associated AWS resources when finished.

## Suggested interview topics covered

This project now gives hands-on examples for:

- Spring Boot layered architecture
- idempotency
- pessimistic locking
- transaction isolation
- double-entry ledger
- PostgreSQL/RDS
- Flyway
- Redis cache-aside
- Kafka producer
- Kafka consumer groups
- partition keys
- event ordering
- retries and DLT
- outbox pattern
- at-least-once delivery
- idempotent consumers
- CQRS/read models
- DynamoDB partition/sort key design
- Docker
- Kubernetes Deployment/Service
- readiness/liveness
- HPA
- ECR
- EKS
- AWS IAM
- externalized configuration
