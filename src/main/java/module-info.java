module wsobjectstore {
    exports de.huemmerich.web.wsobjectstore;
    exports de.huemmerich.web.wsobjectstore.impl;
    requires jackson.annotations;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires jersey.client;
    requires log4j.api;
    requires java.ws.rs;
    requires commons.lang3;
}