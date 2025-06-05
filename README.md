# E-Commerce Backend API (Spring Boot)

## Project Overview

This project is a backend API for a fashion e-commerce platform, built using **Spring Boot**. It supports product management, user carts, orders, and a messaging system between buyers and sellers. The application is designed to integrate easily with a modern frontend (e.g., React) and provides RESTful endpoints with JWT-based authentication and PostgreSQL persistence.

---

## Key Features

- **User Management**:
    - Registration & login using JWT-based security
    - Role-based access (Buyer & Seller)

- **Product Catalog**:
    - Create, read, update, delete (CRUD) operations for products
    - Seller-linked product management

- **Shopping Cart**:
    - Add, update, or remove items in the cart
    - Cart retrieval by user ID

- **Order Management**:
    - Create orders from cart items
    - View and update order statuses (Pending, Shipped, etc.)

- **Messaging System**:
    - Buyers can message sellers regarding products
    - Sellers can respond using a basic chat system

- **Error Handling & Validation**:
    - Centralized global exception handling
    - Entity validation via Spring annotations

- **Database Integration**:
    - Uses PostgreSQL with Spring Data JPA
    - Automatic schema updates (`ddl-auto=update`)

---

## Technologies Used

- Java 17+
- Spring Boot
- Spring Data JPA
- PostgreSQL
- Spring Security + JWT
- Lombok
- Maven

---

## Getting Started

### Prerequisites:

- Java 17+
- Maven
- PostgreSQL (running locally)

### Setup & Running Locally:

1. **Clone the repository**:

```bash
git clone https://github.com/https://github.com/kidus-yoseph-t/fashion
cd fashion
Configure your database:

Update src/main/resources/application.properties with your PostgreSQL credentials:

properties
Copy
Edit
spring.datasource.url=jdbc:postgresql://localhost:5432/fashion
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
Build and run the app:

bash
Copy
Edit
./mvnw spring-boot:run
Or run the FashionApplication.java file from your IDE.

Project Structure Highlights
bash
Copy
Edit
/src/main/java/com/project/Fashion
│
├── controller         # REST Controllers for API endpoints
├── model              # JPA Entity definitions (Product, Cart, Order, Message)
├── repository         # Interfaces for database access
├── service            # Business logic and helpers
└── FashionApplication.java  # Main class to launch the app

/src/main/resources
└── application.properties    # Configuration for DB and JPA

Notes on Development
The app currently uses a basic application.properties setup. Each developer should configure their own local credentials (not committed to Git).

Messaging and user features are modular and can be scaled or replaced with microservices.

Contribution
Pull requests are welcome! Please fork the repository and submit a PR from your feature branch.

License
This project is open-source.