package edge.discovery.modules;

import org.opt4j.core.config.annotations.Info;
import org.opt4j.core.config.annotations.Order;
import org.opt4j.core.start.Constant;
import at.uibk.dps.ee.enactables.serverless.FunctionFactoryServerless;
import at.uibk.dps.ee.guice.modules.ResourceModule;
import edge.discovery.LocalNetworkResources;
import edge.discovery.device.DeviceManager;
import edge.discovery.graph.FunctionFactoryServerlessLocal;

/**
 * Configures the classes for the init and cleanup of local network resources.
 *
 * @author Fedor Smirnov
 */
public class LNResourcesModule extends ResourceModule {

  @Order(1)
  @Info("The time (in seconds) for the broadcast to discover local resources.")
  @Constant(value = "waitTimeInit", namespace = DeviceManager.class)
  public int waitTimeInit = 5;

  @Override
  protected void config() {
    addLocalResources(LocalNetworkResources.class);
    bind(FunctionFactoryServerless.class).to(FunctionFactoryServerlessLocal.class);
  }

  public int getWaitTimeInit() {
    return waitTimeInit;
  }

  public void setWaitTimeInit(int waitTimeInit) {
    this.waitTimeInit = waitTimeInit;
  }
}
