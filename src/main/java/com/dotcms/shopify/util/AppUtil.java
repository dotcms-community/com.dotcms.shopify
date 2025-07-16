package com.dotcms.shopify.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import org.apache.commons.io.IOUtils;


import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;

public class AppUtil {
    public static final String SHOPIFY_APP_KEY = "shopify";
    private final File installedAppYaml = new File(ConfigUtils.getAssetPath() + File.separator + "server"
                    + File.separator + "apps" + File.separator + SHOPIFY_APP_KEY + ".yml");

    /**
     * copies the App yaml to the apps directory and refreshes the apps
     * 
     * @throws IOException
     */
    public void copyAppYml() throws IOException {


        Logger.info(this.getClass().getName(), "copying YAML File:" + installedAppYaml);
        try (final InputStream in = this.getClass().getResourceAsStream("/" + SHOPIFY_APP_KEY + ".yml")) {
            IOUtils.copy(in, Files.newOutputStream(installedAppYaml.toPath()));
        }
        CacheLocator.getAppsCache().clearCache();


    }

    /**
     * Deletes the App yaml to the apps directory and refreshes the apps
     * 
     * @throws IOException
     */
    public void deleteYml() throws IOException {


        Logger.info(this.getClass().getName(), "deleting the YAML File:" + installedAppYaml);

        installedAppYaml.delete();
        CacheLocator.getAppsCache().clearCache();


    }



}
