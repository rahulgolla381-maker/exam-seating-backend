# Exam Hall Seating Allocator — Backend

The robust Spring Boot REST API powering the **Exam Hall Seating Allocator**. Implements graph-based conflict-minimizing seat allocation algorithms, JWT-secured role-based access control (ADMIN and STUDENT), PDF generation via OpenPDF, and comprehensive academic structure management.

---

## 🚀 Tech Stack

- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Database Access**: Spring Data JPA / Hibernate
- **Database**: MySQL 8.0+
- **Security**: Spring Security 6, JJWT (io.jsonwebtoken 0.11.5)
- **PDF Generation**: OpenPDF 1.3.30
- **Build Tool**: Apache Maven (Maven Wrapper included)

---

## 📋 Prerequisites

- **Java Development Kit (JDK)**: Version 17 or higher
- **MySQL Database Server**: Version 8.0+ running locally (or cloud database e.g., Aiven)
- **Maven**: (Optional; included `mvnw` wrapper can be used)

---

## 🛠️ Configuration & Environment Variables

The backend supports full configuration through environment variables with safe defaults for local development:

| Variable | Description | Local Default | Cloud Example (Render / Aiven) |
|---|---|---|---|
| `PORT` | HTTP Server port | `8083` | `10000` (Assigned by Render) |
| `SPRING_DATASOURCE_URL` | Complete JDBC URL | *Constructed from host/db* | `jdbc:mysql://aiven-host:port/defaultdb?sslMode=REQUIRED` |
| `DB_HOST` | MySQL hostname | `localhost` | `mysql-3a-myproject.aivencloud.com` |
| `DB_PORT` | MySQL port | `3306` | `26543` |
| `DB_NAME` | Database name | `exam_seating_db` | `defaultdb` |
| `DB_USER` / `SPRING_DATASOURCE_USERNAME` | Database username | `root` | `avnadmin` |
| `DB_PASSWORD` / `SPRING_DATASOURCE_PASSWORD` | Database password | *Local env / `application-local.properties`* | `[Aiven password placeholder]` |
| `DB_USE_SSL` | Enable SSL for DB | `false` | `true` |
| `JWT_SECRET` | 256-bit secret key for JWT signing | *Local env / `application-local.properties`* | `[Random 256-bit secret placeholder]` |
| `JWT_EXPIRATION` | Token validity in ms | `86400000` (24h) | `86400000` |
| `CORS_ALLOWED_ORIGINS` | Permitted frontend origins | `http://localhost:5173,http://localhost:3000` | `https://your-frontend.vercel.app` |

See [`.env.example`](.env.example) for a quick reference template.

---

## 💻 Running Locally

1. **Verify MySQL Database**:
   Ensure MySQL is running and database `exam_seating_db` is accessible. (The backend auto-creates database tables with `spring.jpa.hibernate.ddl-auto=update`).

2. **Run using Maven Wrapper**:
   - **Windows PowerShell / CMD**:
     ```cmd
     mvnw.cmd spring-boot:run
     ```
   - **Linux / macOS**:
     ```bash
     ./mvnw spring-boot:run
     ```
   - **Or with system Maven**:
     ```bash
     mvn spring-boot:run
     ```

The backend server will start at: **`http://localhost:8083`**

---

## 📦 Building the Production JAR

To package the standalone executable JAR:

```bash
mvnw.cmd clean package -DskipTests
```
The resulting executable JAR will be located in:
`target/exam-seating-0.0.1-SNAPSHOT.jar`

Run the packaged JAR:
```bash
java -jar target/exam-seating-0.0.1-SNAPSHOT.jar
```

---

## 🧪 Running Tests

Execute backend test suite (unit and integration tests for allocation algorithm and PDF generation):
```cmd
mvnw.cmd test
```

---

## 📡 REST API Endpoint Summary

### Authentication (`/api/auth`)
- `POST /api/auth/login`: Authenticate user and receive JWT Bearer token + role

### Seating Allocations (`/api/allocations`)
- `POST /api/allocations/generate`: Execute seating allocation wizard across rooms
- `GET /api/allocations/{examId}`: Retrieve persisted seating allocations for an exam
- `DELETE /api/allocations/{examId}`: Delete existing allocations for an exam
- `GET /api/allocations/{examId}/pdf`: Download printable master exam seating PDF
- `GET /api/allocations/{examId}/room/{roomId}/pdf`: Download room-specific seating PDF

### Academic Structure (`/api/departments`, `/api/sections`, `/api/students`)
- `GET`, `POST`, `PUT`, `DELETE` operations for Departments, Sections (with roll ranges), and Students

### Campus Infrastructure (`/api/buildings`, `/api/floors`, `/api/rooms`)
- Hierarchical building and floor management with room dimensions and capacity

### Student Portal (`/api/student`)
- `GET /api/student/me`: View student personal profile and assigned exam seats

---

## ☁️ Deployment Guide (Render & Aiven)

1. **Database on Aiven**:
   - Create a free/starter MySQL service on [Aiven](https://aiven.io).
   - Copy the Service URI, Host, Port, User (`avnadmin`), and Password.
   - Note: Aiven requires SSL mode (`sslMode=REQUIRED`).

2. **Backend on Render**:
   - Go to [render.com](https://render.com) and create a **Web Service**.
   - Connect this `exam-seating-backend` repository.
   - **Environment**: `Java` (or Docker / Maven)
   - **Build Command**: `./mvnw clean package -DskipTests`
   - **Start Command**: `java -jar target/exam-seating-0.0.1-SNAPSHOT.jar`
   - **Environment Variables**:
     - `SPRING_DATASOURCE_URL`: `jdbc:mysql://<aiven-host>:<port>/defaultdb?sslMode=REQUIRED`
     - `SPRING_DATASOURCE_USERNAME`: `avnadmin`
     - `SPRING_DATASOURCE_PASSWORD`: `<aiven-password>`
     - `CORS_ALLOWED_ORIGINS`: `https://<your-frontend>.vercel.app`
     - `JWT_SECRET`: `<generate-a-strong-random-secret>`
