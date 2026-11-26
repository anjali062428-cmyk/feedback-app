# Feedback App

A minimal Java 17 web application built with Maven and **zero external dependencies**. It uses the JDK built-in `HttpServer` to serve a modern customer feedback form and handle form submissions.

---

## Features

- **Java 17 & Zero Dependencies:** Uses only standard JDK APIs (`com.sun.net.httpserver.HttpServer`).
- **Static Form Serving:** Serves `src/main/resources/index.html` at `http://localhost:8080/`.
- **Form Fields:** Name, Email, Category, Rating (1–5), Comments, and a Submit button.
- **Form Handler:** Handles `POST /submit`, decodes form parameters, logs them to stdout, and displays a styled confirmation page with submitted details.
- **Packaged Executable:** Builds into `target/feedback-app.jar` with a `Main-Class` manifest header.
- **Docker Support:** Multi-stage `Dockerfile` with minimal JRE runtime footprint.
- **CI/CD Automation:**
  - `Jenkinsfile` pipeline with `Checkout`, `Build`, and `Test` stages.
  - GitHub Actions workflow (`.github/workflows/maven.yml`) executing `mvn -B package` on push.

---

## Project Structure

```text
feedback-app/
├── .github/
│   └── workflows/
│       └── maven.yml          # GitHub Actions workflow (mvn -B package on push)
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── example/
│       │           └── feedback/
│       │               └── App.java        # JDK HttpServer application
│       └── resources/
│           └── index.html                 # Feedback form (Name, Email, Category, Rating, Comments)
├── .gitignore
├── Dockerfile                             # Multi-stage Docker build
├── Jenkinsfile                            # Jenkins pipeline (Checkout, Build, Test)
├── pom.xml                                # Maven configuration (no dependencies, feedback-app.jar)
└── README.md
```

---

## Getting Started

### Prerequisites

- Java Development Kit (JDK) 17 or higher
- Apache Maven 3.8+ (or Docker)

### Build with Maven

Package the application into `target/feedback-app.jar`:

```bash
mvn clean package
```

### Run Locally

Run the packaged executable JAR:

```bash
java -jar target/feedback-app.jar
```

Or run directly using Maven:

```bash
mvn compile exec:java -Dexec.mainClass="com.example.feedback.App"
```

Once started, open your browser and navigate to:
[http://localhost:8080](http://localhost:8080)

---

## Docker Support

### Build Docker Image

```bash
docker build -t feedback-app:latest .
```

### Run Docker Container

```bash
docker run -d -p 8080:8080 --name feedback-app feedback-app:latest
```

Access the app at [http://localhost:8080](http://localhost:8080).

---

## CI / CD Pipelines

- **Jenkins:** Configure a Pipeline job targeting the repository. The provided `Jenkinsfile` runs `Checkout`, `Build` (`mvn -B clean compile`), and `Test` (`mvn -B test`).
- **GitHub Actions:** Automatically runs on every push using `.github/workflows/maven.yml` with `actions/setup-java@v4` on JDK 17.
