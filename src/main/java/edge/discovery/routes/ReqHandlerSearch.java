package edge.discovery.routes;

import edge.discovery.DiscoverySearch;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class ReqHandlerSearch implements Handler<RoutingContext> {

  @Override
  public void handle(RoutingContext event) {
    var discoveryModule = new DiscoverySearch();
    discoveryModule.broadcast();

    event.response()
      .setStatusCode(200)
      .end(discoveryModule.getSubnetsAsJson().toString());
  }
}
