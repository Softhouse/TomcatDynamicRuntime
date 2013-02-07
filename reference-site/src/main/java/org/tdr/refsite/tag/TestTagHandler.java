package org.tdr.refsite.tag;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.FaceletException;
import com.sun.facelets.tag.TagAttribute;
import com.sun.facelets.tag.TagConfig;
import com.sun.facelets.tag.TagHandler;

import java.io.IOException;
import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;

/**
 * Tag handler for the tag 'refsite:testTag
 * User: nic
 */
public class TestTagHandler extends TagHandler {

    private final TagAttribute name;
    private final TagAttribute value;

    public TestTagHandler(TagConfig config) {
        super(config);

        this.name = super.getRequiredAttribute("name");
        this.value = super.getRequiredAttribute("value");
    }

    /* (non-Javadoc)
   * @see com.sun.facelets.FaceletHandler#apply(com.sun.facelets.FaceletContext, javax.faces.component.UIComponent)
   */
    public void apply(FaceletContext ctx, UIComponent parent) throws IOException, FacesException, FaceletException,
            ELException
    {
        ctx.setAttribute(this.name.getValue(ctx), this.value.getValue(ctx));
    }

}