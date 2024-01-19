.PHONY: run-test-db
run-test-db:
	 docker run -p 5432:5432 -e POSTGRES_USER=pigeon-scoops-user -e POSTGRES_DB=pigeon-scoops-db -e POSTGRES_PASSWORD=password postgres:alpine

.PHONY: run-local-db
run-local-db:
	 docker run -v ~/postgres-data:/var/lib/postgresql/data -p 5432:5432 -e POSTGRES_USER=pigeon-scoops-user -e POSTGRES_DB=pigeon-scoops-db -e POSTGRES_PASSWORD=password postgres:alpine
