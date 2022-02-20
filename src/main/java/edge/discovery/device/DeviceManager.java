package edge.discovery.device;

import at.uibk.dps.ee.guice.starter.VertxProvider;
import at.uibk.dps.ee.model.graph.EnactmentSpecification;
import at.uibk.dps.ee.model.graph.SpecificationProvider;
import at.uibk.dps.ee.model.properties.PropertyServiceMapping;
import at.uibk.dps.ee.model.properties.PropertyServiceMappingLocal;
import edge.discovery.Constants;
import edge.discovery.DiscoverySearch;
import edge.discovery.graph.SpecificationUpdate;
import io.vertx.core.CompositeFuture;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
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

  protected final Set<Device> discoveredDevices;

  @Inject
  public DeviceManager(VertxProvider vProv, DiscoverySearch discoverySearch,
      final SpecificationUpdate specUpdate, SpecificationProvider specProv) {
    this.devices = new ArrayList<>();
    this.vertx = vProv.getVertx();
    this.httpClient = WebClient.create(vertx);
    this.discoverySearch = discoverySearch;
    this.specUpdate = specUpdate;
    this.spec = specProv.getSpecification();
    this.discoveredDevices = new HashSet<>();
  }

  /**
   * Broadcast message to all device in network to discover new devices.
   */
  public void startSearch() {
    this.discoverySearch.broadcast();
  }

  public void noteDiscoveredDevice(Device device) {
    this.discoveredDevices.add(device);
  }

  public JsonArray getNetworkSubnetsAsJson() {
    return this.discoverySearch.getSubnetsAsJson();
  }

  public Optional<Device> getDeviceById(int id) {
    return this.devices.stream().filter(device -> device.getId() == id).findFirst();
  }

  /**
   * Deploys functions to a device and updates specification afterwards.
   * (blocking)
   * @param device which is added
   */
  public Future<Boolean> addDevice(Device device) {
    Promise<Boolean> promise = Promise.promise();
    logger.info("Deploying functions to {}.", device.getUniqueName());

    var futures = spec.getMappings().mappingStream()
      .filter(m -> PropertyServiceMapping.getEnactmentMode(m)
        .equals(PropertyServiceMapping.EnactmentMode.Local))
      .map(m -> {
        var image = PropertyServiceMappingLocal.getImageName(m);
        Promise<Boolean> mappingPromise = Promise.promise();

        deployFunction(device, image).onComplete(r1 -> {
          scaleFunction(device, image).onComplete(r2 -> {
            specUpdate.addLocalResourceToModel(device);
            devices.add(device);
            logger.info("Deployed function {} to device {}.", image, device.getUniqueName());
            mappingPromise.complete(true);
          });
        });

        return mappingPromise.future();
      })
      .collect(Collectors.toList());

    CompositeFuture.join(new ArrayList<>(futures))
      .onSuccess(r -> promise.complete(true))
      .onFailure(r -> promise.complete(false));

    return promise.future();
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
   * @param device the function is deployed to.
   * @param function, the name of the function.
   * @return future receiving true on success, false otherwise.
   */
  public Future<Boolean> deployFunction(Device device, String function) {
    Promise<Boolean> promise = Promise.promise();

    httpClient.post(8080, device.getAddressString(), "/system/functions")
        .basicAuthentication("admin", device.getKey()).putHeader("content-type", "application/json")
        .sendJson(
            new JsonObject().put("service", function.replaceAll(".+/", "")).put("image", function))
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
    httpClient.post(8080, device.getAddressString(), "/function/" + function.replaceAll(".+/", ""))
        .basicAuthentication("admin", device.getKey()).putHeader("content-type", "application/json")
        .sendJson(new JsonObject()).onSuccess(res -> {
          if (res.statusCode() == 404) {
            // The function was not found on the host and is therefore not ready.

            vertx.setTimer(5000, id -> {
              checkFunctionAvailability(promise, device, function);
            });
          } else {
            promise.complete(true);
          }
        }).onFailure(e -> {
          logger.debug(e.getMessage());
          promise.complete(false);
        });
  }

  /**
   * Scales a serverless function at a device to exactly one replica.
   *
   * @param device the function is scaled at.
   * @param function, the name of the function.
   * @return future receiving true on success, false otherwise.
   */
  public Future<Boolean> scaleFunction(Device device, String function) {
    Promise<Boolean> promise = Promise.promise();
    var service = function.replaceAll(".+/", "");

    httpClient.post(8080, device.getAddressString(), "/system/scale-function/" + service)
      .basicAuthentication("admin", device.getKey()).putHeader("content-type", "application/json")
      .sendJson(
        new JsonObject().put("serviceName", service).put("replicas", 1))
      .onSuccess(res -> {
        if (res.statusCode() == 200) {
          promise.complete(true);
        } else {
          promise.complete(false);
        }
      }).onFailure(e -> logger.debug(e.getMessage()));

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

  public Set<Device> getDiscoveredDevices() {
    return discoveredDevices;
  }
}
