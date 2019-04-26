package de.huemmerich.web.storesthal;

import de.huemmerich.web.storesthal.configuration.StoreresthalConfigurationFactory;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pretty much the same as {@link GeneralStoresthalTest}, but working annotationless!
 */
public class AnnotationlessGeneralStoresthalTest extends GeneralStoresthalTest {

    /**
     * Override {@link BeforeEach}-annotated method to make sure, annotationless mode is used.
     */
    @BeforeEach
    public void init() {
        Storesthal.init(new StoreresthalConfigurationFactory().setAnnotationless(true).getConfiguration());
        assertTrue(Storesthal.getConfiguration().isAnnotationless());
    }

}
