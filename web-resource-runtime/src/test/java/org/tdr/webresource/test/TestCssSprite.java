package org.tdr.webresource.test;

import org.carrot2.labs.smartsprites.SmartSpritesParameters;
import org.carrot2.labs.smartsprites.SpriteBuilder;
import org.carrot2.labs.smartsprites.message.MemoryMessageSink;
import org.carrot2.labs.smartsprites.message.Message;
import org.carrot2.labs.smartsprites.message.MessageLog;

import java.util.ArrayList;
import java.util.List;

/**
 * @author nic
 */
public class TestCssSprite {


    public static void main(String[] args) throws Throwable {


        List<String> cssFiles = new ArrayList<String>();  //  input.getWebModules()
        //cssFiles.add("web-resource-runtime/src/test/resources/common/icon.css");
        //String outputDir = "web-resource-runtime/src/test/resources/common/out";   // temp dir??
        cssFiles.add("web-resource-runtime/src/test/resources/test/style.css");
        String outputDir = "web-resource-runtime/src/test/resources/test/out";   // temp dir??

        String rootDir = "web-resource-runtime/src/test/resources/test";  // Is base for images??
        MemoryMessageSink messageSink = new MemoryMessageSink();
        MessageLog messageLog =  new MessageLog(messageSink);
        SpriteBuilder spriteBuilder = new SpriteBuilder(
                //new SmartSpritesParameters(rootDir),

                new SmartSpritesParameters(rootDir, cssFiles, outputDir, null, Message.MessageLevel.INFO,
                        SmartSpritesParameters.DEFAULT_CSS_FILE_SUFFIX,
                        SmartSpritesParameters.DEFAULT_SPRITE_PNG_DEPTH,
                        SmartSpritesParameters.DEFAULT_SPRITE_PNG_IE6,
                        SmartSpritesParameters.DEFAULT_CSS_FILE_ENCODING),

                messageLog);




         spriteBuilder.buildSprites();

        System.out.println("Sprites generated.\nMessages:");
        for ( Message message : messageSink.messages ) {
            System.out.println(message.getFormattedMessage());
        }
    }
}
