package com.mansur.orderplatform.orderservice.product;

import com.mansur.orderplatform.orderservice.common.BaseEntity;
import com.mansur.orderplatform.orderservice.common.exception.InsufficientStockException;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Product with stock. Stock can only change through decreaseStock / increaseStock,
 * which enforce the rules (encapsulation). @Version protects against two orders
 * buying the last item at the same time (optimistic locking).
 */
@Entity
@Table(name = "products")
public class Product extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Version
    private Long version;

    protected Product() {
        // required by JPA
    }

    public Product(String name, String description, BigDecimal price, int stockQuantity) {
        update(name, description, price, stockQuantity);
    }

    public void update(String name, String description, BigDecimal price, int stockQuantity) {
        if (price == null || price.signum() <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
        if (stockQuantity < 0) {
            throw new IllegalArgumentException("Stock cannot be negative");
        }
        this.name = name;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
    }

    public void decreaseStock(int quantity) {
        requirePositive(quantity);
        if (stockQuantity < quantity) {
            throw new InsufficientStockException(name, stockQuantity, quantity);
        }
        stockQuantity -= quantity;
    }

    public void increaseStock(int quantity) {
        requirePositive(quantity);
        stockQuantity += quantity;
    }

    private static void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }
}
