package com.dotcms.shopify.api;

import com.dotmarketing.portlets.contentlet.model.Contentlet;

/**
 * Model class to represent content for Shopify integration
 */
public class ContentModel {
    
    private final Contentlet contentlet;
    private final String id;
    
    public ContentModel(Contentlet contentlet) {
        this.contentlet = contentlet;
        this.id = contentlet.getIdentifier();
    }
    
    public String getId() {
        return id;
    }
    
    public Contentlet getContentlet() {
        return contentlet;
    }
}
