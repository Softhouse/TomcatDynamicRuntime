package org.tdr.webresource.processor.css;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.carrot2.labs.smartsprites.SmartSpritesParameters;
import org.carrot2.labs.smartsprites.SpriteBuilder;
import org.carrot2.labs.smartsprites.message.MemoryMessageSink;
import org.carrot2.labs.smartsprites.message.Message;
import org.carrot2.labs.smartsprites.message.MessageLog;
import org.tdr.webresource.model.ResourceType;
import org.tdr.webresource.model.WebResource;
import org.tdr.webresource.processor.ProcessingException;
import org.tdr.webresource.processor.WebResourceProcessor;
import org.tdr.webresource.util.FileUtilities;

import java.util.ArrayList;
import java.util.List;

/**
 * @author nic
 */
public class SpriteBuilderProcessor implements WebResourceProcessor {

    private static final Log log = LogFactory.getLog(SpriteBuilderProcessor.class);

            // TODO: In which stage should sprite generation take place??
            // -> Should be one of the first steps in the process...
            // Can smartsprites read LESS??? Is probably a risk...
            // Will LESS remove the smartsprite comments??
            //

            // Smartsprites verkar inte fixa background css tag

            // Göra en processor som skriver om background till upppackade varianter??

            // Hur hantera delade bilder?

            // Ha en en CSS sprite fil som matchar som web resursen, dvs
            //   main.css -> main-sprites.png
            // Fixa följande processors innan smartsprites kickar in (efter less compile & merge):
            //  - Packa upp background till background-image osv
            //  - Lägg in smartsprite direktiv i CSS filen som automatiskt bygger upp en matris av ett antal
            //    bilder på en rad som bryter efter visst antal pixels
            //    + fixa sprite-alignment: repeat & sprite-margin-top: 5px ifall background-repeat: repeat-x resp background-position: 5px top används
            //
            // - Ska man ha en image locator som tar med alla refererande bilder och flyttar dessa till defineras bild katalog vid target resursen?

            // Behöver man ha en sprite per modul? För att finetuna beteendet

            // DO WE NEED TO SOMEHOW NEED THAT A LESS FILE HAS BECOME A CSS FILE???


    // TODO: Have some mechanism to control if source or target should be processed
    // -> Maybe some kind of merge status on the web resource??



    @Override
    public void process(WebResource webResource, String workAreaPath) throws ProcessingException {

        if ( webResource.getType() != ResourceType.CSS ) {
            return;
        }

        List<String> cssFiles = new ArrayList<>();
        String cssFilename = workAreaPath + "/" + webResource.getName();
        cssFiles.add(cssFilename);
        MemoryMessageSink messageSink = new MemoryMessageSink();
        MessageLog messageLog =  new MessageLog(messageSink);
        SpriteBuilder spriteBuilder = new SpriteBuilder(
                new SmartSpritesParameters(workAreaPath, cssFiles, workAreaPath, null, Message.MessageLevel.INFO,
                        SmartSpritesParameters.DEFAULT_CSS_FILE_SUFFIX,
                        SmartSpritesParameters.DEFAULT_SPRITE_PNG_DEPTH,
                        SmartSpritesParameters.DEFAULT_SPRITE_PNG_IE6,
                        SmartSpritesParameters.DEFAULT_CSS_FILE_ENCODING), messageLog);

        try {
            spriteBuilder.buildSprites();
            for ( Message message : messageSink.messages ) {
                log.info(message.getFormattedMessage());
            }

            FileUtilities.moveFile(cssFilename.replace(".css", SmartSpritesParameters.DEFAULT_CSS_FILE_SUFFIX + ".css"),
                                   cssFilename);
            //Files.move(Paths.get(cssFilename.replace(".css", SmartSpritesParameters.DEFAULT_CSS_FILE_SUFFIX + ".css")),
            //           Paths.get(cssFilename),
            //           StandardCopyOption.REPLACE_EXISTING);
        }
        catch ( Exception e ) {
            throw new ProcessingException("Could not build CSS sprites.", e);
        }

        /* SOURCE VERSION
        for ( WebModule module : webResource.getModulesByTypes(ResourceType.CSS,  ResourceType.LESS) ) {
            List<String> cssFiles = new ArrayList<>();
            cssFiles.add(module.getSourcePath() + "/" + module.getName());
            MemoryMessageSink messageSink = new MemoryMessageSink();
            MessageLog messageLog =  new MessageLog(messageSink);
            SpriteBuilder spriteBuilder = new SpriteBuilder(
                    new SmartSpritesParameters(module.getSourcePath(), cssFiles, workAreaPath, null, Message.MessageLevel.INFO,
                            "",
                            SmartSpritesParameters.DEFAULT_SPRITE_PNG_DEPTH,
                            SmartSpritesParameters.DEFAULT_SPRITE_PNG_IE6,
                            SmartSpritesParameters.DEFAULT_CSS_FILE_ENCODING), messageLog);

            try {
                spriteBuilder.buildSprites();
                for ( Message message : messageSink.messages ) {
                    log.info(message.getFormattedMessage());
                }
            }
            catch ( Exception e ) {
                throw new ProcessingException("Could not build CSS sprites.", e);
            }

        } */
    }
}
