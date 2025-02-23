# Short URL Shred

## Title
Short URL Shred

## Description
A collision-free URL shortening service that integrates with NoSQL databases for storage and features Prometheus-based metrics for monitoring.

## Setup
1. Clone the repository.
2. Make sure you have Java 17, Maven, and Docker installed.
3. Build the application using:
   ```bash
   mvn clean install
   ```
4. Run the application locally:
   ```bash
   mvn spring-boot:run
   ```
5. Or dockerize the application by building the Docker image:
   ```bash
   docker build -t short-url-shred .
   ```
6. And then run the Docker container:
   ```bash
   docker run -p 8080:8080 short-url-shred
   ```

## Usage Instructions
1. Create a new short URL:
   - Endpoint: `POST /shorturls`
   - Request JSON body: `{ "originalUrl": "https://example.com" }`
   - Response JSON body: `{ "shortKey": "string", "originalUrl": "string" }`
2. Retrieve the original URL:
   - Endpoint: `GET /shorturls/{shortKey}`
   - Response JSON body: `{ "shortKey": "string", "originalUrl": "string" }`

## API Documentation
- POST `/shorturls` -> Creates a short URL.
- GET `/shorturls/{shortKey}` -> Retrieves the original URL.

## API
Refer to the endpoints for creating and retrieving short URLs.

## Additional Info
For advanced configurations, refer to `application.yml` and adjust NoSQL settings as needed.
