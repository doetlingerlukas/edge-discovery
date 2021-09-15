package edge.discovery.device;

import at.uibk.dps.ee.guice.starter.VertxProvider;
import edge.discovery.Constants;
import edge.discovery.DiscoverySearch;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Used to manage connected network devices.
 *
 * @author Lukas Dötlinger
 */
@Singleton
public class DeviceManager {

  private final Logger logger = LoggerFactory.getLogger(DeviceManager.class);

  private int nextDeviceId = 1;
  private List<Device> devices;
  private WebClient httpClient;
  private DiscoverySearch discoverySearch;

  @Inject
  public DeviceManager(VertxProvider vProv, DiscoverySearch discoverySearch) {
    this.devices = new ArrayList<>();
    this.httpClient = WebClient.create(vProv.getVertx());
    this.discoverySearch = discoverySearch;
  }

  /**
   * Broadcast message to all device in network to discover new devices.
   */
  public void startSearch() {
    this.discoverySearch.broadcast();
  }

  public JsonArray getNetworkSubnetsAsJson() {
    return this.discoverySearch.getSubnetsAsJson();
  }

  public Optional<Device> getDeviceById(int id) {
    return this.devices.stream()
      .filter(device -> device.getId() == id)
      .findFirst();
  }

  public void addDevice(Device device) {
    devices.add(device);
  }

  /**
   * Deploys a serverless function to a device.
   *
   * @param id of the device the function is deployed to.
   * @param function, the name of the function.
   * @return true on success, false otherwise.
   */
  public Future<Boolean> deployFunction(int id, String function) {
    var deviceOptional = getDeviceById(id);
    if (deviceOptional.isEmpty()) {
      return Future.failedFuture("Device id of " + id + " unknown");
    }
    var device = deviceOptional.get();
    Promise<Boolean> promise = Promise.promise();

    httpClient.post("https://" + device.getAddress().toString() + ":8080/system/function")
      .putHeader("content-type", "application/json")
      .sendJson(new JsonObject()
        .put("service", function)
        .put("image", function)
        .put("registryAuth", device.getKey())
        .toString())
      .onSuccess(res -> {
        if (res.statusCode() == 200) {
          promise.complete(true);
        } else {
          promise.complete(false);
        }
      })
      .onFailure(e -> logger.debug(e.getMessage()));

    return promise.future();
  }

  /**
   * Releases a device to be used by other Apollo instances.
   *
   * @param device to be releases.
   * @throws IOException if socket or packet fails.
   */
  public void releaseDevice(Device device) throws IOException {
    var socket = new DatagramSocket();
    socket.setBroadcast(true);

    var buffer = Constants.broadcastReleaseMessage.getBytes();
    var packet = new DatagramPacket(buffer, buffer.length, device.getAddress(), Constants.broadcastPort);

    socket.send(packet);
    socket.close();

    removeDeviceFromList(device);
  }

  public void releaseAllDevice() {
    this.devices
      .forEach(device -> {
        try {
          releaseDevice(device);
        } catch (IOException e) {
          e.printStackTrace();
        }
      });
  }

  public void removeDeviceFromList(Device device) {
    this.devices = this.devices.stream()
      .filter(d -> d.getId() == device.getId())
      .collect(Collectors.toList());
  }

  public int getNextDeviceId() {
    return this.nextDeviceId++;
  }
}
