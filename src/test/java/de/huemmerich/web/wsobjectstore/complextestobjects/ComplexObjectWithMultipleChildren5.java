package de.huemmerich.web.wsobjectstore.complextestobjects;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.huemmerich.web.wsobjectstore.HALRelation;

import java.util.List;

/**
 * Like {@link ComplexObjectWithMultipleChildren1}, but having children with (back-)references to their parent.
 * Note, that the child collection is implemented as an abstract {@link List} here and the {@link HALRelation}
 * annotation is present.
 */
public class ComplexObjectWithMultipleChildren5 {

        @JsonProperty("category_id")
        private Integer categoryId;

        private Integer number;

        private String name;

        private Integer color;

        private String type;

        private String comment;

        private List<ChildObjectWithParentRelationCollection> children;

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

        public List<ChildObjectWithParentRelationCollection> getChildren() {
        return children;
    }

        @HALRelation
        public void setChildren(List<ChildObjectWithParentRelationCollection> children) {
        this.children = children;
    }
}