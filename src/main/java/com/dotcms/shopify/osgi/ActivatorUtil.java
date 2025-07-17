package com.dotcms.shopify.osgi;

import com.dotcms.shopify.util.DotShopifyApp;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import common.Assert;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;


public class ActivatorUtil {

  void moveJarFilestoFileAssets(String packagePathInJar, String destinationFolderPathIn) throws Exception {
    this.moveJarFilestoFileAssets(packagePathInJar, destinationFolderPathIn, null);
  }


  void moveJarFilestoFileAssets(String packagePathInJar, String destinationFolderPathIn, Host siteIn) throws Exception{
    final Host site = UtilMethods.isSet(siteIn) ? siteIn : APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(),false);
    Folder graphFolder = APILocator.getFolderAPI().createFolders(destinationFolderPathIn, site, APILocator.systemUser(), false);




    String destinationFolderPath = destinationFolderPathIn.endsWith("/") ? destinationFolderPathIn : destinationFolderPathIn+"/";
    Host defaultSite = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(),false);
    List<String> fileList = listFilesInPackage(packagePathInJar);
    for(String pathName : fileList){
      String fileName = pathName.substring(pathName.lastIndexOf("/")+1);

      Identifier id = APILocator.getIdentifierAPI().find(defaultSite,destinationFolderPath+fileName);
      if(UtilMethods.isSet(()->id.getId())){
        Logger.warn(this.getClass(), "File already exists: " + destinationFolderPath+fileName);
        continue;
      }

      // Create tmp file
      File tmpDir = Files.createTempFile("shopify", "").toFile();
      tmpDir.delete();
      tmpDir.mkdirs();
      File tmpFile = new File(tmpDir, fileName);


      // write content to tmp file
      try (final InputStream in = this.getClass().getResourceAsStream("/" + pathName )) {
        IOUtils.copy(in, Files.newOutputStream(tmpFile.toPath()));
      }

      //
      Folder folder = APILocator.getFolderAPI().findFolderByPath(destinationFolderPath, defaultSite, APILocator.systemUser(), false);

      FileAsset fileAsset = new FileAsset();
      fileAsset.setFolder(folder.getIdentifier());
      fileAsset.setHost(defaultSite.getIdentifier());
      fileAsset.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, fileName);
      fileAsset.setBinary(FileAssetAPI.BINARY_FIELD, tmpFile);
      fileAsset.setTitle(fileName);


      fileAsset.setContentTypeId(APILocator.getContentTypeAPI(APILocator.systemUser()).find("fileAsset").id());
      fileAsset.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());

      Contentlet dotfile = APILocator.getContentletAPI().checkin(fileAsset, APILocator.systemUser(), false);
      APILocator.getContentletAPI().publish(dotfile, APILocator.systemUser(), false);
      Assert.verify(dotfile != null && dotfile.getIdentifier()!=null, "Unable to create file asset: " + fileName);
      tmpFile.delete();
      tmpDir.delete();

    }

  }


  List<String> listFilesInPackage(String packagePath) {
    List<String> fileList = new ArrayList<>();


    try {
      String jarPath = this.getClass().getProtectionDomain()
          .getCodeSource()
          .getLocation()
          .getPath();

      try (java.util.jar.JarFile jarFile = new java.util.jar.JarFile(jarPath)) {
        java.util.Enumeration<java.util.jar.JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
          java.util.jar.JarEntry entry = entries.nextElement();
          String entryName = entry.getName();

          if (entryName.startsWith(packagePath) && !entry.isDirectory()) {
            fileList.add(entryName);
          }
        }
      }
    } catch (java.io.IOException e) {
      throw new RuntimeException("Error reading JAR file", e);
    }
    return fileList;
  }


  private final File installedAppYaml = new File(ConfigUtils.getAssetPath() + File.separator + "server"
      + File.separator + "apps" + File.separator + DotShopifyApp.DOT_SHOPIFY_APP_KEY.appValue + ".yml");

  /**
   * copies the App yaml to the apps directory and refreshes the apps
   *
   * @throws IOException
   */
  public void copyAppYml() throws IOException {


    Logger.info(this.getClass().getName(), "copying YAML File:" + installedAppYaml);
    try (final InputStream in = this.getClass().getResourceAsStream("/" + DotShopifyApp.DOT_SHOPIFY_APP_KEY.appValue + ".yml")) {
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
