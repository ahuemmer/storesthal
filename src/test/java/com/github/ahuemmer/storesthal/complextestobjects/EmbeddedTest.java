package com.github.ahuemmer.storesthal.complextestobjects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EmbeddedTest {

    @JsonProperty("_embedded")
    private ObjectCollection objectCollection;

    public ObjectCollection getObjectCollection() {
        return objectCollection;
    }

    public void setObjectCollection(ObjectCollection objectCollection) {
        this.objectCollection = objectCollection;
    }
}
