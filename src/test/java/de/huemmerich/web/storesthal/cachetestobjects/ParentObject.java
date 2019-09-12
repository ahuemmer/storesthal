package de.huemmerich.web.storesthal.cachetestobjects;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.huemmerich.web.storesthal.Cacheable;
import de.huemmerich.web.storesthal.complextestobjects.ChildObjectWithParentRelation;

import java.util.List;

/**
 * "Parent" object having some more fields being read as well.
 */
@Cacheable(cacheName = "parents")
public class ParentObject {

        @JsonProperty("category_id")
        private Integer categoryId;

        private Integer number;

        private String name;

        private Integer color;

        private String type;

        private String comment;

        private List<ChildObjectWithParentRelation> children;

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

        public List<ChildObjectWithParentRelation> getChildren() {
        return children;
    }

        public void setChildren(List<ChildObjectWithParentRelation> children) {
        this.children = children;
    }
}