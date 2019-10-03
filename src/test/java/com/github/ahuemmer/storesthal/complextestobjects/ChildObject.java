package com.github.ahuemmer.storesthal.complextestobjects;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Simple "child" object having an id and a name
 */
public class ChildObject {

    @JsonProperty("objectId")
    private int childId;

    @JsonProperty("name")
    private String childName;

    public int getChildId() {
        return childId;
    }

    public void setChildId(int childId) {
        this.childId = childId;
    }

    public String getChildName() {
        return childName;
    }

    public void setChildName(String childName) {
        this.childName = childName;
    }
}
