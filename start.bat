@echo off
set SERVER_PORT=29909

echo Building application (mvn package -DskipTests)...
call mvn package -DskipTests -q

echo Starting Student Management System on port %SERVER_PORT%...
java -jar target\app.jar --server.port=%SERVER_PORT%
