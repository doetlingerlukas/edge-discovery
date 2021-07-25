package edge.discovery.routes;

import edge.discovery.Constants;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class ReqHandlerRegister implements Handler<RoutingContext> {

  @Override
  public void handle(RoutingContext event) {
    var res = event.response();
    var req = event.getBodyAsString();

    event.vertx().eventBus().publish(Constants.eventBusName, req);
    res.setStatusCode(200).end("Ok!");
  }
}
