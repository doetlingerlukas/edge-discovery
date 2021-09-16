package edge.discovery;

import java.util.concurrent.CountDownLatch;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import at.uibk.dps.ee.core.LocalResources;
import at.uibk.dps.ee.guice.starter.VertxProvider;
import at.uibk.dps.ee.model.graph.ResourceGraph;
import at.uibk.dps.ee.model.graph.SpecificationProvider;
import edge.discovery.device.DeviceManager;
import io.vertx.core.Vertx;

/**
 * Class implementing the operations require for the init and the cleanup of
 * usable processing resources within the same local network as the triggering
 * Apollo instance.
 *
 * @author Fedor Smirnov, Lukas Dötlinger
 */
@Singleton
public class LocalNetworkResources implements LocalResources {

  protected ResourceGraph resourceGraph;
  protected DeviceManager deviceManager;

  protected final Vertx vertx;

  /**
   * Injection constructor.
   *
   * @param vProv the vertx provider.
   */
  @Inject
  public LocalNetworkResources(VertxProvider vProv, final SpecificationProvider specProvider,
      DeviceManager deviceManager) {
    this.resourceGraph = specProvider.getResourceGraph();
    this.deviceManager = deviceManager;
    this.vertx = vProv.getVertx();
  }

  @Override
  public void init() {
    var verticle = new LocalNetworkVerticle(this.deviceManager);
    this.vertx.deployVerticle(verticle);

    // So, my guess is that it just takes too long for the device to answer so that
    // it is added while the enactment is already under way (we will implement sth
    // like this later, but it comes with a whole load of concurrency and
    // synchronization issues)

    // Start broadcast in subnets.
    this.deviceManager.startSearch(); // this one is effectively asynchronous, since the device node
                                      // is only added after the device answers -> Apollo just does
                                      // its thing, before the devices can answer

    // try to wait at this point, so that this method blocks the rest of the flow
    // until we have the device answers

    CountDownLatch latch = new CountDownLatch(1);
    int secondsToWait = 5; // this one should be configurable via Gui

    this.vertx.setTimer(secondsToWait * 1000, timerId -> {
      latch.countDown();
    });

    try {
      latch.await();
    } catch (InterruptedException e) {
      throw new IllegalStateException("Interrupted while waiting for the LN devices", e);
    }
  }

  @Override
  public void close() {
    deviceManager.releaseAllDevice();
  }

}
