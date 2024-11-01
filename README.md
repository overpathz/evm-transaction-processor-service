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

1. Clone the repository
git clone https://github.com/overpathz/evm-transaction-processor-service.git<br>
cd evm-transaction-processor-service<br>

2. Configure environment variables (.env file in base dir)
INFURA_API_KEY=your_infura_api_key

3. Run the service with Docker compoe
docker-compose up --build
