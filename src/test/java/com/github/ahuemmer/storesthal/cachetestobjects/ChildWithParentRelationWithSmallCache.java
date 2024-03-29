package com.github.ahuemmer.storesthal.cachetestobjects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.ahuemmer.storesthal.Cacheable;
import com.github.ahuemmer.storesthal.HALRelation;

/**
 * Simple "child" object with relation to one single parent object, having a small cache size.
 */
@Cacheable(cacheName = "children")
public class ChildWithParentRelationWithSmallCache {

    private int childId;

    @JsonProperty("name")
    private String childName;

    @HALRelation("parent")
    private SmallSizedCacheObject parent;

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

    public SmallSizedCacheObject getParent() {
        return parent;
    }

    public void setParent(SmallSizedCacheObject parent) {
        this.parent = parent;
    }
}
