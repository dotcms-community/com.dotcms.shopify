package com.dotcms.shopify.workflow;


import com.dotcms.shopify.api.ShopifyAPI;
import com.dotcms.shopify.api.ShopifyAPIImpl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.model.MultiKeyValue;
import com.dotmarketing.portlets.workflows.model.MultiSelectionWorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ShopifyActionlet extends WorkFlowActionlet {

    private static final long serialVersionUID = 1L;
    private static final String CONTENT_TYPE_LIST = "contentTypeList";
    private static final String PUBLISH_UNPUBLISH = "publishUnpublish";






    @Override
    public List<WorkflowActionletParameter> getParameters() {
        WorkflowActionletParameter publishUnpublish = new MultiSelectionWorkflowActionletParameter(
            PUBLISH_UNPUBLISH,
                "Sync on Publish or Unpublish or Both", "BOTH", true,
                () -> List.of(
                        new MultiKeyValue("PUBLISH", "PUBLISH"),
                        new MultiKeyValue("UNPUBLISH", "UNPUBLISH"),
                        new MultiKeyValue("AUTO", "AUTO")
                        )
        );


        final List<WorkflowActionletParameter> params = new ArrayList<>();

        params.add(new WorkflowActionletParameter(CONTENT_TYPE_LIST, "Comma Separated list of Content Types to sync with Shopify.  Leave blank to sync all content types.", "", false));
        params.add(publishUnpublish);

        return params;
    }

    @Override
    public String getName() {
        return "Sync to Shopify";
    }

    @Override
    public String getHowTo() {
        return "This actionlet will sync contentlets to the Shopify database.  It will sync all contentlets of the content types specified in the Content Type List parameter.  It will also sync the contentlets on Publish or Unpublish or AUTO, which means if a content is published, it will be pushed to Shopify and if it is unpublished, and there are no live versions, it will be removed from Shopify.";
    }

    @Override
    public void executeAction(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params)
                    throws WorkflowActionFailureException {

        final Contentlet contentlet = processor.getContentlet();

        //final boolean isPurgeContentlet = Try.of(()-> Boolean.parseBoolean(params.get(PURGE_CONTENTLET_PARAM).getValue().trim())).getOrElse(true);
        final Host host = Try.of(() -> APILocator.getHostAPI()
                .find(contentlet.getHost(), APILocator.systemUser(), false))
                .getOrNull();
        if (host == null) {
            Logger.warn(this.getClass().getName(),"Contentlet Host is Null");
            return;
        }


        String contentTypes = params.get(CONTENT_TYPE_LIST).getValue();

        if(UtilMethods.isSet(contentTypes) && !Arrays.stream(contentTypes.split(",")).anyMatch(s -> s.trim().equalsIgnoreCase(contentlet.getContentType().variable()))){
                return;

        }


        final boolean publish = Try.of(()->params.get(PUBLISH_UNPUBLISH).getValue().equalsIgnoreCase("PUBLISH") ).getOrElse(false);

        final boolean unpublish = Try.of(()->params.get(PUBLISH_UNPUBLISH).getValue().equalsIgnoreCase("UNPUBLISH") ).getOrElse(false);


        ShopifyAPI api = new ShopifyAPIImpl(host);


        if(publish){
            api.pushContentlet(contentlet);
            return;
        }
        if (unpublish) {
            api.removeContentlet(contentlet);
            return;
        }
        api.syncContentlet(contentlet);




        



    }


}
