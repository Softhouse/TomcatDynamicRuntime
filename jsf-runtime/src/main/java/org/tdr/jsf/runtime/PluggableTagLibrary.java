package org.tdr.jsf.runtime;

import com.sun.facelets.tag.TagConfig;
import com.sun.facelets.tag.TagHandler;
import com.sun.facelets.tag.TagLibrary;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import javax.faces.FacesException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Pluggable Tag Library. Is used to have implementations of different tags be pluggable.
 *
 * User: nic
 */
public class PluggableTagLibrary implements TagLibrary {

    private static final Log log = LogFactory.getLog(PluggableTagLibrary.class);

    private final String namespace;
    private final Collection<TagLibrary> tagLibraries = new ArrayList<TagLibrary>();

    public PluggableTagLibrary(String namespace) {
        this.namespace = namespace;
    }

    public void addConcreteTagLibrary(TagLibrary tagLibrary) {
        this.tagLibraries.add(tagLibrary);
    }

    public void removeConcreteTagLibrary(TagLibrary tagLibrary) {
        this.tagLibraries.remove(tagLibrary);
    }

    public boolean containsConcreteTagLibrary(TagLibrary tagLibrary) {
        return this.tagLibraries.contains(tagLibrary);
    }

    public boolean isEmpty() {
        return this.tagLibraries.isEmpty();
    }

    @Override
    public boolean containsNamespace(String ns) {
        return this.namespace.equals(ns);
    }

    @Override
    public boolean containsTagHandler(String ns, String localName) {
        if ( this.namespace.equals(ns) ) {
            for ( TagLibrary tagLibrary : this.tagLibraries ) {
                if ( tagLibrary.containsTagHandler(ns, localName) ) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public TagHandler createTagHandler(String ns, String localName, TagConfig tag) throws FacesException {
        if ( this.namespace.equals(ns) ) {
            for ( TagLibrary tagLibrary : this.tagLibraries ) {
                if ( tagLibrary.containsTagHandler(ns, localName) ) {
                    log.info("Creating tag handler for: " + ns + " : " + localName);
                    return tagLibrary.createTagHandler(ns, localName, tag);
                }
            }
        }
        log.warn("Could not create tag handler for namespace: " + ns);
        return null;
    }

    @Override
    public boolean containsFunction(String ns, String name) {
        if ( this.namespace.equals(ns) ) {
            for ( TagLibrary tagLibrary : this.tagLibraries ) {
                if ( tagLibrary.containsFunction(ns, name) ) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Method createFunction(String ns, String name) {
        if ( this.namespace.equals(ns) ) {
            for ( TagLibrary tagLibrary : this.tagLibraries ) {
                if ( tagLibrary.containsFunction(ns, name) ) {
                    return tagLibrary.createFunction(ns, name);
                }
            }
        }
        log.warn("Could not create function for namespace: " + ns);
        return null;
    }
}
