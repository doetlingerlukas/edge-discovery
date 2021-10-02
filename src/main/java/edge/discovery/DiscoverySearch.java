package edge.discovery;

import com.google.inject.Inject;
import io.vertx.core.json.JsonArray;
import org.apache.commons.net.util.SubnetUtils;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class handling a UDP broadcast to signal potential edge devices.
 *
 * @author Lukas Dötlinger
 */
public class DiscoverySearch {

  private List<InterfaceAddress> subnets = new ArrayList<>();
  protected Set<InetAddress> broadcastAddresses = new HashSet<>();

  /**
   * Constructs a list of broadcast addresses for every network interface.
   */
  @Inject
  public DiscoverySearch() {
    try {
      NetworkInterface.networkInterfaces().forEach(i -> {
        try {
          if (i.isUp()) {
            var new_subnets = i.getInterfaceAddresses().stream()
              .filter(si -> si.getAddress() instanceof Inet4Address)
              .collect(Collectors.toList());

            this.broadcastAddresses.addAll(new_subnets.stream()
              .map(InterfaceAddress::getBroadcast)
              .filter(Objects::nonNull)
              .collect(Collectors.toList()));

            this.subnets.addAll(new_subnets);
          }
        } catch (SocketException e) {
          e.printStackTrace();
        }
      });
    } catch (Exception e) {
     e.printStackTrace();
    }

    // Used for debugging
    try {
      broadcastAddresses.add(InetAddress.getByName("192.168.0.255"));
      broadcastAddresses.add(InetAddress.getByName("127.0.0.1"));
    } catch (UnknownHostException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  /**
   * Send a broadcast to all subnets.
   */
  public void broadcast() {
    this.broadcastAddresses.forEach(s -> {
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
   * Send a UDP packet to all IP addresses in each network interface's subnet. Use
   * only if UDP broadcast is blocked on network.
   */
  public void sendToAll() {
    this.subnets.stream().filter(si -> !si.getAddress().isLoopbackAddress()).forEach(si -> {

      var address = si.getAddress().getHostAddress();
      var prefix = si.getNetworkPrefixLength();

      var subnetUtils = new SubnetUtils(address + "/" + prefix);
      Arrays.stream(subnetUtils.getInfo().getAllAddresses()).forEach(a -> {
        try {
          var socket = new DatagramSocket();

          var buffer = Constants.broadcastAvailableMessage.getBytes();
          var packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(a),
              Constants.broadcastPort);

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

    subnets.stream().map(InterfaceAddress::getBroadcast).filter(Objects::nonNull)
        .map(InetAddress::getHostAddress).forEach(json::add);

    return json;
  }
}
