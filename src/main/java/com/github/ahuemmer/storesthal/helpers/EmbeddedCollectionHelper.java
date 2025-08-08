package com.github.ahuemmer.storesthal.helpers;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.hateoas.EntityModel;

import java.util.List;

public class EmbeddedCollectionHelper<T> {

    @JsonProperty("_embedded")
    private ObjectCollection<T> objectCollection;

    public ObjectCollection<T> getObjectCollection() {
        return objectCollection;
    }

    public void setObjectCollection(ObjectCollection<T> objectCollection) {
        this.objectCollection = objectCollection;
    }

    public List<EntityModel<T>> getObjects() {
        return this.objectCollection.getObjectCollection();
    }

}

class ObjectCollection<T> {

    private List<EntityModel<T>> objectCollection;

    public List<EntityModel<T>> getObjectCollection() {
        return objectCollection;
    }

    public void setObjectCollection(List<EntityModel<T>> objectCollection) {
        this.objectCollection = objectCollection;
    }

}
