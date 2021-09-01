package edge.discovery.routes;

import com.google.gson.Gson;
import edge.discovery.Constants;
import edge.discovery.device.Device;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used by edge-devices to register to an Apollo instance.
 *
 * @author Lukas Dötlinger
 */
public class ReqHandlerRegister implements Handler<RoutingContext> {

  protected final Logger logger = LoggerFactory.getLogger(ReqHandlerRegister.class);

  @Override
  public void handle(RoutingContext event) {
    var res = event.response();
    var req = event.getBodyAsString();

    logger.info("Received device information: " + req);

    var reqJson = event.getBodyAsJson();
    // TODO: Save new device in some data structure. Maybe use VertX Shared Memory for this?

    event.vertx().eventBus().publish(Constants.eventBusName, req);
    res.setStatusCode(200).end("Ok!");
  }
}
