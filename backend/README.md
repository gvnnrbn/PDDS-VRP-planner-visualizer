# Backend

## Prerequisites

### Database Setup

1. **Using Docker** (Recommended):
   ```bash
   # Start PostgreSQL container
   docker run --name postgres-db -e POSTGRES_PASSWORD=mysecretpassword -p 5432:5432 -d postgres
   ```

   This will:
   - Start a PostgreSQL container
   - Expose port 5432
   - Set default password to `mysecretpassword`
   - Use default database name `postgres`

2. **Manual Setup**:
   - PostgreSQL 14+
   - Database name: `postgres`
   - Username: `postgres`
   - Password: `mysecretpassword`
   - Port: `5432`

## Project Structure

```
src/main/java/pucp/pdds/backend/
├── controller/     # REST endpoints
├── model/         # Entity classes
├── repository/    # JPA repositories
├── seeder/        # Database seeding
└── BackendApplication.java # Spring Boot entry point
```

## Adding New Endpoint

### 1. Create Entity

```java
@Entity
@Table(name = "your_table")
public class YourEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Add your fields here
    private String field1;
    private int field2;
    
    // Getters and Setters
}
```

### 2. Create Repository

```java
public interface YourRepository extends JpaRepository<YourEntity, Long> {
    // Add custom query methods here
}
```

### 3. Create Controller

```java
@RestController
@RequestMapping("/api/your-entity")
public class YourController {
    
    @Autowired
    private YourRepository repository;
    
    // CRUD endpoints
    @GetMapping
    public ResponseEntity<List<YourEntity>> getAll() {
        return ResponseEntity.ok(repository.findAll());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<YourEntity> getById(@PathVariable Long id) {
        return repository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<YourEntity> create(@RequestBody YourEntity entity) {
        return ResponseEntity.ok(repository.save(entity));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<YourEntity> update(@PathVariable Long id, @RequestBody YourEntity entity) {
        return repository.findById(id)
            .map(existing -> {
                // Update fields
                return ResponseEntity.ok(repository.save(existing));
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return repository.findById(id)
            .map(existing -> {
                repository.delete(existing);
                return ResponseEntity.ok().build();
            })
            .orElse(ResponseEntity.notFound().build());
    }
}
```

### 4. Add Seeder (Optional)

```java
@Component
public class YourSeeder implements CommandLineRunner {
    
    @Autowired
    private YourRepository repository;
    
    public void run(String... args) throws Exception {
        repository.deleteAll();
        
        YourEntity entity = new YourEntity();
        // Set entity fields
        repository.save(entity);
    }
}
```

## Running the Application

### With Docker

1. Start PostgreSQL:
```bash
docker run --name postgres-db -e POSTGRES_PASSWORD=mysecretpassword -p 5432:5432 -d postgres
```

2. Build and run (if not using intellij idea):
```bash
./mvnw clean package
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

### Without Docker

1. Ensure PostgreSQL is running with:
   - Database: `postgres`
   - Username: `postgres`
   - Password: `mysecretpassword`
   - Port: `5432`

2. Build and run (if not using intellij idea):
```bash
./mvnw clean package
java -jar target/backend-0.0.1-SNAPSHOT.jar
```