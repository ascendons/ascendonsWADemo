Divisha WhatsApp Bot - Starter
================================

Stack:
- Backend: Java 17 + Spring Boot 3 + MongoDB
- Frontend: React + Vite
- Docker + docker-compose for easy startup

Placeholders:
- META_ACCESS_TOKEN, PHONE_NUMBER_ID, WHATSAPP_VERIFY_TOKEN must be set as environment variables.

Run with Docker Compose:
1. Edit docker-compose.yml and replace YOUR_META_TOKEN etc.
2. docker-compose up --build

Local run:
- Backend: mvn clean package && mvn spring-boot:run (from backend/)
- Dashboard: npm install && ng serve (from dashboard/)
