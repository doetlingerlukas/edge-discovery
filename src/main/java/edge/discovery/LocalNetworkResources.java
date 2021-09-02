package edge.discovery;

import com.google.inject.Inject;
import at.uibk.dps.ee.core.LocalResources;
import at.uibk.dps.ee.guice.starter.VertxProvider;
import io.vertx.core.Vertx;

/**
 * Class implementing the operations require for the init and the cleanup of
 * usable processing resources within the same local network as the triggerring
 * Apollo instance.
 * 
 * @author Fedor Smirnov
 */
public class LocalNetworkResources implements LocalResources {

  protected final Vertx vertx;

  /**
   * Injection constructor.
   * 
   * @param vProv the vertx provider.
   */
  @Inject
  public LocalNetworkResources(VertxProvider vProv) {
    this.vertx = vProv.getVertx();
  }

  @Override
  public void init() {
    // Create the Discovery Verticle (should maybe be named LocalNetwork Verticle or
    // sth since it is going to do more than just discovering the stuff) using the injected Vertx context
    
    // The whole adjustment of the Specification also has to happen here. 

  }

  @Override
  public void close() {
    // Derigister the current Apollo instance from the occupied local network resources.

  }

}
