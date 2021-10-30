package com.github.ahuemmer.storesthal.cachetestobjects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.ahuemmer.storesthal.Cacheable;
import com.github.ahuemmer.storesthal.HALRelation;

/**
 * Simple "child" test object with relation to a single parent object
 */
@Cacheable(cacheName = "children")
public class ChildWithParentRelation {

    private int childId;

    @JsonProperty("name")
    private String childName;

    @HALRelation("parent")
    private ParentObject parent;

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

    public ParentObject getParent() {
        return parent;
    }

    public void setParent(ParentObject parent) {
        this.parent = parent;
    }
}
