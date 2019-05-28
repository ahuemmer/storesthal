package de.huemmerich.web.wsobjectstore.cachetestobjects;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.huemmerich.web.wsobjectstore.Cacheable;
import de.huemmerich.web.wsobjectstore.complextestobjects.ComplexObjectWithMultipleChildren4;

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
