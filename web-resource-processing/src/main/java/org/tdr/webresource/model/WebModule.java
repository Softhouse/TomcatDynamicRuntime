package org.tdr.webresource.model;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Web Module. A pluggable unit that is a composite in a web resource.
 *
 * @author nic
 */
public class WebModule implements Comparable<WebModule> {

    private final ResourceType type;
    private final String sourcePath;
    private final String name;
    private final int priority;
    private final List<String> resourcesPaths = new ArrayList<>();

    public WebModule(ResourceType type, String name, String sourcePath, int priority) {
        this.type = type;
        this.name = name;
        this.sourcePath = sourcePath;
        this.priority = priority;
    }

    public ResourceType getType() {
        return this.type;
    }

    public String getName() {
        return this.name;
    }

    public String getSourcePath() {
        return this.sourcePath;
    }

    public int getPriority() {
        return this.priority;
    }

    public List<String> getResourcePaths() {
        return this.resourcesPaths;
    }

    public void addResourcePath(String resourcePath) {
        this.resourcesPaths.add(resourcePath);
    }

    @Override
    public int compareTo(WebModule o) {
        return this.priority-o.priority;
    }
}
