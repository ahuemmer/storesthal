package com.github.ahuemmer.storesthal.complextestobjects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.ahuemmer.storesthal.HALRelation;

import java.util.AbstractList;
import java.util.AbstractSequentialList;
import java.util.LinkedList;
import java.util.List;

/**
 * Like {@link ComplexObjectWithMultipleChildren5}, but having a parent relation itself.
 * Note, that the child collection is implemented as a {@link LinkedList} here!
 */
public class ComplexObjectWithMultipleChildren7 {

    @JsonProperty("category_id")
    private Integer categoryId;

    private Integer number;

    private String name;

    private Integer color;

    private List<String> types;

    private String comment;

    @HALRelation
    private ComplexObjectWithMultipleChildren7 parent;

    // Using AbstractList here just to prove, that Storesthal can also deal with abstract collection classes
    private AbstractList<ComplexObjectWithMultipleChildren7> children;

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

    public List<String> getTypes() {
        return types;
    }

    public void setType(List<String> types) {
        this.types = types;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public AbstractList<ComplexObjectWithMultipleChildren7> getChildren() {
        return children;
    }

    @HALRelation
    public void setChildren(AbstractSequentialList<ComplexObjectWithMultipleChildren7> children) {
        this.children = children;
    }

    public ComplexObjectWithMultipleChildren7 getParent() {
        return parent;
    }

    public void setParent(ComplexObjectWithMultipleChildren7 parent) {
        this.parent = parent;
    }
}

;