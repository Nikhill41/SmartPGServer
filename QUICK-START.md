# 🚀 Quick Start Commands

## Start Everything
```powershell
docker-compose up --build
```

## Start in Background
```powershell
docker-compose up -d --build
```

## Stop Everything
```powershell
docker-compose down
```

## View Logs
```powershell
docker-compose logs -f
```

## Check Status
```powershell
docker-compose ps
```

---

## 🌐 Access Points
- **Application**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **PostgreSQL**: localhost:5432
- **Redis**: localhost:6379

---

## 📋 Database Credentials
- **Host**: localhost (from outside) or postgres (inside Docker)
- **Port**: 5432
- **Database**: smart_rooms_db
- **Username**: postgres
- **Password**: Shivam@12345

---

## ⚡ Quick Tips
- First build takes 5-10 minutes (downloads dependencies)
- Data persists in Docker volumes
- Java 17 runs inside Docker (no local Java 17 needed)
- Press `Ctrl+C` to stop services

