# Define variables
DOCKER_COMPOSE = docker-compose
DOCKER = docker
SBT = sbt
PROJECT_DIR = enterprise-data-platform

# Define services
SERVICES = kafka-producer ingestion transformation storage serving


# Default target
all: build

# Build Docker images for all services according to docker-compose.yml
build: build-kafka-producer build-ingestion build-processing build-storage build-serving

build-kafka-producer:
	$(DOCKER) build -f docker/Dockerfile-kafka-producer -t kafka-producer .

build-ingestion:
	$(DOCKER) build -f docker/Dockerfile-ingestion -t $(PROJECT_DIR)/ingestion .

build-processing:
	$(DOCKER) build -f docker/Dockerfile-transformation -t $(PROJECT_DIR)/transformation .

build-storage:
	$(DOCKER) build -f docker/Dockerfile-storage -t $(PROJECT_DIR)/storage .

build-serving:
	$(DOCKER) build -f docker/Dockerfile-serving -t $(PROJECT_DIR)/serving .


# Start individual services
start-kafka-producer:
	$(DOCKER_COMPOSE) up -d kafka-producer

start-ingestion:
	$(DOCKER_COMPOSE) up -d ingestion

start-processing:
	$(DOCKER_COMPOSE) up -d processing

start-storage:
	$(DOCKER_COMPOSE) up -d storage

start-serving:
	$(DOCKER_COMPOSE) up -d serving

# Start all services
start-all: 
	$(DOCKER_COMPOSE) up -d


# Stop and remove services
down:
	$(DOCKER_COMPOSE) down

# Stop, remove containers, and remove images
clean:
	$(DOCKER_COMPOSE) down --rmi all --volumes --remove-orphans

# Build the project with sbt
build-sbt:
	$(SBT) clean compile

# Run tests with sbt
test:
	$(SBT) test

# Show status of running services
status:
	$(DOCKER_COMPOSE) ps
