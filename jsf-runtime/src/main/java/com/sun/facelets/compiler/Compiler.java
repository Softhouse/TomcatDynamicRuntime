/**
 * Licensed under the Common Development and Distribution License,
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.sun.com/cddl/
 *   
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or 
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.sun.facelets.compiler;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.faces.FacesException;
import javax.faces.context.FacesContext;

import com.sun.facelets.FaceletException;
import com.sun.facelets.FaceletHandler;
import com.sun.facelets.tag.*;
import com.sun.facelets.tag.ui.UILibrary;
import com.sun.facelets.util.ParameterCheck;
import com.sun.facelets.util.FacesAPI;
import com.sun.facelets.util.ReflectionUtil;
import org.tdr.jsf.runtime.PluggableTagLibrary;

/**
 * A Compiler instance may handle compiling multiple sources
 * 
 * @author Jacob Hookom
 * @version $Id: Compiler.java,v 1.15 2006/10/19 03:48:12 jhook Exp $
 */
public abstract class Compiler {

    protected final static Logger log = Logger.getLogger("facelets.compiler");

    public final static String EXPRESSION_FACTORY = "compiler.ExpressionFactory";

    private static final TagLibrary EMPTY_LIBRARY = new CompositeTagLibrary(
            new TagLibrary[0]);

    private static final TagDecorator EMPTY_DECORATOR = new CompositeTagDecorator(
            new TagDecorator[0]);

    private boolean validating = false;

    private boolean trimmingWhitespace = false;

    private boolean trimmingComments = false;

    private final List libraries = new ArrayList();

    private final List decorators = new ArrayList();

    private final Map features = new HashMap();

    private boolean initialized = false;

//    private final Map<String, PluggableTagLibrary> pluggableTagLibraries = new HashMap<String, PluggableTagLibrary>();

    /**
     * 
     */
    public Compiler() {
        
    }

    private synchronized void initialize() {
        if (this.initialized)
            return;
        log.fine("Initializing");
        try {
            TagLibraryConfig cfg = new TagLibraryConfig();
            cfg.loadImplicit(this);
            
            if (!this.createTagLibrary().containsNamespace(UILibrary.Namespace)) {
                log.severe("Missing Built-in Tag Libraries! Make sure they are included within the META-INF directory of Facelets' Jar");
            }
            
        } catch (IOException e) {
            log.log(Level.SEVERE, "Compiler Initialization Error", e);
        } finally {
            this.initialized = true;
        }
        log.fine("Initialization Successful");
    }

    public final FaceletHandler compile(URL src, String alias)
            throws IOException, FaceletException, ELException, FacesException {
        if (!this.initialized)
            this.initialize();
        return this.doCompile(src, alias);
    }

    protected abstract FaceletHandler doCompile(URL src, String alias)
            throws IOException, FaceletException, ELException, FacesException;

    public final TagDecorator createTagDecorator() {
        if (this.decorators.size() > 0) {
            return new CompositeTagDecorator((TagDecorator[]) this.decorators
                    .toArray(new TagDecorator[this.decorators.size()]));
        }
        return EMPTY_DECORATOR;
    }

    public final void addTagDecorator(TagDecorator decorator) {
        ParameterCheck.notNull("decorator", decorator);
        if (!this.decorators.contains(decorator)) {
            this.decorators.add(decorator);
        }
    }

    public final ExpressionFactory createExpressionFactory() {
        ExpressionFactory el = null;
        el = (ExpressionFactory) this.featureInstance(EXPRESSION_FACTORY);
        if (el == null && FacesAPI.getVersion() >= 12) {
            try {
                el = FacesContext.getCurrentInstance().getApplication()
                        .getExpressionFactory();
                if (el == null) {
                    log.warning("No default ExpressionFactory from Faces Implementation, attempting to load from Feature["
                                + EXPRESSION_FACTORY + "]");
                }
            } catch (Exception e) {
                // do nothing
            }
        }
        if (el == null) {
            this.features.put(EXPRESSION_FACTORY, "com.sun.el.ExpressionFactoryImpl");
            el = (ExpressionFactory) this.featureInstance(EXPRESSION_FACTORY);
        }
        return el;
    }

    private final Object featureInstance(String name) {
        String type = (String) this.features.get(name);
        if (type != null) {
            try {
                return ReflectionUtil.forName(type).newInstance();
            } catch (Throwable t) {
                throw new FaceletException("Could not instantiate feature["
                        + name + "]: " + type);
            }
        }
        return null;
    }

    public final TagLibrary createTagLibrary() {
        if (this.libraries.size() > 0) {
            return new CompositeTagLibrary((TagLibrary[]) this.libraries
                    .toArray(new TagLibrary[this.libraries.size()]));
        }
        return EMPTY_LIBRARY;
    }

    public final void addTagLibrary(TagLibrary library) {
        ParameterCheck.notNull("library", library);

//        if ( library instanceof AbstractTagLibrary ) {
//            String namespace =  ((AbstractTagLibrary)library).getNamespace();
//            PluggableTagLibrary pluggableTagLibrary = this.pluggableTagLibraries.get(namespace);
//            if ( pluggableTagLibrary == null ) {
//                pluggableTagLibrary = new PluggableTagLibrary(namespace);
//                this.pluggableTagLibraries.put(namespace, pluggableTagLibrary);
//                this.libraries.add(pluggableTagLibrary);
//            }
//            if ( ! pluggableTagLibrary.containsConcreteTagLibrary(library) ) {
//                log.info("Adding pluggable tag library: " + library);
//                pluggableTagLibrary.addConcreteTagLibrary(library);
//            }
//        }
//        else {
            if (!this.libraries.contains(library)) {
                log.info("Adding tag library: " + library.getClass() + ", hash: " + library.getClass().hashCode());
                this.libraries.add(library);
            }
//        }
    }
    
    /**
     * Extension to open up the possibility to remove tag libraries on-the-fly
     * @param library
     */
    public final void removeTagLibrary(TagLibrary library) {
    	log.info("Removing tag library: " + library.getClass() + ", hash: " + library.getClass().hashCode());

//        if ( library instanceof AbstractTagLibrary ) {
//            String namespace =  ((AbstractTagLibrary)library).getNamespace();
//            PluggableTagLibrary pluggableTagLibrary = this.pluggableTagLibraries.get(namespace);
//            if ( pluggableTagLibrary != null && pluggableTagLibrary.containsConcreteTagLibrary(library) ) {
//                log.info("Removing pluggable tag library: " + library);
//                pluggableTagLibrary.removeConcreteTagLibrary(library);
//                if ( pluggableTagLibrary.isEmpty() ) {
//                    this.libraries.remove(pluggableTagLibrary);
//                }
//            }
//        }
//        else {
            this.libraries.remove(library);
//        }
    	log.info("Current tag libraries: " + this.libraries);
    	
    }

    public final void setFeature(String name, String value) {
        this.features.put(name, value);
    }

    public final String getFeature(String name) {
        return (String) this.features.get(name);
    }

    public final boolean isTrimmingComments() {
        return this.trimmingComments;
    }

    public final void setTrimmingComments(boolean trimmingComments) {
        this.trimmingComments = trimmingComments;
    }

    public final boolean isTrimmingWhitespace() {
        return this.trimmingWhitespace;
    }

    public final void setTrimmingWhitespace(boolean trimmingWhitespace) {
        this.trimmingWhitespace = trimmingWhitespace;
    }

    public final boolean isValidating() {
        return this.validating;
    }

    public final void setValidating(boolean validating) {
        this.validating = validating;
    }
}
