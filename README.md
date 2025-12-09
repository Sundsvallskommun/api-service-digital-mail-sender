# Digital Mail Sender

_The service provides functionality to send emails to digital mailboxes connected to citizens and companies, as well as
digital invoices to citizens' Kivra mailboxes. Supported mailbox operators for emails: Kivra, Min Myndighetspost,
Fortnox and Billo._

_This service is part of the Messaging service eco system and should not be used by its own._

## Getting Started

### Prerequisites

- **Java 25 or higher**
- **Maven**
- **MariaDB**
- **Git**
- **[Dependent Microservices](#dependencies)**

### Installation

1. **Clone the repository:**

```bash
git clone https://github.com/Sundsvallskommun/api-service-digital-mail-sender.git
cd api-service-digital-mail-sender
```

2. **Configure the application:**

   Before running the application, you need to set up configuration settings.
   See [Configuration](#configuration)

   **Note:** Ensure all required configurations are set; otherwise, the application may fail to start.

3. **Ensure dependent services are running:**

   If this microservice depends on other services, make sure they are up and accessible.
   See [Dependencies](#dependencies) for more details.

4. **Build and run the application:**

   - Using Maven:

```bash
mvn spring-boot:run
```

- Using Gradle:

```bash
gradle bootRun
```

## Dependencies

This microservice depends on the following services:

- **Party**
  - **Purpose:** Used for translating party id to legal id.
  - **Repository:
    ** [https://github.com/Sundsvallskommun/api-service-messaging](https://github.com/Sundsvallskommun/api-service-messaging)
  - **Setup Instructions:** See documentation in repository above for installation and configuration steps.
- **Messaging**
  - **Purpose:** Used for sending information emails and slack messages.
  - **Repository:
    ** [https://github.com/Sundsvallskommun/api-service-messaging](https://github.com/Sundsvallskommun/api-service-messaging)
  - **Setup Instructions:** See documentation in repository above for installation and configuration steps.
- **Kivra**
  - **Purpose:** Backend service provided by 3rd party for sending invoices to citizens that has a Kivra mailbox.
- **Skatteverket**
  - **Purpose:** Backend service provided by 3rd party to verify that the citizen or company has a digital mailbox and
    to send email to valid receivers.

Ensure that these services are running and properly configured before starting this microservice.

## API Documentation

Access the API documentation via Swagger UI:

- **Swagger UI:** [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

## Usage

### API Endpoints

See the [API Documentation](#api-documentation) for detailed information on available endpoints.

### Example Request

```bash
curl -X GET http://localhost:8080/2281/has-available-mailbox/f04e3f82-0c62-4aad-bf42-a2902d850ddd
```

## Configuration

Configuration is crucial for the application to run successfully. Ensure all necessary settings are configured in
`application.yml`.

### Key Configuration Parameters

- **Server Port:**

```yaml
server:
  port: 8080
```

- **Database Settings**

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/your_database
    username: your_db_username
    password: your_db_password
  sql:
    init:
      mode: sql-init-mode (should be set to  'never' in production env)
  jpa:
    ddl-auto: auto-setting (should be set to 'validate' in production env)
```

- **External Service URLs**

```yaml
integration:
  party:
    api-url: http://dependency-service-url
    oauth2:
      token-url: http://dependecy-token-url
      client-id: some-client-id
      client-secret: some-client-secret
  messaging:
    api-url: http://dependency-service-url
    oauth2:
      token-url: http://dependecy-token-url
      client-id: some-client-id
      client-secret: some-client-secret
  kivra:
    api-url: http://dependency-service-url
    oauth2:
      token-url: http://dependecy-token-url
      client-id: some-client-id
      client-secret: some-client-secret
  skatteverket:
    key-store-as-base64: keystore-as-base64
    key-store-password: some-password
    recipient-url: http://dependency-service-url
```

- ** Configuration of schedulers**

```yaml
scheduler:
  certificateHealth:
    cron: cron expression when scheduler should run (or "-" to disable it)
```

### Database Initialization

The project is set up with [Flyway](https://github.com/flyway/flyway) for database migrations. Flyway is disabled by
default so you will have to enable it to automatically populate the database schema upon application startup.

```yaml
spring:
  flyway:
    enabled: true
```

- **No additional setup is required** for database initialization, as long as the database connection settings are
  correctly configured.

### Additional Notes

- **Application Profiles:**

  Use Spring profiles (`dev`, `prod`, etc.) to manage different configurations for different environments.

- **Logging Configuration:**

  Adjust logging levels if necessary.

## Contributing

Contributions are welcome! Please
see [CONTRIBUTING.md](https://github.com/Sundsvallskommun/.github/blob/main/.github/CONTRIBUTING.md) for guidelines.

## License

This project is licensed under the [MIT License](LICENSE).

## Code status

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-digital-mail-sender&metric=alert_status)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-digital-mail-sender)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-digital-mail-sender&metric=reliability_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-digital-mail-sender)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-digital-mail-sender&metric=security_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-digital-mail-sender)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-digital-mail-sender&metric=sqale_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-digital-mail-sender)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-digital-mail-sender&metric=vulnerabilities)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-digital-mail-sender)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-digital-mail-sender&metric=bugs)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-digital-mail-sender)

---

&copy; 2024 Sundsvalls kommun
