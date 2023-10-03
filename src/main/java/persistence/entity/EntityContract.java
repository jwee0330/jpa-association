package persistence.entity;

import java.util.Map;
import persistence.config.EntityMetaData;

public interface EntityContract {
    Map<Class<?>, EntityMetaData<?>>  getEntityMetaDataMap();
}
