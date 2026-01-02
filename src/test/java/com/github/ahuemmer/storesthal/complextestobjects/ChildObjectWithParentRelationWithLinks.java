package com.github.ahuemmer.storesthal.complextestobjects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.ahuemmer.storesthal.HALRelation;
import org.springframework.hateoas.EntityModel;

/**
 * "Child" object, having an id, a name and a relation to its parent.
 */
public class ChildObjectWithParentRelationWithLinks {

    private int childId;

    @JsonProperty("name")
    private String childName;

    @HALRelation("parent")
    private EntityModel<ComplexObjectWithMultipleChildren4> parent;

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

    public EntityModel<ComplexObjectWithMultipleChildren4> getParent() {
        return parent;
    }

    public void setParent(EntityModel<ComplexObjectWithMultipleChildren4> parent) {
        this.parent = parent;
    }
}
