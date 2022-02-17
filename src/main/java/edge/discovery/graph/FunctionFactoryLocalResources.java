package edge.discovery.graph;

import java.util.Set;
import com.google.inject.Inject;
import at.uibk.dps.ee.core.function.EnactmentFunction;
import at.uibk.dps.ee.enactables.FactoryInputUser;
import at.uibk.dps.ee.enactables.FunctionFactoryUser;
import at.uibk.dps.ee.enactables.decorators.FunctionDecoratorFactory;
import at.uibk.dps.ee.guice.starter.VertxProvider;

/**
 * Extension of the class capable of working with mappings to local resources
 * (TODO: this integration is ugly. Modularity needs to be improved).
 * 
 * @author Fedor Smirnov
 *
 */
public class FunctionFactoryLocalResources extends FunctionFactoryUser {

  protected final VertxProvider vProv;

  @Inject
  public FunctionFactoryLocalResources(Set<FunctionDecoratorFactory> decoratorFactories,
      VertxProvider vProv) {
    super(decoratorFactories);
    this.vProv = vProv;
  }

  @Override
  protected EnactmentFunction makeActualFunction(FactoryInputUser input) {
    return new LocalNetworkFunction(input.getTask(), input.getMapping(), vProv.getWebClient());
  }

  @Override
  public boolean isApplicable(FactoryInputUser factoryInput) {
    return PsMappingLocalRes.isLocResMapping(factoryInput.getMapping());
  }
}
