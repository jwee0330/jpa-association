package persistence;

import database.DatabaseServer;
import database.H2;
import java.util.List;
import jdbc.JdbcTemplate;
import order.domain.Order;
import order.domain.OrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import persistence.config.InitSchema;
import persistence.config.PersistenceConfiguration;
import persistence.entity.EntityManager;

public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        logger.info("Starting application...");
        try {
            final DatabaseServer server = new H2();
            server.start();

            final JdbcTemplate jdbcTemplate = new JdbcTemplate(server.getConnection());
            PersistenceConfiguration persistenceConfiguration = new PersistenceConfiguration(
                    List.of(Order.class, OrderItem.class),
                    jdbcTemplate,
                    InitSchema.DROP_AND_CREATE
            );
            persistenceConfiguration.initiateDatabase();

            Order order = new Order(1L, "1000")
                    .addOrderItem(new OrderItem(1L, "item1", 5))
                    .addOrderItem(new OrderItem(2L, "item2", 3))
                    .addOrderItem(new OrderItem(3L, "item3", 10));
            jdbcTemplate.execute(order.convertToInsertQuery());

            EntityManager entityManager = persistenceConfiguration.getEntityManager();
            Order result = entityManager.find(Order.class, 1L);
            System.out.println(result);

            if (result.getOrderItems().size() == 3) {
                logger.info("OrderItem is loaded successfully");
            } else {
                logger.error("OrderItem is not loaded");
            }

            server.stop();
        } catch (Exception e) {
            logger.error("Error occurred", e);
        } finally {
            logger.info("Application finished");
        }
    }
}
