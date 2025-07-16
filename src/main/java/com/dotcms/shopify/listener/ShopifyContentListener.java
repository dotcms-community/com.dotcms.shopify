package com.dotcms.shopify.listener;



import com.dotcms.concurrent.Debouncer;
import com.dotcms.content.elasticsearch.business.event.ContentletArchiveEvent;
import com.dotcms.content.elasticsearch.business.event.ContentletDeletedEvent;
import com.dotcms.content.elasticsearch.business.event.ContentletPublishEvent;
import com.dotcms.shopify.api.ContentModel;
import com.dotcms.shopify.api.ShopifyAPI;
import com.dotcms.system.event.local.model.Subscriber;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletListener;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;
import java.util.concurrent.TimeUnit;


/**
 * This subscriber is in charge of invalidated
 * @author jsanca
 */
public class ShopifyContentListener implements ContentletListener<Contentlet> {

    private static Debouncer debouncer = new Debouncer();

    @Override
    public String getId() {
        return getClass().getCanonicalName();
    }

    @Override
    public void onModified(final ContentletPublishEvent<Contentlet> contentletPublishEvent) {
        final Contentlet contentlet = contentletPublishEvent.getContentlet();
        if (contentletPublishEvent.isPublish()) {
            Logger.info(this.getClass(), "onModified - PublishEvent:true " + contentlet.getTitle());
        } else {
            Logger.info(this.getClass(), "onModified - PublishEvent:false " + contentlet.getTitle());
        }
    }

    @Subscriber
    public void onPublish(final ContentletPublishEvent<Contentlet> contentletPublishEvent) {


        final Contentlet contentlet = contentletPublishEvent.getContentlet();

        final Host host = Try.of(() -> APILocator.getHostAPI()
                        .find(contentlet.getHost(), APILocator.systemUser(), false))
                .getOrNull();



        if (host == null) {
            Logger.warn(this.getClass().getName(),"Contentlet Host is Null");
            return;
        }
        final ShopifyAPI api = ShopifyAPI.api(host);

        if(!api.syncContentTypeListener(contentlet.getContentType().variable())){
            return;
        }


        Logger.info(this.getClass(), "PublishEvent:"+contentletPublishEvent.isPublish()+", title:" + contentlet.getTitle());

        ContentModel cm = new ContentModel(contentlet);

        debouncer.debounce(
                    cm.getId(), ()->
                        api.syncContentlet(contentlet)
                   , 3, TimeUnit.SECONDS);


    }

    @Subscriber
    @Override
    public void onArchive(final ContentletArchiveEvent<Contentlet> contentletArchiveEvent) {
        final Contentlet contentlet = contentletArchiveEvent.getContentlet();
        Logger.info(this.getClass(), "onArchive -  " + contentlet.getTitle());
    }

    @Subscriber
    @Override
    public void onDeleted(final ContentletDeletedEvent<Contentlet> contentletDeletedEvent) {
        final Contentlet contentlet = contentletDeletedEvent.getContentlet();
        Logger.info(this.getClass(), "onDeleted -  " + contentlet.getTitle());
    }

}
