package de.huemmerich.web.storesthal.complextestobjects;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.huemmerich.web.storesthal.HALRelation;

import java.util.LinkedList;

/**
 * Like {@link ComplexObjectWithMultipleChildren5}, but having a parent relation itself.
 * Note, that the child collection is implemented as a {@link LinkedList} here!
 */
public class ComplexObjectWithMultipleChildren6 {

    @JsonProperty("category_id")
    private Integer categoryId;

    private Integer number;

    private String name;

    private Integer color;

    private String type;

    private String comment;

    @HALRelation
    private ComplexObjectWithMultipleChildren6 parent;

    private LinkedList<ComplexObjectWithMultipleChildren6> children;

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

    public LinkedList<ComplexObjectWithMultipleChildren6> getChildren() {
        return children;
    }

    @HALRelation
    public void setChildren(LinkedList<ComplexObjectWithMultipleChildren6> children) {
        this.children = children;
    }

    public ComplexObjectWithMultipleChildren6 getParent() {
        return parent;
    }

    public void setParent(ComplexObjectWithMultipleChildren6 parent) {
        this.parent = parent;
    }
}
