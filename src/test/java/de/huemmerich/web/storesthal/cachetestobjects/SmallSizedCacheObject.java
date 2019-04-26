package de.huemmerich.web.storesthal.cachetestobjects;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.huemmerich.web.storesthal.Cacheable;

import java.util.Objects;

/**
 * An object using a cache with a size of only five items.
 */
@Cacheable(cacheName = "de.huemmerich.web.wsobjectstore.cachetestobjects.SmallSizedCacheObject", cacheSize = 5)
public class SmallSizedCacheObject {

    @JsonProperty("objectId")
    private int id;

    private String name;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SmallSizedCacheObject that = (SmallSizedCacheObject) o;

        if (id != that.id) return false;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

}
