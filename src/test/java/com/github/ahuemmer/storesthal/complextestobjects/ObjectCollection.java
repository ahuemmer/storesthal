package com.github.ahuemmer.storesthal.complextestobjects;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.hateoas.EntityModel;

import java.util.List;

public class ObjectCollection {

    @JsonProperty("objectCollection")
    private List<EntityModel<ChildObject>> objectCollection;

    public List<EntityModel<ChildObject>> getObjectCollection() {
        return objectCollection;
    }

    public void setObjectCollection(List<EntityModel<ChildObject>> objectCollection) {
        this.objectCollection = objectCollection;
    }

}
