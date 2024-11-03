# Java To-Do List

My first Java implementation with todo list.

Using the built-in HTTP server from the `com.sun.net.httpserver` package, we will implement CRUD operations.

Before we start with Spring Framework, we need to understand the basics of how the HTTP server works in Java.

It supports operations such as **creating**, **reading**, **updating**, and **deleting** to-do items using a RESTful API.

## TODOs

- [x] CLI interactive to-do list using a variable-based, temporary in-memory storage.

- [x] HTTP server using a variable-based, temporary in-memory to-do list.

- [] with [PostgreSQL](https://www.postgresql.org).

  - [x] setup docker

- [ ] with [MongoDB](https://www.mongodb.com).

- [ ] with [Redis](https://redis.io) and [PostgreSQL](https://www.postgresql.org).

## Features

1. GET:

   - Retrieve the list of all to-do items.

   - Retrieve the to-do item with id.

2. POST: Add a new to-do item.

3. PATCH: Update an existing to-do item by its ID(index currently).

4. DELETE: Remove a to-do item by its ID(index currently).

5. Simple error handling for invalid requests and missing parameters.

## Endpoints

- **GET**: /todos

  Response: JSON array of to-do items.

- **GET**: /todo/$id

  Response: JSON object of to-do item.

- **POST**: /todos

  Request Body: Plain text of the new to-do item.

  Response: Confirmation message.

- **PATCH**: /todos/{id}

  Request Body: Plain text of the updated to-do item.

  Response: Confirmation message or error if ID is invalid.

- **DELETE**: /todos?id={id}

  Response: Confirmation message or error if ID is invalid.

  P.S. Prefer using /todos/{id},
  but this is just for practicing parsing the query string.

## Success Response

- 200 Success.

- 201 Created: New item added successfully.

## Error Handling

- 400 Bad Request: Missing parameters or invalid input.

- 404 Not Found: Item not found based on provided ID.

- 405 Method Not Allowed: Unsupported HTTP method.

## Licenses

This project is licensed under the [MIT License](LICENSE).

The third-party licenses used in this project are listed in [THIRD-PARTY-LICENSE](THIRD-PARTY-LICENSE)
