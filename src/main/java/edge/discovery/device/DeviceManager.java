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
import org.opt4j.core.start.Constant;
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

  protected final int waitTimeDiscovery;
  protected final Set<Device> discoveredDevices;

  @Inject
  public DeviceManager(VertxProvider vProv, DiscoverySearch discoverySearch,
      final SpecificationUpdate specUpdate, SpecificationProvider specProv,
      @Constant(value = "waitTimeInit",
          namespace = DeviceManager.class) final int waitTimeDiscovery) {
    this.devices = new ArrayList<>();
    this.vertx = vProv.getVertx();
    this.httpClient = WebClient.create(vertx);
    this.discoverySearch = discoverySearch;
    this.specUpdate = specUpdate;
    this.spec = specProv.getSpecification();
    this.waitTimeDiscovery = waitTimeDiscovery;
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
  public void addDevice(Promise<Boolean> promise, Device device) {
    logger.info("Deploying functions to device " + device.getUniqueName());

    spec.getMappings().forEach(m -> {
      if (PropertyServiceMapping.getEnactmentMode(m)
          .equals(PropertyServiceMapping.EnactmentMode.Local)) {
        var image = PropertyServiceMappingLocal.getImageName(m);

        CountDownLatch funcDeployLatch = new CountDownLatch(2);

        deployFunction(device, image).onComplete(asyncRes -> {
          funcDeployLatch.countDown();
        });

        scaleFunction(device, image).onComplete(asyncRes -> {
          funcDeployLatch.countDown();
        });

        try {
          funcDeployLatch.await();
        } catch (InterruptedException e) {
          throw new IllegalStateException("Interrupted while waiting for function deployment", e);
        }
      }
    });

    specUpdate.addLocalResourceToModel(device);
    promise.complete(true);
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
