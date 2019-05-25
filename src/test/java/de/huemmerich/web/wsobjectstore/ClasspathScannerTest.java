package de.huemmerich.web.wsobjectstore;

import de.huemmerich.web.wsobjectstore.testpackage.SecondTestObject;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ClasspathScannerTest {

    @Test
    public void testClasspathScanner() {
        /*WSObjectStore store = new WSObjectStore();

        Set<HALObjectMetadata> classes = store.getHalObjectClasses();

        assertEquals(2,classes.size());
        for (HALObjectMetadata md: classes) {
            if ((md.getObjectClass()!=TestObject.class)&&(md.getObjectClass()!=SecondTestObject.class)) {
                fail();
            }
        }

        WSObjectStore store2 = new WSObjectStore("de.huemmerich.web.wsobjectstore.testpackage");

        Set<HALObjectMetadata> classes2 = store2.getHalObjectClasses();

        assertEquals(1,classes2.size());
        for (HALObjectMetadata md: classes2) {
            if (md.getObjectClass()!=SecondTestObject.class) {
                fail();
            }
        }

        WSObjectStore store3 = new WSObjectStore("de.huemmerich.web.wsobjectstore");

        Set<HALObjectMetadata> classes3 = store3.getHalObjectClasses();

        assertEquals(2,classes3.size());
        for (HALObjectMetadata md: classes3) {
            if ((md.getObjectClass()!=TestObject.class)&&(md.getObjectClass()!=SecondTestObject.class)) {
                fail();
            }
        }*/
    }

}
