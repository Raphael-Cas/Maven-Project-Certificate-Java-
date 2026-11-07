# Maven-Project-Certificate-Java-

# Library (JDBC → JPA/Hibernate)

A console application for managing a library's books, built with **Maven**, **Jakarta Persistence (JPA)**, **Hibernate ORM** and **PostgreSQL** — no Spring Boot.

This project is a migration of an application that used plain JDBC. The menu, the `Scanner` input, the validations and the messages are exactly the same; **only the persistence layer changed**. That makes it a good side-by-side reference for learning what JPA/Hibernate replaces.

## Features

```
===== LIBRARY =====
1 - Register book
2 - List books
3 - Find book by ID
4 - Update book
5 - Delete book
0 - Exit
```

- Full CRUD for books, with input validation (required fields, valid numbers, positive year, `y`/`n` answers).
- Deleting a book that still has loans is refused by the database and reported with a friendly message.
- Database errors are reported in the menu instead of crashing the program.

## Tech stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 17+ | Language (the project is compiled with `release 17`) |
| Maven | 3.9+ | Build and dependency management |
| Jakarta Persistence API | 3.2.0 | JPA annotations and `EntityManager` |
| Hibernate ORM | 7.4.12.Final | JPA provider; generates all the SQL |
| PostgreSQL JDBC driver | 42.7.14 | Used internally by Hibernate |
| Logback | 1.5.38 | Logging (only errors are shown, so the console shows just the menu) |
| PostgreSQL | 17 (tested) | Database |

## Prerequisites

- JDK 17 or newer
- Maven 3.9 or newer
- A running PostgreSQL server with a database named `biblioteca`

## Database setup

Hibernate is configured with `hbm2ddl.auto=none`, so it **does not create or change tables**: the tables must already exist, just as they did with the JDBC version. A schema compatible with the entities:

```sql
CREATE TABLE livro (
    id             SERIAL PRIMARY KEY,
    titulo         VARCHAR(200) NOT NULL,
    autor          VARCHAR(150) NOT NULL,
    ano_publicacao INT          NOT NULL,
    disponivel     BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE usuario (
    id    SERIAL PRIMARY KEY,
    nome  VARCHAR(150) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL
);

CREATE TABLE emprestimo (
    id                      SERIAL PRIMARY KEY,
    livro_id                INT  NOT NULL REFERENCES livro(id),
    usuario_id              INT  NOT NULL REFERENCES usuario(id),
    data_emprestimo         DATE NOT NULL,
    data_prevista_devolucao DATE NOT NULL,
    data_devolucao          DATE
);
```

The menu only works with the `livro` table. `usuario` and `emprestimo` are mapped as entities (and the foreign key from `emprestimo` to `livro` is what makes the "book has loans" refusal work), but the menu does not use them yet.

> To let Hibernate manage the tables while studying, change `hibernate.hbm2ddl.auto` in `persistence.xml` (`validate`, `update`, `create`, `create-drop`). Be careful: `create` and `create-drop` destroy existing data, and `update` may also change column sizes.

## Configuration

The default connection settings are in
[`src/main/resources/META-INF/persistence.xml`](src/main/resources/META-INF/persistence.xml):

| Setting | Default |
|---|---|
| URL | `jdbc:postgresql://localhost:5432/biblioteca` |
| User | `postgres` |
| Password | `your_password` |

Do not commit your real password. Override the defaults with environment variables instead:

**Windows (PowerShell)**
```powershell
$env:DB_URL      = "jdbc:postgresql://localhost:5432/biblioteca"
$env:DB_USER     = "postgres"
$env:DB_PASSWORD = "your-real-password"
```

**Linux / macOS**
```bash
export DB_URL="jdbc:postgresql://localhost:5432/biblioteca"
export DB_USER="postgres"
export DB_PASSWORD="your-real-password"
```

## Running the application

Build and run the executable JAR (it already contains Hibernate, the PostgreSQL driver and Logback):

