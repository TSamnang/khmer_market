package com.example.khmer_market.business;

import com.example.khmer_market.domain.entity.*;
import com.example.khmer_market.domain.enums.OrderStatus;
import com.example.khmer_market.domain.enums.UserRole;
import com.example.khmer_market.domain.enums.UserStatus;
import com.example.khmer_market.exception.InvalidOrderTransitionException;
import com.example.khmer_market.repository.*;
import com.example.khmer_market.service.OrderStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class OrderStateMachineTest {

    @Autowired
    private OrderStateMachine orderStateMachine;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductPackageRepository productPackageRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    private Order testOrder;
    private Inventory testInventory;

    @BeforeEach
    void setUp() {
        User seller = new User();
        seller.setUsername("statemachineseller");
        seller.setEmail("seller_sm@example.com");
        seller.setPassword("ValidPassword123!");
        seller.setRole(UserRole.FARMER);
        seller.setStatus(UserStatus.ACTIVE);
        seller = userRepository.saveAndFlush(seller);

        User buyer = new User();
        buyer.setUsername("statemachinebuyer");
        buyer.setEmail("buyer_sm@example.com");
        buyer.setPassword("ValidPassword123!");
        buyer.setRole(UserRole.BUYER);
        buyer.setStatus(UserStatus.ACTIVE);
        buyer = userRepository.saveAndFlush(buyer);

        Address address = new Address();
        address.setUser(buyer);
        address.setName("Buyer");
        address.setPhone("012345678");
        address.setProvince("Kandal");
        address = addressRepository.saveAndFlush(address);

        Product product = new Product();
        product.setSeller(seller);
        product.setName("Organic Mango");
        product = productRepository.saveAndFlush(product);

        ProductPackage pkg = new ProductPackage();
        pkg.setProduct(product);
        pkg.setPackageName("5kg Box");
        pkg.setPrice(new BigDecimal("10.00"));
        pkg = productPackageRepository.saveAndFlush(pkg);

        testInventory = new Inventory(pkg, 100, 5);
        testInventory.setReservedQuantity(5); // 5 units reserved
        testInventory = inventoryRepository.saveAndFlush(testInventory);

        testOrder = new Order();
        testOrder.setBuyer(buyer);
        testOrder.setAddress(address);
        testOrder.setOrderNumber("KM-TEST-SM01");
        testOrder.setSubtotal(new BigDecimal("50.00"));
        testOrder.setTotalAmount(new BigDecimal("50.00"));
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder = orderRepository.saveAndFlush(testOrder);

        OrderItem item = new OrderItem();
        item.setOrder(testOrder);
        item.setProductPackage(pkg);
        item.setSeller(seller);
        item.setPackageName("5kg Box");
        item.setUnitPrice(new BigDecimal("10.00"));
        item.setQuantity(5);
        item.setSubtotal(new BigDecimal("50.00"));
        item = orderItemRepository.saveAndFlush(item);
        testOrder.addItem(item);
    }

    @Test
    @DisplayName("Valid lifecycle: pending -> confirmed -> shipped -> delivered")
    void testValidLifecycle() {
        // 1. Confirm
        orderStateMachine.confirmOrder(testOrder, UserRole.FARMER);
        assertEquals(OrderStatus.CONFIRMED, testOrder.getStatus());

        // 2. Ship (Stock deducted: quantity -= 5, reserved -= 5)
        orderStateMachine.shipOrder(testOrder, UserRole.FARMER);
        assertEquals(OrderStatus.SHIPPED, testOrder.getStatus());

        Inventory updatedInv = inventoryRepository.findByProductPackageId(testInventory.getProductPackage().getId()).orElseThrow();
        assertEquals(95, updatedInv.getQuantity(), "Physical stock should be deducted from 100 to 95");
        assertEquals(0, updatedInv.getReservedQuantity(), "Reserved stock should be decremented from 5 to 0");

        // 3. Deliver
        orderStateMachine.deliverOrder(testOrder, UserRole.BUYER);
        assertEquals(OrderStatus.DELIVERED, testOrder.getStatus());
    }

    @Test
    @DisplayName("Invalid transition: Cannot ship directly from pending")
    void testInvalidTransitionPendingToShipped() {
        assertThrows(InvalidOrderTransitionException.class, () ->
                orderStateMachine.shipOrder(testOrder, UserRole.FARMER));
    }

    @Test
    @DisplayName("Cancelling pending order releases reservation")
    void testCancelPendingReleasesReservation() {
        orderStateMachine.cancelOrder(testOrder, UserRole.BUYER, "Changed mind");
        assertEquals(OrderStatus.CANCELLED, testOrder.getStatus());

        Inventory updatedInv = inventoryRepository.findByProductPackageId(testInventory.getProductPackage().getId()).orElseThrow();
        assertEquals(100, updatedInv.getQuantity(), "Physical stock unchanged");
        assertEquals(0, updatedInv.getReservedQuantity(), "Reserved stock released to 0");
    }

    @Test
    @DisplayName("Buyer cannot cancel confirmed or shipped orders")
    void testBuyerCannotCancelConfirmedOrder() {
        orderStateMachine.confirmOrder(testOrder, UserRole.FARMER);
        assertThrows(InvalidOrderTransitionException.class, () ->
                orderStateMachine.cancelOrder(testOrder, UserRole.BUYER, "Too late"));
    }
}
