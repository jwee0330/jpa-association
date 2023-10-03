package order.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String product;

    private Integer quantity;

    public OrderItem(Long id, String product, Integer quantity) {
        this.id = id;
        this.product = product;
        this.quantity = quantity;
    }

    public String toInsertQuery(Long orderId) {
        if (id == null || product == null || quantity == null || orderId == null) {
            throw new IllegalArgumentException();
        }
        return String.format("INSERT INTO order_items (id, product, quantity, order_id) VALUES (%d, '%s', %d, %d);", id, product, quantity, orderId);
    }
}
