package de.huemmerich.web.wsobjectstore;

public class WSObjectStoreConfiguration {

    public WSObjectStoreConfiguration(){}

    private int defaultCacheSize=1000;

    public int getDefaultCacheSize() {
        return defaultCacheSize;
    }

    public void setDefaultCacheSize(int defaultCacheSize) {
        this.defaultCacheSize = defaultCacheSize;
    }
}
