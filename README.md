# Wildberries Reviews Scraper

API-сервис для сбора отзывов с Wildberries и экспорта в CSV формат. Работает через HTTP запросы и автоматически сохраняет данные на компьютер пользователя.

## Особенности

- REST API для запуска скрапинга по требованию
- Автоматическое сохранение CSV файлов на компьютере пользователя
- Запуск в Docker контейнере одной командой
- Поддержка всех архитектур процессоров (ARM64/AMD64) через Selenium Grid
- Headless Chrome/Chromium для работы без GUI
- Anti-detection настройки для Selenium
- Динамическая загрузка всех отзывов через скроллинг
- Работает на Mac (Intel & Apple Silicon), Windows и Linux

## Собираемые данные

Каждый отзыв содержит:
- **Date** - Дата публикации
- **Author** - Имя автора
- **Text** - Текст отзыва
- **Rating** - Оценка (1-5 звезд)
- **Photos** - Количество фотографий
- **Video** - Наличие видео (Yes/No)
- **Tags** - Дополнительные теги

## Быстрый старт

```bash
git clone https://github.com/yourusername/wildbeerries_bot.git
cd wildbeerries_bot
docker-compose up --build
```

Сервер будет доступен по адресу: http://localhost:8080

## Использование API

### Проверить статус API

```bash
curl http://localhost:8080/
```

### Запустить скрапинг отзывов

```bash
curl "http://localhost:8080/scrape?url=https://www.wildberries.ru/catalog/PRODUCT_ID/feedbacks?imtId=IMAGE_ID"
```

Процесс скрапинга запустится в фоновом режиме. CSV файл автоматически сохранится в директорию `output/`.

### Проверить статус скрапинга

```bash
curl http://localhost:8080/status
```

Возвращает список всех созданных CSV файлов с размерами и временем создания.

## Структура выходных файлов

Файлы сохраняются в директорию `output/`:
- Имя файла: `reviews_<timestamp>.csv`
- Кодировка: UTF-8
- Формат: CSV с заголовками
- Автоматическое экранирование спецсимволов

Пример `reviews_1234567890.csv`:
```csv
Date,Author,Text,Rating,Photos,Video,Tags
"15 января 2025",Иван И.,"Отличный товар!",5,3,No,"Качество: Отличное"
"14 января 2025",Мария П.,"Пришло быстро",4,0,Yes,""
```

## Примеры использования

### Полный workflow

```bash
docker-compose up --build

curl "http://localhost:8080/scrape?url=https://www.wildberries.ru/catalog/521896959/feedbacks?imtId=234818091"

curl http://localhost:8080/status

ls -lh output/
```

### Для Windows (PowerShell)

```powershell
docker-compose up --build

Invoke-WebRequest "http://localhost:8080/scrape?url=https://www.wildberries.ru/catalog/521896959/feedbacks?imtId=234818091"

Invoke-WebRequest http://localhost:8080/status

dir output
```

## Остановка приложения

```bash
docker-compose down
```

CSV файлы в директории `output/` сохраняются на компьютере пользователя.

## Поддержка разных архитектур процессоров

Приложение автоматически работает на всех архитектурах благодаря Selenium Grid:

- **ARM64** (Apple Silicon M1/M2/M3, ARM серверы) - использует `seleniarm/standalone-chromium:latest`
- **AMD64/x86_64** (Intel/AMD процессоры) - можно использовать `selenium/standalone-chrome:latest`

### Конфигурация для AMD64/Intel процессоров (опционально)

Для Intel/AMD процессоров можно использовать официальный образ Chrome. Создайте файл `.env`:

```bash
SELENIUM_IMAGE=selenium/standalone-chrome:latest
```

По умолчанию используется `seleniarm/standalone-chromium`, который работает на всех архитектурах.

### Архитектура приложения

Приложение состоит из двух Docker контейнеров:
1. **selenium-chrome** - Selenium Grid с headless браузером (порты 4444, 7900)
2. **wildbeerries_bot** - Ktor API сервер (порт 8080)

Контейнеры общаются между собой через Docker network. CSV файлы сохраняются через volume mounting в директорию `output/` на компьютере пользователя.

## Запуск без Docker Compose

```bash
docker build -t wildbeerries_bot .

docker run -p 8080:8080 -v $(pwd)/output:/app/output wildbeerries_bot
```

## Локальная разработка

### Требования

- JDK 21
- Gradle 8.12+
- Chrome/ChromeDriver (для тестов)

### Запуск

```bash
./gradlew run
```

### Сборка

```bash
./gradlew build
./gradlew buildFatJar
```

### Тесты

```bash
./gradlew test
```

## Архитектура проекта

```
src/main/kotlin/ru/kutoven/
├── Application.kt              # Точка входа
├── Routing.kt                  # API endpoints
├── model/
│   └── Review.kt              # Модель данных отзыва
└── scraper/
    └── WildberriesReviewsScraper.kt  # Логика скрапинга
```

## API Endpoints

| Метод | Путь | Параметры | Описание |
|-------|------|-----------|----------|
| GET | `/` | - | Информация об API |
| GET | `/scrape` | `url` (required) | Запустить скрапинг отзывов |
| GET | `/status` | - | Статус и список CSV файлов |

## Устранение неполадок

### Ошибка при сборке Docker образа

Убедитесь, что Docker запущен и имеет доступ к интернету для загрузки зависимостей.

### CSV файлы не создаются

Проверьте:
- Правильность URL страницы с отзывами
- Наличие отзывов у товара
- Логи контейнера: `docker-compose logs -f`

### Долгий процесс скрапинга

Это нормально для товаров с большим количеством отзывов. Используйте endpoint `/status` для мониторинга.

## Важные замечания

### Юридические аспекты

- Web scraping может нарушать Terms of Service сайта Wildberries
- Используйте только для образовательных целей или с разрешения владельца сайта
- Проверьте robots.txt перед использованием

### Технические ограничения

- Wildberries может использовать CAPTCHA или rate limiting
- Структура HTML может измениться
- Для production использования рекомендуется официальное API

## Технологии

- **Kotlin** 2.2.21
- **Ktor** 3.3.1 - HTTP сервер
- **Selenium** 4.38.0 - Веб-автоматизация (RemoteWebDriver)
- **Apache Commons CSV** 1.11.0 - CSV экспорт
- **Docker & Docker Compose** - Контейнеризация
- **Selenium Grid** - Управление headless браузером
- **Chrome/Chromium** - Headless браузер (seleniarm для ARM64, selenium для AMD64)

## Лицензия

MIT License

## Поддержка

При возникновении проблем создайте Issue в репозитории GitHub.