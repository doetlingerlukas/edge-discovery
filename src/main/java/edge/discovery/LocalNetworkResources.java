package edge.discovery;

import at.uibk.dps.ee.guice.init_term.ManagedComponent;
import at.uibk.dps.ee.guice.starter.VertxProvider;
import at.uibk.dps.ee.model.graph.MappingsConcurrent;
import at.uibk.dps.ee.model.graph.ResourceGraph;
import at.uibk.dps.ee.model.graph.SpecificationProvider;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import edge.discovery.device.Device;
import edge.discovery.device.DeviceManager;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import org.opt4j.core.start.Constant;
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
public class LocalNetworkResources implements ManagedComponent {

  protected final Logger logger = LoggerFactory.getLogger(LocalNetworkResources.class);

  protected ResourceGraph resourceGraph;
  protected final MappingsConcurrent mappings;
  protected DeviceManager deviceManager;
  protected final int waitTimeInit;

  protected final Vertx vertx;

  /**
   * Injection constructor.
   *
   * @param vProv the vertx provider.
   */
  @Inject
  public LocalNetworkResources(VertxProvider vProv, final SpecificationProvider specProvider,
    DeviceManager deviceManager, @Constant(namespace = LocalNetworkResources.class,
    value = "waitTimeInit") final int waitTimeInit) {

    this.waitTimeInit = waitTimeInit;
    this.resourceGraph = specProvider.getResourceGraph();
    this.mappings = specProvider.getMappings();
    this.deviceManager = deviceManager;
    this.vertx = vProv.getVertx();
  }

  @Override
  public Future<String> initialize() {
    Promise<String> promise = Promise.promise();

    var registrationServer = new DeviceRegistrationVerticle(this.deviceManager);
    this.vertx.deployVerticle(registrationServer).onComplete(asyncRes -> {
      this.deviceManager.startSearch();
    });

    this.vertx.setTimer(waitTimeInit * 1000, timerId -> {
      promise.complete("Device discovery completed");
      logger.info("Successfully waited for new devices for {} seconds.", waitTimeInit);
    });

    return promise.future();
  }

  @Override
  public Future<String> terminate() {
    return Future.future(promise -> {
      deviceManager.releaseAllDevice();
      promise.complete("Devices released");
    });
  }


}
