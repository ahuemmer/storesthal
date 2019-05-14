package de.huemmerich.web.wsobjectstore.complextestobjects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ComplexChildWithParentRelation {

    private int childId;

    @JsonProperty("name")
    private String childName;

    private ComplexObjectWithMultipleChildren4 parent;

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

    public ComplexObjectWithMultipleChildren4 getParent() {
        return parent;
    }

    public void setParent(ComplexObjectWithMultipleChildren4 parent) {
        this.parent = parent;
    }
}