```bash
mvn clean package
java -jar target/biblioteca-jpa-1.0-SNAPSHOT.jar
```

Or run it directly through Maven:

```bash
mvn -q compile exec:java
```

`-q` hides Maven's own log lines so only the application menu is printed.

## Project structure

```
.
├── pom.xml
└── src
    ├── main
    │   ├── java/com/biblioteca
    │   │   ├── Main.java                      # menu, Scanner input, output
    │   │   ├── entity                         # JPA entities
    │   │   │   ├── Livro.java
    │   │   │   ├── Usuario.java
    │   │   │   └── Emprestimo.java
    │   │   ├── repository
    │   │   │   └── LivroRepository.java       # CRUD using only EntityManager
    │   │   ├── persistence
    │   │   │   ├── EntityManagerProvider.java # provides the EntityManager
    │   │   │   └── DatabaseException.java     # replaces SQLException in the menu
    │   │   └── util
    │   │       └── EnvUtil.java               # reads environment variables
    │   └── resources
    │       ├── META-INF/persistence.xml       # JPA / Hibernate configuration
    │       └── logback.xml                    # logging configuration
    └── test/java
```

## What changed from JDBC to JPA/Hibernate

| Concern | Plain JDBC (before) | JPA / Hibernate (now) |
|---|---|---|
| Getting a connection | `ConnectionFactory.getConnection()` with `DriverManager` | `EntityManagerProvider.getEntityManager()` |
| Writing SQL | Hand-written `INSERT` / `SELECT` / `UPDATE` / `DELETE` strings | Generated by Hibernate from the entity mapping |
| Executing queries | `PreparedStatement` + `ResultSet` | `persist`, `find`, `merge`, `remove` and one JPQL query |
| Row ↔ object mapping | Manual `mapRow` column by column | Annotations: `@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `@Column` |
| Generated id | `getGeneratedKeys()` | `@GeneratedValue(strategy = GenerationType.IDENTITY)` |
| Relationships | Foreign keys as plain `int` fields | `@ManyToOne` / `@OneToMany` / `@JoinColumn` |
| Transactions | Implicit (auto-commit) | Explicit `begin` / `commit` / `rollback` in the repository |
| Resource handling | try-with-resources on `Connection` | try-with-resources on `EntityManager` |
| Errors | `SQLException` | `DatabaseException` (same `getSQLState()` / `getMessage()` used by the menu) |

A few details worth knowing:

- **No cascade on `@OneToMany`.** Deleting a book that still has loans must keep being refused by PostgreSQL (SQLState `23503`). With `CascadeType.REMOVE`, the loans would be silently deleted instead.
- **`Emprestimo`** now holds `Livro livro` and `Usuario usuario` instead of `int livroId` / `int usuarioId`. `getLivroId()` and `getUsuarioId()` still return the same values.
- **Lazy `EntityManagerFactory`.** It is created on first use, so the menu opens even when the database is down and the error shows up when an operation needs the database, as before.
- **Seeing the SQL.** Set `hibernate.show_sql` to `true` in `persistence.xml` to print the SQL Hibernate generates. It is `false` by default so the console shows only the menu.

## Troubleshooting

| Message | Likely cause |
|---|---|
| `Database error: FATAL: password authentication failed...` (SQLState `28P01`) | Wrong user or password — set `DB_USER` / `DB_PASSWORD` |
| `Database error: Connection to localhost:5432 refused...` (SQLState `08001`) | PostgreSQL is not running or the URL/port is wrong |
| `Database error: FATAL: database "biblioteca" does not exist` (SQLState `3D000`) | Create the database first (`CREATE DATABASE biblioteca;`) |
| `Database error: ERROR: relation "livro" does not exist` (SQLState `42P01`) | The tables were not created — run the SQL from [Database setup](#database-setup) |
| `Operation refused: this book is referenced by existing loans.` | Expected: the book has rows in `emprestimo` |
