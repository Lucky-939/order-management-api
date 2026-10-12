package com.example.orders.repository;

import com.example.orders.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Spring Data JPA repository providing standard CRUD operations for Product entity
public interface ProductRepository extends JpaRepository<Product, Long> {

    // The database checks and changes stock in one statement, so there is no read-then-write gap; a JPQL bulk update skips @Version, so version is bumped by hand
    // Clearing would detach the Order loaded in cancelOrder and break its version check
    @Modifying(flushAutomatically = true)
    @Query("update Product p set p.stock = p.stock - :qty, p.version = p.version + 1 where p.id = :id and p.stock >= :qty")
    int decreaseStock(@Param("id") Long id, @Param("qty") int qty);

    // The database checks and changes stock in one statement, so there is no read-then-write gap; a JPQL bulk update skips @Version, so version is bumped by hand
    // Clearing would detach the Order loaded in cancelOrder and break its version check
    @Modifying(flushAutomatically = true)
    @Query("update Product p set p.stock = p.stock + :qty, p.version = p.version + 1 where p.id = :id")
    int increaseStock(@Param("id") Long id, @Param("qty") int qty);
}
