package de.huemmerich.web.wsobjectstore;

import de.huemmerich.web.wsobjectstore.configuration.WSObjectStoreConfiguration;
import de.huemmerich.web.wsobjectstore.configuration.WSObjectStoreConfigurationFactory;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

public class AnnotationlessGeneralWSObjectStoreTest extends GeneralWSObjectStoreTest {

    @BeforeEach
    public void clearCaches() {

        WSObjectStore.init(new WSObjectStoreConfigurationFactory().setAnnotationless(true).getConfiguration());
        assertTrue(WSObjectStore.getConfiguration().isAnnotationless());
    }


}
