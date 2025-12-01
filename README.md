# Webhook Solver

This Spring Boot app performs the following on startup (no controllers needed):

- POSTs to `https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA` with the required body.
- Receives `webhook` URL and `accessToken`.
- Builds the final SQL query (PostgreSQL-style) for the problem, writes it to `solution.sql`.
- POSTs `{ "finalQuery": "..." }` to the returned webhook URL with `Authorization` header set to the `accessToken` received.

Build:

```bash
mvn -U -DskipTests package
```

Resulting JAR will be in `target/webhook-solver-0.0.1-SNAPSHOT.jar`.

Files of interest:

- `src/main/java/com/example/webhooksolver/WebhookSolverApplication.java` - main startup logic.
- `solution.sql` - generated after running the app.

Notes:

- The SQL produced is written to `solution.sql` in the working directory.
- The application is configured to run as a non-web Spring Boot app.
