package edge.discovery;

import com.google.inject.Inject;
import at.uibk.dps.ee.core.LocalResources;
import at.uibk.dps.ee.guice.starter.VertxProvider;
import edge.discovery.device.DeviceManager;
import io.vertx.core.Vertx;

/**
 * Class implementing the operations require for the init and the cleanup of
 * usable processing resources within the same local network as the triggering
 * Apollo instance.
 * 
 * @author Fedor Smirnov, Lukas Dötlinger
 */
public class LocalNetworkResources implements LocalResources {

  private final DiscoverySearch discoverySearch;
  private DeviceManager deviceManager;

  protected final Vertx vertx;

  /**
   * Injection constructor.
   * 
   * @param vProv the vertx provider.
   */
  @Inject
  public LocalNetworkResources(VertxProvider vProv) {
    this.discoverySearch = new DiscoverySearch();
    this.deviceManager = new DeviceManager(vProv);
    this.vertx = vProv.getVertx();
  }

  @Override
  public void init() {
    var verticle = new LocalNetworkVerticle(this.deviceManager);
    this.vertx.deployVerticle(verticle);

    // Start broadcast in subnets.
    this.discoverySearch.broadcast();
    
    // The whole adjustment of the Specification also has to happen here.
  }

  @Override
  public void close() {
    deviceManager.releaseAllDevice();
  }

}
