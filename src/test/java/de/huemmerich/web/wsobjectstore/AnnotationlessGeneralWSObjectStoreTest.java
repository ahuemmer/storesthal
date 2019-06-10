package de.huemmerich.web.wsobjectstore;

import de.huemmerich.web.wsobjectstore.complextestobjects.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AnnotationlessGeneralWSObjectStoreTest extends GeneralWSObjectStoreTest {

    @BeforeEach
    public void clearCaches() {
        WSObjectStoreConfiguration conf = new WSObjectStoreConfiguration();
        conf.setAnnotationless(true);
        WSObjectStore.init(conf);
        assertTrue(WSObjectStore.getConfiguration().isAnnotationless());
    }


}
