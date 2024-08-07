# Используем официальный образ Maven
FROM maven:3.9.0-jdk-21

# Устанавливаем рабочую директорию
WORKDIR /usr/src/app

# Копируем файл pom.xml
COPY pom.xml .

# Загружаем зависимости
RUN mvn dependency:go-offline

# Копируем остальные файлы проекта
COPY . .

# Выполняем сборку
RUN mvn clean package
