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

To package the standalone executable JAR locally:

```bash
mvnw.cmd clean package -DskipTests
```
The resulting executable JAR will be located in:
`target/app.jar`

Run the packaged JAR locally:
```bash
java -jar target/app.jar
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

## ☁️ Production Deployment Guide (Render + Docker & Aiven MySQL)

Render uses **Docker** to build and run Java Spring Boot applications. The repository includes a production-ready, multi-stage `Dockerfile` using Eclipse Temurin JDK 17 and JRE 17.

### Step 1: Prepare Database on Aiven MySQL
1. Log in to [Aiven Console](https://console.aiven.io) and create a **MySQL** service.
2. In your service **Overview**, locate the **Connection information**:
   - **Host** (e.g. `mysql-xxxxx.aivencloud.com`)
   - **Port** (e.g. `12345`)
   - **User** (`avnadmin`)
   - **Password**
   - **Database** (`defaultdb`)
3. Construct the JDBC URL with mandatory SSL:
   ```text
   jdbc:mysql://<HOST>:<PORT>/defaultdb?sslMode=REQUIRED
   ```

### Step 2: Deploy Web Service on Render
1. Log in to [Render Dashboard](https://dashboard.render.com).
2. Click **New +** → **Web Service**.
3. Select and connect your repository: `rahulgolla381-maker/exam-seating-backend`.
4. Configure service settings:
   - **Name**: `exam-seating-backend`
   - **Region**: Select a region close to your database
   - **Branch**: `main`
   - **Root Directory**: Leave blank (or `exam-seating-backend` if using a monorepo root)
   - **Runtime**: **Docker** *(Render automatically selects Docker because of the `Dockerfile` in the repository root)*
   - **Instance Type**: Free or Starter
5. Under **Environment Variables**, add:

| Key | Value / Example | Notes |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://<HOST>:<PORT>/defaultdb?sslMode=REQUIRED` | Full Aiven JDBC connection string with SSL |
| `SPRING_DATASOURCE_USERNAME` | `avnadmin` | Default Aiven MySQL user |
| `SPRING_DATASOURCE_PASSWORD` | `<your-aiven-password>` | Generated by Aiven |
| `JWT_SECRET` | `<your-secure-256-bit-secret>` | Minimum 32-character secure secret |
| `CORS_ALLOWED_ORIGINS` | `https://<your-frontend>.vercel.app,http://localhost:5173` | Allowed frontend origins (comma-separated) |
| `PORT` | *(Render provides this automatically)* | Defaults to 10000 on Render |

6. Click **Deploy Web Service**.
7. Render will automatically build the multi-stage Docker container and start your Spring Boot application!
