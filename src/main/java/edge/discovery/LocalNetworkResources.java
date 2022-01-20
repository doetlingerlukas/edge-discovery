package edge.discovery;

import at.uibk.dps.ee.core.LocalResources;
import at.uibk.dps.ee.guice.starter.VertxProvider;
import at.uibk.dps.ee.model.graph.MappingsConcurrent;
import at.uibk.dps.ee.model.graph.ResourceGraph;
import at.uibk.dps.ee.model.graph.SpecificationProvider;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import edge.discovery.device.Device;
import edge.discovery.device.DeviceManager;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;


/**
 * Class implementing the operations require for the init and the cleanup of
 * usable processing resources within the same local network as the triggering
 * Apollo instance.
 *
 * @author Fedor Smirnov, Lukas Dötlinger
 */
@Singleton
public class LocalNetworkResources implements LocalResources {

  protected final Logger logger = LoggerFactory.getLogger(LocalNetworkResources.class);

  protected ResourceGraph resourceGraph;
  protected final MappingsConcurrent mappings;
  protected DeviceManager deviceManager;

  protected final Vertx vertx;

  /**
   * Injection constructor.
   *
   * @param vProv the vertx provider.
   */
  @Inject
  public LocalNetworkResources(VertxProvider vProv, final SpecificationProvider specProvider, DeviceManager deviceManager) {
    this.resourceGraph = specProvider.getResourceGraph();
    this.mappings = specProvider.getMappings();
    this.deviceManager = deviceManager;
    this.vertx = vProv.getVertx();
  }

  @Override
  public void init() {
    var registrationServer = new DeviceRegistrationVerticle(this.deviceManager);
    this.vertx.deployVerticle(registrationServer).onComplete(asyncRes -> {
      this.deviceManager.startSearch();
    });

    CountDownLatch latch = new CountDownLatch(1);
    int secondsToWait = 5; // Should be configurable in GUI.

    this.vertx.setTimer(secondsToWait * 1000, timerId -> {
      latch.countDown();
    });

    try {
      latch.await();
    } catch (InterruptedException e) {
      throw new IllegalStateException("Interrupted while waiting for the LN devices", e);
    }

    for (Device device : deviceManager.getDiscoveredDevices()) {
      deviceManager.addDevice(device);
    }
  }

  @Override
  public void close() {
    deviceManager.releaseAllDevice();
  }
}
