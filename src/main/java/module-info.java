module wsobjectstore {
    exports de.huemmerich.web.wsobjectstore;
    exports de.huemmerich.web.wsobjectstore.impl;
    requires jackson.annotations;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires jersey.client;
    requires org.apache.logging.log4j;
    requires commons.lang3;
    requires jsr311.api;
}