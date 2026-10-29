package com.swiggy.catalog.repository;

import com.swiggy.catalog.model.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA Repository for MenuItem entities.
 */
@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {

    List<MenuItem> findByRestaurantIdAndIsAvailableTrue(UUID restaurantId);
}
