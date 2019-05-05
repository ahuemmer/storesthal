package de.huemmerich.web.wsobjectstore;

public class HALObjectMetadata {

    private Class objectClass;
    private String objectBaseURL=null;

    public HALObjectMetadata(Class objectClass) {
        this(objectClass,null);
    }

    public HALObjectMetadata(Class objectClass, String baseUrl) {
        this.objectClass=objectClass;
        this.objectBaseURL = baseUrl;
    }

    public Class getObjectClass() {
        return objectClass;
    }

    public void setObjectClass(Class objectClass) {
        this.objectClass = objectClass;
    }

    public String getObjectBaseURL() {
        return objectBaseURL;
    }

    public void setObjectBaseURL(String objectBaseURL) {
        this.objectBaseURL = objectBaseURL;
    }
}
