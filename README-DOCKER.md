# Docker Setup for Smart PG Management System

This guide will help you run the Smart PG Management System using Docker.

## Prerequisites

- Docker Desktop installed on your system
- Docker Compose (comes with Docker Desktop)

## Quick Start

### Option 1: Using Docker Compose (Recommended)

This will start the Spring Boot application along with PostgreSQL and Redis.

1. **Create environment file**
   ```powershell
   Copy-Item .env.example .env
   ```

2. **Edit the `.env` file** with your actual AWS and SendGrid credentials:
   ```
   AWS_ACCESS_KEY=your_actual_aws_access_key
   AWS_SECRET_KEY=your_actual_aws_secret_key
   SENDGRID_API_KEY=your_actual_sendgrid_api_key
   ```

3. **Start all services**
   ```powershell
   docker-compose up -d
   ```

4. **View logs**
   ```powershell
   docker-compose logs -f app
   ```

5. **Stop all services**
   ```powershell
   docker-compose down
   ```

6. **Stop and remove volumes** (clean slate)
   ```powershell
   docker-compose down -v
   ```

### Option 2: Using Dockerfile Only

If you already have PostgreSQL and Redis running separately:

1. **Build the Docker image**
   ```powershell
   docker build -t smartpg-app .
   ```

2. **Run the container**
   ```powershell
   docker run -d `
     -p 8080:8080 `
     -e DB_URL="jdbc:postgresql://host.docker.internal:5432/your_db" `
     -e DB_USERNAME="your_username" `
     -e DB_PASSWORD="your_password" `
     -e AWS_REGION="ap-south-1" `
     -e AWS_ACCESS_KEY="your_aws_key" `
     -e AWS_SECRET_KEY="your_aws_secret" `
     -e SENDGRID_API_KEY="your_sendgrid_key" `
     -e SPRING_PROFILES_ACTIVE="dev" `
     --name smartpg-app `
     smartpg-app
   ```

   Note: Use `host.docker.internal` to access services running on your host machine from within the container.

## Accessing the Application

- **Application URL**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Actuator Health**: http://localhost:8080/actuator/health

## Database Access

When using docker-compose, you can access PostgreSQL:
- **Host**: localhost
- **Port**: 5432
- **Database**: smartpg_db
- **Username**: smartpg_user
- **Password**: smartpg_password

## Redis Access

When using docker-compose, Redis is available at:
- **Host**: localhost
- **Port**: 6379

## Useful Docker Commands

### View running containers
```powershell
docker ps
```

### View all containers (including stopped)
```powershell
docker ps -a
```

### View logs for a specific service
```powershell
docker-compose logs -f app
docker-compose logs -f postgres
docker-compose logs -f redis
```

### Execute commands inside container
```powershell
docker exec -it smartpg-app sh
```

### Restart a specific service
```powershell
docker-compose restart app
```

### Rebuild and restart after code changes
```powershell
docker-compose up -d --build
```

### Check container resource usage
```powershell
docker stats
```

## Troubleshooting

### Application won't start
1. Check logs: `docker-compose logs app`
2. Ensure PostgreSQL is healthy: `docker-compose ps`
3. Verify environment variables in `.env` file

### Database connection issues
1. Check PostgreSQL is running: `docker-compose ps postgres`
2. Verify database credentials in `.env` file
3. Ensure the database is healthy: `docker-compose logs postgres`

### Port already in use
If port 8080, 5432, or 6379 is already in use, edit `docker-compose.yml` to use different ports:
```yaml
ports:
  - "8081:8080"  # Map to different host port
```

### Out of memory errors
Increase memory limits in `docker-compose.yml`:
```yaml
environment:
  JAVA_OPTS: "-Xmx1024m -Xms512m"
```

## Production Considerations

For production deployment:

1. **Use secrets management** instead of `.env` file
2. **Set up proper volume backups** for PostgreSQL data
3. **Use external managed services** for database and cache
4. **Enable HTTPS** with reverse proxy (nginx/traefik)
5. **Set profile to prod**: `SPRING_PROFILES_ACTIVE=prod`
6. **Review and adjust** JVM memory settings based on your needs
7. **Set up monitoring** and logging solutions
8. **Use Docker secrets** for sensitive data

## Building for Different Platforms

To build for a specific platform (e.g., for deployment on ARM-based servers):

```powershell
docker buildx build --platform linux/amd64 -t smartpg-app .
```

## Clean Up

To completely remove all containers, networks, and volumes:

```powershell
docker-compose down -v --rmi all
```

## Support

For issues or questions, please refer to the main README.md or contact the development team.

