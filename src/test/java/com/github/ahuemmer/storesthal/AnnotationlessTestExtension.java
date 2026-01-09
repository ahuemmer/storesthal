package com.github.ahuemmer.storesthal;

import com.github.ahuemmer.storesthal.configuration.StoreresthalConfigurationFactory;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AnnotationlessTestExtension implements BeforeEachCallback {
    @Override
    public void beforeEach(ExtensionContext context) {
        Storesthal.init(new StoreresthalConfigurationFactory().setAnnotationless(true).getConfiguration());
        assertTrue(Storesthal.getConfiguration().isAnnotationless());
    }
}
