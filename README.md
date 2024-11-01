# java-todo-list

My first to-do list with Java implementation, using the built-in HTTP server from the `com.sun.net.httpserver` package.

Before we start with Spring Boot, we need to understand the basics of how the HTTP server works in java.

The application allows users to manage their to-do items through a RESTful API, supporting operations such as creating, reading, updating, and deleting to-do items.

## Features

1. GET: Retrieve the list of all to-do items.

2. POST: Add a new to-do item.

3. PATCH: Update an existing to-do item by its ID(index currently).

4. DELETE: Remove a to-do item by its ID(index currently).

5. Simple error handling for invalid requests and missing parameters.

## Endpoints

- **GET**: /todos

  Response: JSON array of to-do items.

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

## Error Handling

- 400 Bad Request: Missing parameters or invalid input.

- 404 Not Found: Item not found based on provided ID.

- 405 Method Not Allowed: Unsupported HTTP method.
