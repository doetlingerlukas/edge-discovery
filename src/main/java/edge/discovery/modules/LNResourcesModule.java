package edge.discovery.modules;

import at.uibk.dps.ee.enactables.modules.FunctionModule;
import edge.discovery.LocalNetworkResources;
import edge.discovery.graph.FunctionFactoryLocalResources;
import org.opt4j.core.config.annotations.Info;
import org.opt4j.core.config.annotations.Order;
import org.opt4j.core.start.Constant;

/**
 * Configures the classes for the init and cleanup of local network resources.
 *
 * @author Fedor Smirnov
 */
public class LNResourcesModule extends FunctionModule {

  @Order(1)
  @Info("The time (in seconds) for the broadcast to discover local resources.")
  @Constant(value = "waitTimeInit", namespace = LocalNetworkResources.class)
  public int waitTimeInit = 5;

  @Override
  protected void config() {
    addManagedComponent(LocalNetworkResources.class);
    addFunctionFactoryUser(FunctionFactoryLocalResources.class);
  }

  public int getWaitTimeInit() {
    return waitTimeInit;
  }

  public void setWaitTimeInit(int waitTimeInit) {
    this.waitTimeInit = waitTimeInit;
  }
}
