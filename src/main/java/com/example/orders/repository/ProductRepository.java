package com.example.orders.repository;

import com.example.orders.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

// Spring Data JPA repository providing standard CRUD operations for Product entity
public interface ProductRepository extends JpaRepository<Product, Long> {
}
