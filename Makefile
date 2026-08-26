.PHONY: build run test package clean docker-build docker-up docker-down

build:
	mvn compile

package:
	mvn package -DskipTests

run:
	mvn spring-boot:run

test:
	mvn test

clean:
	mvn clean

docker-build:
	docker build -t sms-app .

docker-up:
	docker-compose up --build

docker-down:
	docker-compose down
