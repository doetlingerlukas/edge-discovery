package edge.discovery.routes;

import edge.discovery.DiscoveryModule;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;

public class ReqHandlerSearch implements Handler<RoutingContext> {

  @Override
  public void handle(RoutingContext event) {
    var discoveryModule = new DiscoveryModule();
    discoveryModule.broadcast();

    event.response()
      .setStatusCode(200)
      .end(discoveryModule.getSubnetsAsJson().toString());
  }
}
