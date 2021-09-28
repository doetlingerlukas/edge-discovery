package edge.discovery;

import at.uibk.dps.ee.model.properties.PropertyServiceMapping;
import at.uibk.dps.ee.model.properties.PropertyServiceMappingLocal;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import at.uibk.dps.ee.core.LocalResources;
import at.uibk.dps.ee.guice.starter.VertxProvider;
import at.uibk.dps.ee.model.graph.ResourceGraph;
import at.uibk.dps.ee.model.graph.SpecificationProvider;
import edge.discovery.device.DeviceManager;
import io.vertx.core.Vertx;
import net.sf.opendse.model.Mapping;
import net.sf.opendse.model.Mappings;
import net.sf.opendse.model.Resource;
import net.sf.opendse.model.Task;

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
  protected final Mappings<Task, Resource> mappings;
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
    var verticle = new LocalNetworkVerticle(this.deviceManager);
    this.vertx.deployVerticle(verticle);

    // Start broadcast in subnets.
    this.deviceManager.startSearch();
  }

  @Override
  public void close() {
    deviceManager.releaseAllDevice();
  }

}
