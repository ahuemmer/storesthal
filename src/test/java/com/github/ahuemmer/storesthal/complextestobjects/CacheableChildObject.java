package com.github.ahuemmer.storesthal.complextestobjects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.ahuemmer.storesthal.Cacheable;

import java.util.List;

/**
 * Simple "child" object having an id and a name, cacheable as set by annotation
 */
@Cacheable(cacheName = "test", collectionCacheName = "test-collection")
public class CacheableChildObject {

    @JsonProperty("objectId")
    private int childId;

    @JsonProperty("name")
    private String childName;

    private List<String> tags;

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

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
