package edge.discovery.graph;

import java.util.Set;
import com.google.inject.Inject;
import at.uibk.dps.ee.core.function.EnactmentFunction;
import at.uibk.dps.ee.core.function.FunctionDecoratorFactory;
import at.uibk.dps.ee.enactables.FactoryInputUser;
import at.uibk.dps.ee.enactables.serverless.FunctionFactoryServerless;
import at.uibk.dps.ee.guice.starter.VertxProvider;

/**
 * Extension of the class capable of working with mappings to local resources
 * (TODO: this integration is ugly. Modularity needs to be improved).
 * 
 * @author Fedor Smirnov
 *
 */
public class FunctionFactoryServerlessLocal extends FunctionFactoryServerless {

  @Inject
  public FunctionFactoryServerlessLocal(Set<FunctionDecoratorFactory> decoratorFactories,
      VertxProvider vProv) {
    super(decoratorFactories, vProv);
  }

  @Override
  protected EnactmentFunction makeActualFunction(FactoryInputUser input) {
    if (PsMappingLocalRes.isLocResMapping(input.getMapping())) {
      return new LocalResFunction(input.getTask(), input.getMapping(), vProv.getWebClient());
    } else {
      return super.makeActualFunction(input);
    }
  }
}
