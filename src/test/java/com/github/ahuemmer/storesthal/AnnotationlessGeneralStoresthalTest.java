package com.github.ahuemmer.storesthal;

import com.github.ahuemmer.storesthal.configuration.StoreresthalConfigurationFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

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
        Assertions.assertTrue(Storesthal.getConfiguration().isAnnotationless());
    }

}
