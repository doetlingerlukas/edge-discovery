package edge.discovery;

import com.google.inject.Inject;
import io.vertx.core.json.JsonArray;
import org.apache.commons.net.util.SubnetUtils;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Class handling a UDP broadcast to signal potential edge devices.
 *
 * @author Lukas Dötlinger
 */
public class DiscoverySearch {

  private List<InterfaceAddress> subnets = new ArrayList<>();

  /**
   * Constructs a list of broadcast addresses for every network interface.
   */
  @Inject
  public DiscoverySearch() {
    try {
      NetworkInterface.networkInterfaces()
        .forEach(i -> {
          try {
            if (i.isUp()) {
              this.subnets.addAll(i.getInterfaceAddresses().stream()
                .filter(si -> si.getAddress() instanceof Inet4Address)
                .collect(Collectors.toList()));
            }
          } catch (SocketException e) {
            e.printStackTrace();
          }
        });
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Send a broadcast to all subnets.
   */
  public void broadcast() {
    this.subnets.stream()
      .map(InterfaceAddress::getBroadcast)
      .filter(Objects::nonNull)
      .forEach(s -> {
        try {
          var socket = new DatagramSocket();
          socket.setBroadcast(true);

          var buffer = Constants.broadcastAvailableMessage.getBytes();
          var packet = new DatagramPacket(buffer, buffer.length, s, Constants.broadcastPort);

          socket.send(packet);
          socket.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      });
  }

  /**
   * Send a UDP packet to all IP addresses in each network interface's subnet.
   * Use only if UDP broadcast is blocked on network.
   */
  public void sendToAll() {
    this.subnets.stream()
      .filter(si -> !si.getAddress().isLoopbackAddress())
      .forEach(si -> {

        var address = si.getAddress().getHostAddress();
        var prefix = si.getNetworkPrefixLength();

        var subnetUtils = new SubnetUtils(address + "/" + prefix);
        Arrays.stream(subnetUtils.getInfo().getAllAddresses())
          .forEach(a -> {
            try {
              var socket = new DatagramSocket();

              var buffer = Constants.broadcastAvailableMessage.getBytes();
              var packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(a), Constants.broadcastPort);

              socket.send(packet);
              socket.close();
            } catch (IOException e) {
              e.printStackTrace();
            }
          });
      });
  }

  public JsonArray getSubnetsAsJson() {
    var json = new JsonArray();

    subnets.stream()
      .map(InterfaceAddress::getBroadcast)
      .filter(Objects::nonNull)
      .map(InetAddress::getHostAddress)
      .forEach(json::add);

    return json;
  }
}
