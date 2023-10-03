package order.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number")
    private String orderNumber;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id")
    private List<OrderItem> orderItems;

    protected Order() {
    }

    public Order(Long id, String orderNumber) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.orderItems = new ArrayList<>();
    }

    public Order addOrderItem(OrderItem orderItem) {
        orderItems.add(orderItem);
        return this;
    }

    public List<OrderItem> getOrderItems() {
        return orderItems;
    }

    public String convertToInsertQuery() {
        if (id == null || orderNumber == null) {
            throw new IllegalArgumentException();
        }
        StringBuilder query = new StringBuilder();
        query.append(String.format("INSERT INTO orders (id, order_number) VALUES (%d, '%s');", id, orderNumber));
        orderItems.stream()
                .map(e -> e.toInsertQuery(id))
                .forEach(query::append);
        return query.toString();
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", orderNumber='" + orderNumber + '\'' +
                ", orderItems=" + orderItems +
                '}';
    }
}
