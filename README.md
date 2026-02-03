# Order Link Full-Stack Application

This repository contains the source code for the Order Link application, a full-stack web application.

## Project Structure

The workspace is organized into two main projects:

-   [`order-link`](order-link): The backend service, built with Java and Maven.
-   [`order-link-frontend`](order-link-frontend): The frontend application, a modern web app likely built with a framework like Vue or React, using Vite.

## Prerequisites

Before you begin, ensure you have the following installed:
-   Java Development Kit (JDK)
-   Apache Maven
-   Node.js and npm
-   Docker (optional, for containerized deployment)


## Repository Layout

```
order-link/
  src/main/java/com/orderlink        # Spring Boot services, events, logistics integration
  src/main/resources                 # Flyway migrations, seed data, config
order-link-frontend/                 # Vue 3 client (PrimeVue + Pinia)
```

Refer to [`order-link-frontend/README.md`](../order-link-frontend/README.md) for UI-specific notes.

## Getting Started

### Installation and set up

1.  **Clone the repository:**
    ```sh
    git clone <repository-url>
    cd <repository-directory>
    ```


1.  **Run both applications and DB using the script**
    ```sh
    ./run.sh [options: [-all|-backend]]
    ```

