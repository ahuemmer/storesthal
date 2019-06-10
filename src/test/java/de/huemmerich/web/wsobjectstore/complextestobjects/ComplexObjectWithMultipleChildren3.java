package de.huemmerich.web.wsobjectstore.complextestobjects;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.huemmerich.web.wsobjectstore.HALRelation;

import java.util.LinkedList;

public class ComplexObjectWithMultipleChildren3 {

        @JsonProperty("category_id")
        protected Integer categoryId;

        protected Integer number;

        protected String name;

        protected Integer color;

        protected String type;

        protected String comment;

        @HALRelation
        protected ComplexChild2[] children;

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

        public ComplexChild2[] getChildren() {
        return children;
    }

        public void setChildren(ComplexChild2[] children) {
        this.children = children;
    }
}