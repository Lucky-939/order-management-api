package com.example.orders.service;

import com.example.orders.dto.OrderItemRequest;
import com.example.orders.dto.OrderResponse;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// Isolated unit tests for OrderService business logic with Mockito strict stubs
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("placeOrder with valid request decrements stocks, snapshots prices, and computes total")
    void placeOrder_validRequest_decrementsStockSnapshotsPriceAndComputesTotal() {
        // Arrange
        Product keyboard = new Product("Mechanical Keyboard", new BigDecimal("89.99"), 10);
        keyboard.setId(1L);
        Product mouse = new Product("Wireless Mouse", new BigDecimal("29.50"), 5);
        mouse.setId(2L);

        PlaceOrderRequest request = new PlaceOrderRequest("Alice Smith", Arrays.asList(
                new OrderItemRequest(1L, 2),
                new OrderItemRequest(2L, 1)
        ));

        when(productRepository.findById(1L)).thenReturn(Optional.of(keyboard));
        when(productRepository.findById(2L)).thenReturn(Optional.of(mouse));
        // Return argument so saved order maintains in-memory references
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OrderResponse response = orderService.placeOrder(request);

        // Assert
        assertEquals(8, keyboard.getStock());
        assertEquals(4, mouse.getStock());
        verify(productRepository).saveAndFlush(keyboard);
        verify(productRepository).saveAndFlush(mouse);
        assertEquals(2, response.getItems().size());
        assertEquals(0, new BigDecimal("89.99").compareTo(response.getItems().get(0).getUnitPrice()));
        assertEquals(0, new BigDecimal("29.50").compareTo(response.getItems().get(1).getUnitPrice()));
        // Expected total: (89.99 * 2) + (29.50 * 1) = 179.98 + 29.50 = 209.48
        assertEquals(0, new BigDecimal("209.48").compareTo(response.getTotalAmount()));
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("placeOrder with insufficient stock on second item throws and never saves order")
    void placeOrder_secondItemInsufficientStock_throwsAndNeverSavesOrder() {
        // Arrange
        Product keyboard = new Product("Mechanical Keyboard", new BigDecimal("89.99"), 10);
        keyboard.setId(1L);
        Product cable = new Product("USB-C Charging Cable", new BigDecimal("12.00"), 1);
        cable.setId(3L);

        PlaceOrderRequest request = new PlaceOrderRequest("Bob Jones", Arrays.asList(
                new OrderItemRequest(1L, 2),
                new OrderItemRequest(3L, 5)
        ));

        when(productRepository.findById(1L)).thenReturn(Optional.of(keyboard));
        when(productRepository.findById(3L)).thenReturn(Optional.of(cable));

        // Act & Assert
        assertThrows(InsufficientStockException.class, () -> orderService.placeOrder(request));

        // This proves the service stops and never saves the order; the database rollback itself is verified against real MySQL, and a Mockito test cannot prove it.
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("placeOrder with duplicate product IDs throws InvalidOrderRequest and touches no repository")
    void placeOrder_duplicateProductIds_throwsInvalidOrderRequestAndTouchesNoRepository() {
        // Arrange
        PlaceOrderRequest request = new PlaceOrderRequest("Duplicate Racer", Arrays.asList(
                new OrderItemRequest(1L, 1),
                new OrderItemRequest(1L, 2)
        ));

        // Act & Assert
        assertThrows(InvalidOrderRequestException.class, () -> orderService.placeOrder(request));

        // Validation happens before any repository lookup, preserving isolation
        verifyNoInteractions(productRepository, orderRepository);
    }

    @Test
    @DisplayName("placeOrder with unknown product ID throws ResourceNotFound")
    void placeOrder_unknownProduct_throwsResourceNotFound() {
        // Arrange
        PlaceOrderRequest request = new PlaceOrderRequest("Ghost Customer", Arrays.asList(
                new OrderItemRequest(999L, 1)
        ));

        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> orderService.placeOrder(request));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateStatus with illegal transition throws and leaves status unchanged")
    void updateStatus_illegalTransition_throwsAndLeavesStatusUnchanged() {
        // Arrange
        Order order = new Order("Customer", OrderStatus.PLACED, new BigDecimal("100.00"));
        order.setId(1L);

        when(orderRepository.findDetailById(1L)).thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(InvalidStatusTransitionException.class, () -> orderService.updateStatus(1L, OrderStatus.DELIVERED));
        assertEquals(OrderStatus.PLACED, order.getStatus());
    }

    @Test
    @DisplayName("updateStatus with legal transition changes status and relies on dirty checking")
    void updateStatus_legalTransition_changesStatus() {
        // Arrange
        Order order = new Order("Customer", OrderStatus.PLACED, new BigDecimal("100.00"));
        order.setId(1L);

        when(orderRepository.findDetailById(1L)).thenReturn(Optional.of(order));

        // Act
        OrderResponse response = orderService.updateStatus(1L, OrderStatus.SHIPPED);

        // Assert
        assertEquals(OrderStatus.SHIPPED, order.getStatus());
        assertEquals(OrderStatus.SHIPPED, response.getStatus());

        // dirty checking persists the change
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateStatus with unknown order ID throws ResourceNotFound")
    void updateStatus_unknownOrder_throwsResourceNotFound() {
        // Arrange
        when(orderRepository.findDetailById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> orderService.updateStatus(999L, OrderStatus.SHIPPED));
    }

    @Test
    @DisplayName("cancelOrder on PLACED order restores stock for every item and deletes order")
    void cancelOrder_placedOrder_restoresStockForEveryItemAndDeletesOrder() {
        // Arrange
        Product p1 = new Product("Product 1", new BigDecimal("10.00"), 7);
        p1.setId(1L);
        Product p2 = new Product("Product 2", new BigDecimal("20.00"), 13);
        p2.setId(2L);

        Order order = new Order("Customer", OrderStatus.PLACED, new BigDecimal("40.00"));
        order.setId(1L);

        OrderItem item1 = new OrderItem(order, p1, 2, new BigDecimal("10.00"));
        OrderItem item2 = new OrderItem(order, p2, 1, new BigDecimal("20.00"));
        order.addItem(item1);
        order.addItem(item2);

        when(orderRepository.findDetailById(1L)).thenReturn(Optional.of(order));

        // Act
        orderService.cancelOrder(1L);

        // Assert
        assertEquals(9, p1.getStock());
        assertEquals(14, p2.getStock());
        verify(orderRepository).delete(order);
    }

    @Test
    @DisplayName("cancelOrder on SHIPPED order throws and does not touch stock")
    void cancelOrder_shippedOrder_throwsAndDoesNotTouchStock() {
        // Arrange
        Product p1 = new Product("Product 1", new BigDecimal("10.00"), 7);
        p1.setId(1L);

        Order order = new Order("Customer", OrderStatus.SHIPPED, new BigDecimal("20.00"));
        order.setId(1L);

        OrderItem item1 = new OrderItem(order, p1, 2, new BigDecimal("10.00"));
        order.addItem(item1);

        when(orderRepository.findDetailById(1L)).thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(OrderNotCancellableException.class, () -> orderService.cancelOrder(1L));
        assertEquals(7, p1.getStock());
        verify(orderRepository, never()).delete(any());
    }

    @Test
    @DisplayName("cancelOrder with unknown order ID throws ResourceNotFound")
    void cancelOrder_unknownOrder_throwsResourceNotFound() {
        // Arrange
        when(orderRepository.findDetailById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> orderService.cancelOrder(999L));
    }
}
