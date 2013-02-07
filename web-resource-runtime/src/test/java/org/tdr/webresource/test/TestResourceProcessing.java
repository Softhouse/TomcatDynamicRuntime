package org.tdr.webresource.test;

import org.tdr.webresource.model.ResourceType;
import org.tdr.webresource.model.WebModule;
import org.tdr.webresource.model.WebResource;
import org.tdr.webresource.processor.*;
import org.tdr.webresource.processor.css.AggregateLessModulesProcessor;
import org.tdr.webresource.processor.css.LessCompilationProcessor;
import org.tdr.webresource.processor.css.SpriteBuilderProcessor;

/**
 * @author nic
 */
public class TestResourceProcessing {


    // TODO: Redesign to JUNIT test case here!!!

    // TODO: All modules should be removed from work area before the actual copy is taken place

    // Or this done by the less compilator??? -> YEPP!!!

    // TODO: Can the spawn of additional java process be switched off??

    public static void main(String[] args) {

        WebResource webResource = new WebResource(ResourceType.CSS,  "common/telenor.css");
        WebModule module = new WebModule(ResourceType.LESS, "common/icon.less", "web-resource-runtime/src/test/resources",1);
        module.addResourcePath("common/img");
        webResource.addModule(module);

        WebResourceProcessManager processManager =
                new WebResourceProcessManager(
                        "web-resource-runtime/test/workarea",
                        "web-resource-runtime/test/processing-out",
                        new WebResourceProcessor[] {
                                new AggregateLessModulesProcessor(),
                                new LessCompilationProcessor(),
                                new SpriteBuilderProcessor()
                        });
        processManager.process(webResource);
    }
}

