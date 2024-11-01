# EVM Transaction Processor Service

## Overview

The EVM Transaction Processor Service connects to an Ethereum Virtual Machine (EVM) node to capture real-time transaction data, storing and enabling searches through a REST API. It supports seamless transaction streaming and efficient handling of high-volume blockchain data.

## Features

1. **Real-Time Transaction Monitoring**: Connects to EVM nodes (e.g., Ethereum, BSC, Polygon) and continuously captures transactions.
2. **Database Storage**: Efficiently stores transactions in a database, handling potentially millions daily.
3. **REST API**: Provides endpoints to search transaction data. Optional features include full-text search, health check, and metrics.
4. **Observability**: Provided metrics, healthcheck endpoint and full-text search.


## Prerequisites

- **EVM Node Access**: Choose a public node (e.g., Infura) or your own.

Create acc here: https://app.infura.io/
Get a token and set it as a ENV variable at project root in .env file. It will be fetched by Docker & Spring.

- **Java & Docker**: Required for the containerized version.

## Setup Instructions

1. Clone the repository<br>
git clone https://github.com/overpathz/evm-transaction-processor-service.git<br>
cd evm-transaction-processor-service<br>

2. Configure environment variables (.env file in base dir)<br>
INFURA_API_KEY=your_infura_api_key<br>

3. Run the service with Docker compoe<br>
docker-compose up --build<br>

<b>Check api doc on swagger locally:</b><br>
http://localhost:8080/swagger-ui.html<br>

<br>Api docs as well:</b><br>
http://localhost:8080/v3/api-docs<br>

<br>Metrics and healthcheck</b><br>
http://localhost:8080/actuator/health<br>
http://localhost:8080/actuator/metrics<br>

<b>Metrics names:</b><br>
"evm.block.processing.time",<br>
"evm.blocks.processed.count",<br>
"evm.transaction.batches.processed",<br>
"evm.transaction.saving.time",<br>
"evm.transactions.per.block",<br>
"evm.transactions.processed.count",<br><br>
