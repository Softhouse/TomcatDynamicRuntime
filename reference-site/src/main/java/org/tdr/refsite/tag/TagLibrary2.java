package org.tdr.refsite.tag;

import com.sun.facelets.tag.AbstractTagLibrary;

/**
 * Test Facelet Tag library
 * User: nic
 */
public class TagLibrary2 extends AbstractTagLibrary {

    public final static String Namespace = "http://tdr.org/tag";

    public TagLibrary2() {
        super(Namespace);

        this.addTagHandler("testTag2", TestTagHandler.class);

        try {
            this.addFunction("doStuff2", this.getClass().getMethod("doStuff2"));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static void doStuff2() {
        System.out.println("Doing stuff2!");
    }


}
