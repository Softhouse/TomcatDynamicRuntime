package org.tdr.webresource.model;

/**
 * @author nic
 */
public enum ResourceType {

    CSS,
    LESS,
    JAVASCRIPT,
    IMAGE;

    static public ResourceType fromString(String typeStr) {
        return valueOf(typeStr.toUpperCase()) ;
    }
}
