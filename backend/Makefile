# Java/Maven configuration
MAVEN = ./mvnw
MAVEN_OPTS = -Dmaven.test.skip=true

# Application configuration
APP_NAME = pdds-vrp-planner
JAR_FILE = target/$(APP_NAME).jar
PORT = 8080

.PHONY: all clean build run dev test

# Default target
all: clean build

# Clean build artifacts
clean:
	$(MAVEN) clean

# Build the application
build:
	$(MAVEN) $(MAVEN_OPTS) package

# Run the application in production mode
run:
	$(MAVEN) spring-boot:run

# Run the application in development mode with live reload and GUI support
dev:
	$(MAVEN) spring-boot:run -Dspring-boot.run.profiles=dev -Djava.awt.headless=false

# Run tests
test:
	$(MAVEN) test

# Install dependencies
install:
	$(MAVEN) install

# Start application with specific profile
run-profile:
	@read -p "Enter profile name (dev/prod/test): " profile; \
	$(MAVEN) spring-boot:run -Dspring-boot.run.profiles=$$profile

# Check application status
status:
	@if pgrep -f $(APP_NAME) > /dev/null; then \
		echo "Application is running"; \
	else \
		echo "Application is not running"; \
	fi

# Show help
help:
	@echo "Available commands:"
	@echo "  make          : Clean and build the application"
	@echo "  make clean    : Clean build artifacts"
	@echo "  make build    : Build the application"
	@echo "  make run      : Run the application in production mode"
	@echo "  make dev      : Run the application in development mode with GUI"
	@echo "  make test     : Run tests"
	@echo "  make install  : Install dependencies"
	@echo "  make status   : Check if application is running"
	@echo "  make help     : Show this help message" 