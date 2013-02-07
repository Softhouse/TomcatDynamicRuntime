package org.tdr.webresource.processor;

import org.tdr.webresource.model.WebResource;

import java.io.Reader;
import java.io.Writer;

/**
 * Interface for web resource processors.
 *
 * @author nic
 */
public interface WebResourceProcessor {


    // make any difference between pre & post processing??
    //

    /**
     *
     *
     *
     * WebResourceProcessingManager
     *  -> make any difference between pre and post processing?
     *  -> merge? Yet another processing step?
     *
     *  -> source to target al'a MG? With a source2target processor?
     *
     *  What in the input?
     *
     *  Deployment descriptor:
     *  <webresource name="css/target.css">
     *          <module type="less" name="source.less" prio="2"/>
     *          <module type="less" name="source2.less" prio="2">
     *              <resources>
     *                  <resource path="img/"/>   <- ANT style pattern??
     *              </resources>
     *          </module>
     *     </webresource>
     *
     *   Is inserted into registry for 'css/target.css'
     *
     *     process(target, sources)?
     *
     *
     *
     *     return ProcessNext(target, sources);
     *
     *     WebResource
     *     WebModule
     *         WebModuleResource with ANT patterns etc
     *
     *
     *
     *     Source
     *      - ref to file
     *      - intermediate format?
     *
     *     Source & target is immutable structures where the processors create
     *     new instances if they touch them.
     *
     *
     *   How to handle temp folder? Also something we need to sort out in the TDR deployment framework???
     *
     *
     */

    /**
     * Process web resource using specified work area.
     *
     * @param webResource
     * @param workAreaPath
     * @throws ProcessingException
     */
    public void process(WebResource webResource, String workAreaPath) throws ProcessingException;
}
