package edge.discovery.init;

import at.uibk.dps.ee.core.Initializer;
import at.uibk.dps.ee.guice.starter.VertxProvider;
import com.google.inject.Inject;
import edge.discovery.device.DeviceManager;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Initializes local resources after discovery.
 *
 * @author Lukas Dötlinger
 */
public class InitializerLocalResources implements Initializer {

  protected final Vertx vertX;
  protected final DeviceManager deviceManager;

  protected final Logger logger = LoggerFactory.getLogger(InitializerLocalResources.class);

  /**
   * Injection constructor
   *
   * @param vProv vertX provider (to get the timer)
   * @param deviceManager device manager providing local resources
   */
  @Inject
  public InitializerLocalResources(final VertxProvider vProv, final DeviceManager deviceManager) {
    this.deviceManager = deviceManager;
    this.vertX = vProv.getVertx();
  }

  @Override
  public Future<String> initialize() {
    final Promise<String> resultPromise = Promise.promise();

    var futures = deviceManager.getDiscoveredDevices().stream()
      .map(d -> {
        final Promise<Boolean> devicePromise = Promise.promise();
        deviceManager.addDevice(devicePromise, d);
        return devicePromise.future();
      }).collect(Collectors.toList());

    var compositeFutureFuture = CompositeFuture.all(new ArrayList<>(futures))
      .onSuccess(r -> resultPromise.complete())
      .onFailure(r -> resultPromise.fail(r.getMessage()));

    return resultPromise.future();
  }
}
