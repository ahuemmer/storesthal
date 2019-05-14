package de.huemmerich.web.wsobjectstore.complextestobjects;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ComplexChildWithParentRelationCollection {

    private int childId;

    @JsonProperty("name")
    private String childName;

    //Does this make sense? Not really in this case, but it does for testing collection handling.
    private List<ComplexObjectWithMultipleChildren5> parents;

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

    public List<ComplexObjectWithMultipleChildren5> getParents() {
        return parents;
    }

    public void setParents(List<ComplexObjectWithMultipleChildren5> parents) {
        this.parents = parents;
    }
}
