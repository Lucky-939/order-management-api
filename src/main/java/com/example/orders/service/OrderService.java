package com.example.orders.service;

import com.example.orders.dto.OrderItemRequest;
import com.example.orders.dto.OrderResponse;
import com.example.orders.dto.OrderSummaryResponse;
import com.example.orders.dto.PagedResponse;
import com.example.orders.dto.PlaceOrderRequest;
import com.example.orders.entity.Order;
import com.example.orders.entity.OrderItem;
import com.example.orders.entity.OrderStatus;
import com.example.orders.entity.Product;
import com.example.orders.exception.InsufficientStockException;
import com.example.orders.exception.InvalidOrderRequestException;
import com.example.orders.exception.InvalidStatusTransitionException;
import com.example.orders.exception.OrderNotCancellableException;
import com.example.orders.exception.ResourceNotFoundException;
import com.example.orders.repository.OrderRepository;
import com.example.orders.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

// Service component handling order placement, retrieval, and pagination workflows
@Service
public class OrderService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    // Constructor injection guarantees required dependencies are provided and immutably held
    public OrderService(ProductRepository productRepository, OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    // Ensures all stock updates, order creation, and child item inserts commit or roll back atomically
    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        // Step a: Reject duplicate productIds in the request
        Set<Long> seenProductIds = new HashSet<>();
        for (OrderItemRequest itemReq : request.getItems()) {
            if (!seenProductIds.add(itemReq.getProductId())) {
                throw new InvalidOrderRequestException("Duplicate productId found in order: " + itemReq.getProductId());
            }
        }

        // Step b: Create the Order with customerName, PLACED status, and temporary total amount of ZERO
        Order order = new Order(request.getCustomerName(), OrderStatus.PLACED, BigDecimal.ZERO);

        // Step c: Loop over the items IN ORDER, resolving products, validating/decrementing stock, and building line items
        BigDecimal runningTotal = BigDecimal.ZERO;
        for (OrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemReq.getProductId()));

            if (product.getStock() < itemReq.getQuantity()) {
                throw new InsufficientStockException("Insufficient stock for product id " + product.getId()
                        + " (" + product.getName() + "). Available: " + product.getStock()
                        + ", requested: " + itemReq.getQuantity());
            }

            // Decrement stock immediately so subsequent validation failures test transaction rollback of earlier decrements
            product.setStock(product.getStock() - itemReq.getQuantity());

            // flush now so the UPDATE is sent to MySQL inside the transaction; this makes a later failure exercise a real rollback instead of discarding an unsent change.
            productRepository.saveAndFlush(product);

            // Create OrderItem taking an immutable price snapshot from the product at purchase time
            OrderItem orderItem = new OrderItem(order, product, itemReq.getQuantity(), product.getPrice());
            order.addItem(orderItem);

            // Compute line total using BigDecimal arithmetic to avoid precision issues
            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            runningTotal = runningTotal.add(itemTotal);
        }

        // Step d: Set order total, persist order (cascades to order_items), and return response DTO
        order.setTotalAmount(runningTotal);
        Order savedOrder = orderRepository.save(order);
        return OrderResponse.from(savedOrder);
    }

    // items and products are lazy and open-in-view is false, so the DTO must be built inside a transaction; readOnly skips dirty checking.
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long id) {
        Order order = orderRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        return OrderResponse.from(order);
    }

    // readOnly skips dirty checking; a deterministic sort keeps pages stable
    @Transactional(readOnly = true)
    public PagedResponse<OrderSummaryResponse> listOrders(OrderStatus status, int page, int size) {
        // a deterministic sort keeps pages stable
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<Order> orderPage;
        if (status == null) {
            orderPage = orderRepository.findAll(pageable);
        } else {
            orderPage = orderRepository.findByStatus(status, pageable);
        }
        Page<OrderSummaryResponse> summaryPage = orderPage.map(OrderSummaryResponse::from);
        return PagedResponse.from(summaryPage);
    }

    // items and products are lazy and the response needs them inside the transaction.
    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus newStatus) {
        Order order = orderRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        if (!order.getStatus().canTransitionTo(newStatus)) {
            throw new InvalidStatusTransitionException("Cannot change status from " + order.getStatus() + " to " + newStatus);
        }

        order.setStatus(newStatus);
        // the order is managed inside the transaction, so dirty checking writes the UPDATE at commit.
        return OrderResponse.from(order);
    }

    // stock restore and the delete share one transaction so both happen or neither
    @Transactional
    public void cancelOrder(Long id) {
        Order order = orderRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        // Check the status before touching any stock to prevent invalid state modification
        if (order.getStatus() != OrderStatus.PLACED) {
            throw new OrderNotCancellableException("Only PLACED orders can be cancelled; current status: " + order.getStatus());
        }

        // Restore product stock using an explicit loop without streams
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
        }
        // the items are removed by cascade = ALL / orphanRemoval, so there is no OrderItemRepository.
        orderRepository.delete(order);
    }
}
