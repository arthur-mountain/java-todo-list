# 📝 Java To-Do List

My first Java implementation of a to-do list.

Using the built-in HTTP server from the `com.sun.net.httpserver` package, we will implement CRUD operations.

Before we start with the Spring Framework,
we need to understand the basics of how the HTTP server works in Java.

It supports operations such as **creating**, **reading**, **updating**, and **deleting** to-do items using a RESTful API.

## 📋 Requirements

1. [Docker](https://www.docker.com)

2. Java >= 21

## 🚀 Getting Started

1. Run Docker containers.

   ```bash
   docker compose -f docker-compose.yaml -f docker-kafka.yaml up
   ```

2. Run the Java HTTP server.

   ```bash
   make run
   ```

3. Run Java tests.

   ```bash
   make test
   ```

## ✅ TODOs

- [x] CLI interactive to-do list using variable-based, temporary in-memory storage.
      -> app/src/main/java/todolistScanner.java

- [x] HTTP server using a variable-based, temporary in-memory to-do list.

- [x] Set up Docker.

- [x] Integrate with [PostgreSQL](https://www.postgresql.org).

- [x] Integrate with [MongoDB](https://www.mongodb.com).

- [x] Integrate with [Redis](https://redis.io) and [PostgreSQL](https://www.postgresql.org).

- [x] Implement logging (JUL).

- [x] Integrate with [Kafka](https://kafka.apache.org).

- [ ] Integrate with [Kubernetes](https://kubernetes.io).

## ✨ Features

1. **GET**:

   - Retrieve the list of all to-do items.

   - Retrieve a to-do item by its ID.

2. **POST**: Add a new to-do item.

3. **PATCH**: Update an existing to-do item by its ID.

4. **DELETE**: Remove a to-do item by its ID.

5. Simple error handling for invalid requests and missing parameters.

## 🔗 Endpoints

<details>
<summary><h4 style='display:inline;'>🗄️ v1/todos -> PostgreSQL</h4></summary>

Entity: **TodoEntity.java**

- **GET**: `/v1/todos`

  - Response: JSON array of to-do items from PostgreSQL.

- **GET**: `/v1/todo/{id}`

  - Response: JSON object of a specific to-do item from PostgreSQL.

- **POST**: `/v1/todos`

  - Request Body: Plain text representing the new to-do item.
  - Response: Confirmation message upon successful addition to PostgreSQL.

- **PATCH**: `/v1/todos/{id}`

  - Request Body: Plain text representing the updated to-do item.
  - Response: Confirmation message or error if the ID is invalid.

- **DELETE**: `/v1/todos/{id}`

  - Response: Confirmation message or error if the ID is invalid.

</details>

<details>
<summary><h4 style='display:inline;'>🍃 v2/todos -> MongoDB</h4></summary>

Entity: **TodoMongoEntity.java**

- **GET**: `/v2/todos`

  - Response: JSON array of to-do items from MongoDB.

- **GET**: `/v2/todo/{id}`

  - Response: JSON object of a specific to-do item from MongoDB.

- **POST**: `/v2/todos`

  - Request Body: Plain text representing the new to-do item.
  - Response: Confirmation message upon successful addition to MongoDB.

- **PATCH**: `/v2/todos/{id}`

  - Request Body: Plain text representing the updated to-do item.
  - Response: Confirmation message or error if the ID is invalid.

- **DELETE**: `/v2/todos/{id}`

  - Response: Confirmation message or error if the ID is invalid.

</details>

<details>
<summary><h4 style='display:inline;'>🚀 v3/todos -> Redis + PostgreSQL</h4></summary>

Entity: **TodoEntity.java**

- **GET**: `/v3/todos`

  - Response: JSON array of to-do items, with Redis caching results from PostgreSQL.

- **GET**: `/v3/todo/{id}`

  - Response: JSON object of a specific to-do item, fetched directly from PostgreSQL (no Redis caching).

- **POST**: `/v3/todos`

  - Request Body: Plain text representing the new to-do item.
  - Response: Confirmation message upon successful addition to PostgreSQL (no Redis caching).

- **PATCH**: `/v3/todos/{id}`

  - Request Body: Plain text representing the updated to-do item.
  - Response: Confirmation message or error if the ID is invalid, with the update applied only to PostgreSQL (no Redis caching).

- **DELETE**: `/v3/todos/{id}`

  - Response: Confirmation message or error if the ID is invalid, with the deletion applied only to PostgreSQL (no Redis caching).

</details>

<details>
<summary><h4 style='display:inline;'>📬 v4/todos -> Kafka</h4></summary>

Entity: **TodoKafkaEntity.java**

- **POST**: `/v4/todos`

  - Request Body: `{id: string, title: string, description: string, price: int}`

  - Response: A string; consumes the message and prints it directly in the log.

</details>

## 🎉 Success Response

- **200** Success.

- **201 Created**: New item added successfully.

## ❗ Error Handling

- **400 Bad Request**: Missing parameters or invalid input.

- **404 Not Found**: Item not found based on the provided ID.

- **405 Method Not Allowed**: Unsupported HTTP method.

## ⚖️ Licenses

This project is licensed under the [MIT License](LICENSE).

The third-party licenses used in this project are listed in [THIRD-PARTY-LICENSE](THIRD-PARTY-LICENSE).
