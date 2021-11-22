package edge.discovery.modules;

import at.uibk.dps.ee.enactables.serverless.FunctionFactoryServerless;
import at.uibk.dps.ee.guice.modules.ResourceModule;
import edge.discovery.LocalNetworkResources;
import edge.discovery.graph.FunctionFactoryServerlessLocal;

/**
 * Configures the classes for the init and cleanup of local network resources.
 *
 * @author Fedor Smirnov
 */
public class LNResourcesModule extends ResourceModule {

  @Override
  protected void config() {
    addLocalResources(LocalNetworkResources.class);
    bind(FunctionFactoryServerless.class).to(FunctionFactoryServerlessLocal.class);
  }
}
