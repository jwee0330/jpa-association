package order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderTest {
    @DisplayName("하나의 주문은 여러개의 아이템을 가질 수 있다.")
    @Test
    public void createOrder() {
        Order order = new Order(1L, "1");
        order = order.addOrderItem(new OrderItem(1L, "상품1", 10));
        order = order.addOrderItem(new OrderItem(2L, "상품2", 10));

        assertThat(order).isNotNull();
        assertThat(order.getOrderItems()).isNotNull();
        assertThat(order.getOrderItems().size()).isEqualTo(2);
    }
}
