package org.tdr.refsite.tag;

import com.sun.facelets.tag.AbstractTagLibrary;

/**
 * Test Facelet Tag library
 * User: nic
 */
public class TagLibrary extends AbstractTagLibrary {

    public final static String Namespace = "http://tdr.org/tag";


    public TagLibrary() {
        super(Namespace);

        this.addTagHandler("testTag", TestTagHandler.class);

        try {
            this.addFunction("doStuff", this.getClass().getMethod("doStuff"));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static void doStuff() {
        System.out.println("Doing stuff!");
    }


}
