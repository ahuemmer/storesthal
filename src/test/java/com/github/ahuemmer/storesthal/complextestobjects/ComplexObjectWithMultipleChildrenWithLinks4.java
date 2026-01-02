package com.github.ahuemmer.storesthal.complextestobjects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.ahuemmer.storesthal.HALRelation;
import org.springframework.hateoas.EntityModel;

import java.util.List;

/**
 * Like {@link ComplexObjectWithMultipleChildren1}, but having children with (back-)references to their parent.
 * Note, that the child collection is implemented as an abstract {@link java.util.List} here!
 */
public class ComplexObjectWithMultipleChildrenWithLinks4 {

    @JsonProperty("category_id")
    private Integer categoryId;

    private Integer number;

    private String name;

    private Integer color;

    private String type;

    private String comment;

    @HALRelation("children")
    private List<EntityModel<ChildObjectWithParentRelationWithLinks>> children;

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getColor() {
        return color;
    }

    public void setColor(Integer color) {
        this.color = color;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<EntityModel<ChildObjectWithParentRelationWithLinks>> getChildren() {
        return children;
    }

    public void setChildren(List<EntityModel<ChildObjectWithParentRelationWithLinks>> children) {
        this.children = children;
    }
}