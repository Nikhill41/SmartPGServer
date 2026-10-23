# Docker Setup Guide for Smart PG Application

This guide will help you run your Spring Boot application using Docker with PostgreSQL and Redis.

## 📋 Prerequisites

- Docker Desktop installed on your machine
- Docker Compose installed (comes with Docker Desktop)
- Internet connection (for downloading Docker images)

## 🏗️ Architecture Overview

Your application runs in Docker with:
- **Spring Boot App** - Runs on Java 17 (port 8080)
- **PostgreSQL Database** - Version 15 (port 5432)
- **Redis Cache** - Version 7 (port 6379)

All services are connected via a Docker network and the app waits for PostgreSQL and Redis to be healthy before starting.

## 📝 Step-by-Step Instructions

### Step 1: Open PowerShell/Command Prompt
Navigate to your project directory:
```powershell
cd C:\Users\shiva\Desktop\SmartPG_MeterDone\smart_pg
```

### Step 2: Build and Start All Services
Run this single command to start everything:
```powershell
docker-compose up --build
```

**What this does:**
- Downloads PostgreSQL 15 and Redis 7 images (first time only)
- Builds your Spring Boot application with Java 17 inside Docker
- Creates a PostgreSQL database named `smart_rooms_db`
- Starts Redis cache service
- Starts your Spring Boot application
- Links all services together

**Expected output:**
You'll see logs from all three services. Wait until you see:
```
smart-pg-app | Started SmartPgApplication in X.XXX seconds
```

### Step 3: Access Your Application
Once running, access:
- **Application**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **PostgreSQL**: localhost:5432 (use any DB client)
- **Redis**: localhost:6379

### Step 4: Stop the Services
To stop all services, press `Ctrl + C` in the terminal, then run:
```powershell
docker-compose down
```

To stop and remove all data (⚠️ deletes database):
```powershell
docker-compose down -v
```

## 🔧 Useful Commands

### Run in Detached Mode (Background)
```powershell
docker-compose up -d --build
```

### View Logs
```powershell
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f app
docker-compose logs -f postgres
docker-compose logs -f redis
```

### Check Service Status
```powershell
docker-compose ps
```

### Restart a Specific Service
```powershell
docker-compose restart app
```

### Rebuild Application Only
```powershell
docker-compose up --build app
```

### Access Database from Command Line
```powershell
docker exec -it smart-pg-postgres psql -U postgres -d smart_rooms_db
```

### Access Redis CLI
```powershell
docker exec -it smart-pg-redis redis-cli
```

## 📂 File Changes Made

### 1. **dockerfile** (Updated)
- Uses multi-stage build for smaller image size
- Stage 1: Builds your app with Maven + Java 17
- Stage 2: Runs your app with Java 17 JRE (lightweight)
- No need for Java 17 on your local machine!

### 2. **docker-compose.yml** (Created)
- Defines 3 services: postgres, redis, and your app
- Sets up network communication between services
- Includes health checks to ensure proper startup order
- Persists data using Docker volumes
- Configures all environment variables from your .env file

### 3. **application.yml** (Minor Update)
- Redis host now supports environment variables
- Allows connection to Redis in Docker (redis) or locally (localhost)

## 🔍 How It Works

### Database Connection
- **Local**: Your app used `localhost:5432`
- **Docker**: Your app uses `postgres:5432` (service name as hostname)
- Docker Compose automatically updates `DB_URL` environment variable

### Redis Connection
- **Local**: Your app used `localhost:6379`
- **Docker**: Your app uses `redis:6379` (service name as hostname)
- Configuration set via `SPRING_DATA_REDIS_HOST` environment variable

### Java Version
- Your laptop has Java 8, but **Docker uses Java 17**
- The application is built and run entirely inside Docker
- No conflict with your local Java installation

## 🎯 What Happens When You Run Docker Compose?

1. **Docker downloads images** (PostgreSQL 15, Redis 7, Maven with Java 17)
2. **PostgreSQL starts** and creates database `smart_rooms_db`
3. **Redis starts** and waits for connections
4. **Maven builds your app** inside Docker with Java 17
5. **Your app starts** and connects to PostgreSQL and Redis
6. **All services communicate** via the `smart-pg-network` network

## 📊 Data Persistence

Your data is stored in Docker volumes:
- `postgres-data`: All PostgreSQL database files
- `redis-data`: Redis cache data

**Data persists** even after stopping containers. Only deleted with `docker-compose down -v`.

## ⚠️ Important Notes

1. **First build takes time**: Maven downloads all dependencies (5-10 minutes)
2. **Subsequent builds are faster**: Docker caches layers
3. **Ports must be free**: Ensure 5432, 6379, and 8080 are not in use
4. **Environment variables**: Loaded from docker-compose.yml (from your .env file)
5. **AWS credentials**: Your AWS keys are included for S3, DynamoDB, and IoT access

## 🐛 Troubleshooting

### Port Already in Use
```powershell
# Check what's using the port
netstat -ano | findstr :8080

# Stop local PostgreSQL/Redis if running
```

### Build Fails
```powershell
# Clean and rebuild
docker-compose down
docker-compose build --no-cache
docker-compose up
```

### Database Connection Issues
```powershell
# Check if PostgreSQL is healthy
docker-compose ps

# View PostgreSQL logs
docker-compose logs postgres
```

### Application Won't Start
```powershell
# Check application logs
docker-compose logs app

# Ensure dependencies are healthy
docker-compose ps
```

## ✅ Verification

After starting, verify everything works:

1. **Check all services are running:**
   ```powershell
   docker-compose ps
   ```
   All should show "Up" and "healthy"

2. **Check application health:**
   ```
   curl http://localhost:8080/actuator/health
   ```

3. **Check database:**
   ```powershell
   docker exec -it smart-pg-postgres psql -U postgres -c "\l"
   ```

4. **Check Redis:**
   ```powershell
   docker exec -it smart-pg-redis redis-cli ping
   ```
   Should return: `PONG`

## 🎉 Summary

You now have a fully Dockerized Spring Boot application with:
- ✅ Java 17 running in Docker (independent of your local Java 8)
- ✅ PostgreSQL database with persistent storage
- ✅ Redis cache with persistent storage
- ✅ Automatic service dependency management
- ✅ Health checks and proper startup order
- ✅ All environment variables configured
- ✅ Network isolation and security

Just run `docker-compose up --build` and you're ready to go! 🚀

