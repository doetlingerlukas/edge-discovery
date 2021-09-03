package edge.discovery.routes;

import edge.discovery.Constants;
import edge.discovery.device.Device;
import edge.discovery.device.DeviceArch;
import edge.discovery.device.DeviceManager;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Used by edge-devices to register to an Apollo instance.
 *
 * @author Lukas Dötlinger
 */
public class ReqHandlerRegister implements Handler<RoutingContext> {

  private final Logger logger = LoggerFactory.getLogger(ReqHandlerRegister.class);

  private DeviceManager manager;

  public ReqHandlerRegister(DeviceManager deviceManager) {
    this.manager = deviceManager;
  }

  @Override
  public void handle(RoutingContext event) {
    var res = event.response();
    var req = event.getBodyAsString();

    logger.info("Received device information: " + req);

    var reqJson = event.getBodyAsJson();

    InetAddress address = InetAddress.getLoopbackAddress();
    try {
       address = InetAddress.getByName(event.request().remoteAddress().hostAddress());
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }
    var newDevice = new Device(manager.getNextDeviceId(), address, reqJson.getString("name"), reqJson.getString("key"));
    newDevice.setNumCores(reqJson.getInteger("numCores"));
    newDevice.setRamSize(reqJson.getFloat("ramSize").intValue());
    newDevice.setArch(DeviceArch.valueOf(reqJson.getString("arch").toUpperCase()));

    manager.addDevice(newDevice);

    event.vertx().eventBus().publish(Constants.eventBusName, newDevice);
    res.setStatusCode(200).end("Ok!");
  }
}
