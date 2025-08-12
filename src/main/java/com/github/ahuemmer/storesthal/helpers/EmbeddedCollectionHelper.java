package com.github.ahuemmer.storesthal.helpers;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.hateoas.EntityModel;

import java.util.List;

import static com.github.ahuemmer.storesthal.configuration.StoresthalConfiguration.EMBEDDED_PARENT_NAME;

public class EmbeddedCollectionHelper<T> {

    @JsonProperty(EMBEDDED_PARENT_NAME)
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
