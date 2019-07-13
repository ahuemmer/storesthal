package de.huemmerich.web.wsobjectstore;

import de.huemmerich.web.wsobjectstore.configuration.WSObjectStoreConfiguration;
import de.huemmerich.web.wsobjectstore.configuration.WSObjectStoreConfigurationFactory;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pretty much the same as {@link GeneralWSObjectStoreTest}, but working annotationless!
 */
public class AnnotationlessGeneralWSObjectStoreTest extends GeneralWSObjectStoreTest {

    /**
     * Override {@link BeforeEach}-annotated method to make sure, annotationless mode is used.
     */
    @BeforeEach
    public void init() {
        WSObjectStore.init(new WSObjectStoreConfigurationFactory().setAnnotationless(true).getConfiguration());
        assertTrue(WSObjectStore.getConfiguration().isAnnotationless());
    }

}
