package edge.discovery.device;

import at.uibk.dps.ee.guice.starter.VertxProvider;
import at.uibk.dps.ee.model.graph.EnactmentSpecification;
import at.uibk.dps.ee.model.graph.SpecificationProvider;
import at.uibk.dps.ee.model.properties.PropertyServiceMapping;
import at.uibk.dps.ee.model.properties.PropertyServiceMappingLocal;
import edge.discovery.Constants;
import edge.discovery.DiscoverySearch;
import edge.discovery.graph.SpecificationUpdate;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.Base64;
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
  protected final SpecificationUpdate specUpdate;
  protected final EnactmentSpecification spec;
  protected final Vertx vertx;

  @Inject
  public DeviceManager(VertxProvider vProv, DiscoverySearch discoverySearch,
      final SpecificationUpdate specUpdate, SpecificationProvider specProv) {
    this.devices = new ArrayList<>();
    this.vertx = vProv.getVertx();
    this.httpClient = WebClient.create(vertx);
    this.discoverySearch = discoverySearch;
    this.specUpdate = specUpdate;
    this.spec = specProv.getSpecification();
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
    return this.devices.stream().filter(device -> device.getId() == id).findFirst();
  }

  public void addDevice(Device device) {
    devices.add(device);

    spec.getMappings().forEach(m -> {
      if (PropertyServiceMapping.getEnactmentMode(m).equals(PropertyServiceMapping.EnactmentMode.Local)) {
        var image = PropertyServiceMappingLocal.getImageName(m);

        var future = deployFunction(device.getId(), image);
        vertx.executeBlocking(promise -> {
          future.onSuccess(res -> promise.complete());
        });
      }
    });

    specUpdate.addLocalResourceToModel(device);
  }

  /**
   * @param device
   * @return the base url of the faasd API for a given device
   */
  public static String getDeviceFunctionUrl(Device device) {
    return "http://" + device.getAddressString() + ":8080/function/";
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

    httpClient.post(8080, device.getAddressString(), "/system/functions")
      .basicAuthentication("admin", device.getKey())
      .putHeader("content-type", "application/json")
      .sendJson(new JsonObject().put("service", function.replaceAll(".+/", ""))
        .put("image", function))
      .onSuccess(res -> {
        if (res.statusCode() == 200) {
          checkFunctionAvailability(promise, device, function);
        } else {
          promise.complete(false);
        }
      }).onFailure(e -> logger.debug(e.getMessage()));

    return promise.future();
  }

  /**
   * Checks if a function is available on a host.
   *
   * @param promise to be fulfilled with true iff function is available
   * @param device the function is running
   * @param function that is being tested
   */
  private void checkFunctionAvailability(Promise<Boolean> promise, Device device, String function) {
    httpClient.post(8080, device.getAddressString(),
      "/function/" + function.replaceAll(".+/", ""))
      .basicAuthentication("admin", device.getKey())
      .putHeader("content-type", "application/json")
      .sendJson(new JsonObject())
      .onSuccess(res -> {
        if (res.statusCode() == 404) {
          // The function was not found on the host and is therefore not ready.

          vertx.setTimer(5000, id -> {
            checkFunctionAvailability(promise, device, function);
          });
        } else {
          promise.complete(true);
        }
      }).onFailure(e ->  {
        logger.debug(e.getMessage());
        promise.complete(false);
      });
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
    var packet =
        new DatagramPacket(buffer, buffer.length, device.getAddress(), Constants.broadcastPort);

    socket.send(packet);
    socket.close();

    removeDeviceFromList(device);
  }

  public void releaseAllDevice() {
    this.devices.forEach(device -> {
      try {
        releaseDevice(device);
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
  }

  public void removeDeviceFromList(Device device) {
    this.devices =
        this.devices.stream().filter(d -> d.getId() == device.getId()).collect(Collectors.toList());
  }

  public int getNextDeviceId() {
    return this.nextDeviceId++;
  }
}
