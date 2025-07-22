package com.dotcms.shopify.osgi;

import com.dotcms.shopify.util.ShopifyApp.AppKey;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import common.Assert;
import io.vavr.control.Try;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.apache.commons.io.IOUtils;


public class ActivatorUtil {

  private final File installedAppYaml = new File(
      ConfigUtils.getAssetPath() + File.separator + "server" + File.separator + "apps" + File.separator
          + AppKey.DOT_SHOPIFY_APP_KEY.appValue + ".yml");

  /**
   * Moves files from the plugin jar to the dotCMS virtual file system as fileAssets. If you do not specify a host, they
   * will be placed on the default host
   *
   * @param packagePathInJar - the directory path in the jar to copy
   * @param destinationFolderPathIn - the destination directory in dotCMS to copy to

   */
  void moveJarFilestoFileAssets(@Nonnull String packagePathInJar, @Nonnull String destinationFolderPathIn) {
    this.moveJarFilestoFileAssets(packagePathInJar, destinationFolderPathIn,
        Try.of(() -> APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false)).getOrElseThrow(
            DotRuntimeException::new));
  }

  /**
   * Moves files from the plugin jar to the dotCMS virtual file system as fileAssets. If you do not specify a host, they
   * will be placed on the default host
   *
   * @param packagePathInJar - the directory path in the jar to copy
   * @param destinationFolderPathIn - the destination directory in dotCMS to copy to
   * @param site - the site to copy to
   */
  void moveJarFilestoFileAssets(@Nonnull String packagePathInJar, @Nonnull String destinationFolderPathIn,
      @Nonnull Host site) {

    try {
      Folder destFolder = Try.of(() -> APILocator.getFolderAPI()
          .createFolders(destinationFolderPathIn, site, APILocator.systemUser(), false)).getOrElseThrow(
          DotRuntimeException::new);

      final String destinationFolderPath =
          destinationFolderPathIn.endsWith("/") ? destinationFolderPathIn : destinationFolderPathIn + "/";

      List<String> fileList = listFilesInPackage(packagePathInJar);
      for (String pathName : fileList) {
        String fileName = pathName.substring(pathName.lastIndexOf("/") + 1);

        Identifier id = APILocator.getIdentifierAPI().find(site, destinationFolderPath + fileName);
        if (UtilMethods.isSet(() -> id.getId())) {
          Logger.warn(this.getClass(), "File already exists: " + destinationFolderPath + fileName);
          continue;
        }

        // Create tmp file
        File tmpDir = Files.createTempFile("shopify", "").toFile();
        tmpDir.delete();
        tmpDir.mkdirs();
        File tmpFile = new File(tmpDir, fileName);

        // write content to tmp file
        try (final InputStream in = this.getClass().getResourceAsStream("/" + pathName)) {
          IOUtils.copy(in, Files.newOutputStream(tmpFile.toPath()));
        } catch (IOException e) {
          Logger.error(this.getClass(), "Error moving file: " + pathName, e);
          continue;
        }

        //

        FileAsset fileAsset = new FileAsset();
        fileAsset.setFolder(destFolder.getIdentifier());
        fileAsset.setHost(site.getIdentifier());
        fileAsset.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, fileName);
        fileAsset.setBinary(FileAssetAPI.BINARY_FIELD, tmpFile);
        fileAsset.setTitle(fileName);

        fileAsset.setContentTypeId(APILocator.getContentTypeAPI(APILocator.systemUser())
            .find(FileAssetAPI.DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME).id());
        fileAsset.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());

        Contentlet dotfile = APILocator.getContentletAPI().checkin(fileAsset, APILocator.systemUser(), false);
        APILocator.getContentletAPI().publish(dotfile, APILocator.systemUser(), false);
        Assert.verify(dotfile != null && dotfile.getIdentifier() != null, "Unable to create file asset: " + fileName);
        tmpFile.delete();
        tmpDir.delete();

      }
      ;
    } catch (Exception e) {
      throw new DotRuntimeException("Error moving files from jar to file assets:" + e.getMessage(), e);
    }

  }

  public static List<String> listFilesInPackage(@Nonnull String packagePath) {
    List<String> fileList = new ArrayList<>();

    try {
      String jarPath = ActivatorUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath();

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

  /**
   * copies the App yaml to the apps directory and refreshes the apps
   *
   * @throws IOException
   */
  public void copyAppYml() throws IOException {

    Logger.info(this.getClass().getName(), "copying YAML File:" + installedAppYaml);
    try (final InputStream in = this.getClass()
        .getResourceAsStream("/" + AppKey.DOT_SHOPIFY_APP_KEY.appValue + ".yml")) {
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
