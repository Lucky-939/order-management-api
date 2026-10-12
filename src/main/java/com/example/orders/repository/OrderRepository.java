package com.example.orders.repository;

import com.example.orders.entity.Order;
import com.example.orders.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// Spring Data JPA repository providing standard and custom query methods for Order entity
public interface OrderRepository extends JpaRepository<Order, Long> {

    // One SQL query loads the order, its items and their products, which avoids N+1 and LazyInitializationException
    @Query("select o from Order o left join fetch o.items i left join fetch i.product where o.id = :id")
    Optional<Order> findDetailById(@Param("id") Long id);

    // Fetch-joining a collection with pagination makes Hibernate paginate in memory, so derived paged query uses standard pagination
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
}
