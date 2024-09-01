# Variables
DOCKER_COMPOSE_FILE = docker-compose.yml

# Default target
.DEFAULT_GOAL := help

# List of targets with descriptions
help:
	@echo "Available targets:"
	@echo "  make build                Build the project"
	@echo "  make start-services       Start all services using Docker Compose"
	@echo "  make deploy               Deploy the application"
	@echo "  make data-migration       Run the data migration script"
	@echo "  make test                 Run all tests"
	@echo "  make clean                Clean up Docker containers and images"

# Build the project
build:
	sbt clean compile

# Start all services using Docker Compose
start-services:
	docker-compose -f $(DOCKER_COMPOSE_FILE) up -d

# Deploy the application
deploy:
	# Add deployment commands here, e.g., using Terraform, Helm, etc.
	echo "Deploying the application..."

# Run the data migration script
data-migration:
	python3 ./scripts/data-migration.py

# Run all tests
test:
	sbt test

# Clean up Docker containers and images
clean:
	docker-compose -f $(DOCKER_COMPOSE_FILE) down --rmi all --volumes --remove-orphans
