package com.github.ahuemmer.storesthal.complextestobjects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.ahuemmer.storesthal.HALRelation;

import java.util.LinkedList;

/**
 * Quite the same as {@link ComplexObject}, but having a relation to multiple child objects.
 * Note, that the child collection is implemented as a {@link LinkedList} here!
 */
public class ComplexObjectWithMultipleChildren2 {

        @JsonProperty("category_id")
        private Integer categoryId;

        private Integer number;

        private String name;

        private Integer color;

        private String type;

        private String comment;

        @HALRelation
        private LinkedList<ChildObject> children;

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

        public LinkedList<ChildObject> getChildren() {
        return children;
    }

        public void setChildren(LinkedList<ChildObject> children) {
        this.children = children;
    }
}