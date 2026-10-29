# ADR-004: JPA Query Optimization and N+1 Query Prevention

## Status
Accepted

## Date
2026-10-30

## Context
In `catalog-service`, customers frequently list restaurants and view their menu items. A naive implementation using Spring Data JPA defaults (`@OneToMany(fetch = FetchType.LAZY)` with standard `findAll()`) causes the **N+1 query problem**:
- 1 initial query to select $N$ restaurants: `SELECT * FROM restaurants;`
- $N$ additional queries when accessing the `menuItems` collection for each restaurant: `SELECT * FROM menu_items WHERE restaurant_id = ?;`
For 100 restaurants, this results in 101 separate round-trips to PostgreSQL, exhausting the HikariCP connection pool and creating catastrophic database latency under high load.

## Options Considered

### Option 1: Eager Fetching (`FetchType.EAGER`)
- **Mechanism:** Set `@OneToMany(fetch = FetchType.EAGER)` on the entity mapping.
- **Why Rejected:**
  - Standard JPQL `findAll()` queries still execute N+1 queries under JPA specification rules.
  - Forces menu items to be loaded in memory even for lightweight queries that only need the restaurant name (e.g., admin drop-downs), bloating memory.

### Option 2: Open Session In View (OSIV)
- **Mechanism:** Keep database connection open throughout the web request lifecycle so Hibernate can lazily load collections during JSON serialization.
- **Why Rejected:** Masking N+1 queries inside serialization code drains the database connection pool while Tomcat writes JSON to slow network sockets. We explicitly set `spring.jpa.open-in-view: false` in Stage 0.

### Option 3: JPA `@EntityGraph` and `LEFT JOIN FETCH` (Chosen)
- **Mechanism:** Keep `@OneToMany` lazy by default. Explicitly instruct Hibernate to fetch associations using an SQL `LEFT JOIN` on specific query methods via `@EntityGraph(attributePaths = {"menuItems"})` or JPQL `LEFT JOIN FETCH r.menuItems`.
- **Pros:**
  - **Single SQL Query:** Fetches all restaurants and their child menu items in exactly 1 round-trip.
  - **Granular Control:** Lightweight queries load only the restaurant; catalog viewing queries load the full graph.
- **Trade-off:** Cartesian product when joining multiple collections (handled by returning `Set` or distinct root entities).

## Decision
We choose **Option 3: Default Lazy Loading + Query-Level `@EntityGraph` / `JOIN FETCH`**.

## Consequences
### Positive
- Reduces database round-trips from $O(N)$ to $O(1)$.
- Guarantees predictable response times independent of the number of child entities.
- Compatible with disabled OSIV (`open-in-view: false`).

### Negative / Trade-offs
- Developers must explicitly declare `@EntityGraph` on query methods that require child associations.

## Code and Config Affected
- `services/catalog-service/src/main/java/com/swiggy/catalog/model/Restaurant.java`
- `services/catalog-service/src/main/java/com/swiggy/catalog/model/MenuItem.java`
- `services/catalog-service/src/main/java/com/swiggy/catalog/repository/RestaurantRepository.java`
