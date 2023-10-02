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
import persistence.dialect.H2DbDialect;
import persistence.sql.ddl.DdlQueryBuilder;
import persistence.sql.ddl.JavaToSqlColumnParser;

public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        logger.info("Starting application...");
        try {
            final DatabaseServer server = new H2();
            server.start();

            final JdbcTemplate jdbcTemplate = new JdbcTemplate(server.getConnection());
            JavaToSqlColumnParser javaToSqlColumnParser = new JavaToSqlColumnParser(new H2DbDialect());
            List<DdlQueryBuilder> ddlQueryBuilders = List.of(
                    new DdlQueryBuilder(javaToSqlColumnParser, Order.class),
                    new DdlQueryBuilder(javaToSqlColumnParser, OrderItem.class)
            );
            new PersistenceConfiguration(ddlQueryBuilders, jdbcTemplate, InitSchema.DROP_AND_CREATE).initiateDatabase();

            server.stop();
        } catch (Exception e) {
            logger.error("Error occurred", e);
        } finally {
            logger.info("Application finished");
        }
    }
}
