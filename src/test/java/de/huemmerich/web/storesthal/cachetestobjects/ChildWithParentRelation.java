package de.huemmerich.web.storesthal.cachetestobjects;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.huemmerich.web.storesthal.Cacheable;

/**
 * Simple "child" test object with relation to a single parent object
 */
@Cacheable(cacheName = "children")
public class ChildWithParentRelation {

    private int childId;

    @JsonProperty("name")
    private String childName;

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
