package edge.discovery.routes;

import edge.discovery.device.DeviceManager;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used internally by Apollo to deploy a function to an edge-device.
 *
 * @author Lukas Dötlinger
 */
public class ReqHandlerDeploy implements Handler<RoutingContext> {

  private DeviceManager deviceManager;

  protected final Logger logger = LoggerFactory.getLogger(ReqHandlerDeploy.class);

  public ReqHandlerDeploy(DeviceManager deviceManager) {
    this.deviceManager = deviceManager;
  }

  @Override
  public void handle(RoutingContext event) {
    var res = event.response();
    var req = event.getBodyAsJson();

    var deviceId = req.getInteger("deviceId");
    var function = req.getString("function");

    var opt = deviceManager.getDeviceById(deviceId);

    if (opt.isPresent()) {
      Future<Boolean> future = deviceManager.deployFunction(opt.get(), function);

      future.onComplete(asyncRes -> {
        res.setStatusCode(200).end(asyncRes.result().toString());
      });
    } else {
      res.setStatusCode(404).end("Device not found.");
    }
  }
}
