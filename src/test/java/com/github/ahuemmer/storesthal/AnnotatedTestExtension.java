package com.github.ahuemmer.storesthal;

import com.github.ahuemmer.storesthal.configuration.StoresthalConfigurationFactory;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class AnnotatedTestExtension implements BeforeEachCallback {
    @Override
    public void beforeEach(ExtensionContext context) {
        Storesthal.init(new StoresthalConfigurationFactory().setAnnotationless(false).getConfiguration());
        assertFalse(Storesthal.getConfiguration().isAnnotationless());
    }
}
