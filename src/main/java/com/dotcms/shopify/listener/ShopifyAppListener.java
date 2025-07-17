package com.dotcms.shopify.listener;

import com.dotcms.ai.validator.AIAppValidator;
import com.dotcms.security.apps.AppSecretSavedEvent;
import com.dotcms.shopify.api.ShopifyAPI;
import com.dotcms.shopify.util.DotShopifyApp;
import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotcms.system.event.local.model.KeyFilterable;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

public final class ShopifyAppListener implements EventSubscriber<AppSecretSavedEvent>,
    KeyFilterable {

  private final HostAPI hostAPI;

  public ShopifyAppListener(final HostAPI hostAPI) {
    this.hostAPI = hostAPI;
  }

  public ShopifyAppListener() {
    this(APILocator.getHostAPI());
  }

  /**
   * Notifies the listener of an {@link AppSecretSavedEvent}.
   *
   * <p>
   * This method is called when an {@link AppSecretSavedEvent} occurs. It performs the following
   * actions:
   * <ul>
   * <li>Logs a debug message if the event is null or the event's host identifier
   * is blank.</li>
   * <li>Finds the host associated with the event's host identifier.</li>
   * <li>Resets the AI models for the found host's hostname.</li>
   * <li>Validates the AI configuration using the {@link AIAppValidator}.</li>
   * </ul>
   * </p>
   *
   * @param event the {@link AppSecretSavedEvent} that triggered the notification
   */
  @Override
  public void notify(final AppSecretSavedEvent event) {
    if (Objects.isNull(event)) {
      Logger.debug(this, "Missing event, aborting");
      return;
    }

    if (StringUtils.isBlank(event.getHostIdentifier())) {
      Logger.debug(this, "Missing event's host id, aborting");
      return;
    }

    final String hostId = event.getHostIdentifier();
    final Host host = Try.of(() -> hostAPI.find(hostId, APILocator.systemUser(), false))
        .getOrNull();

    ShopifyAPI.api(host).reload();
  }

  @Override
  public Comparable<String> getKey() {
    return DotShopifyApp.DOT_SHOPIFY_APP_KEY.name();
  }

}
