package edge.discovery.device;

import java.net.InetAddress;

/**
 * Class representing an edge device.
 *
 * @author Lukas Dötlinger
 */
public class Device {

  private final int id;
  private final InetAddress address;
  private final String name;
  private final DeviceBenchmark benchmark;

  private DeviceArch arch;
  private int numCores;
  private int ramSize;
  private String key;

  public Device(int id, InetAddress address, String name, String key, DeviceBenchmark benchmark) {
    this.id = id;
    this.address = address;
    this.name = name;
    this.key = key;
    this.benchmark = benchmark;
  }

  public int getId() {
    return id;
  }

  public InetAddress getAddress() {
    return address;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getName() {
    return name;
  }

  public DeviceArch getArch() {
    return arch;
  }

  public void setArch(DeviceArch arch) {
    this.arch = arch;
  }

  public int getNumCores() {
    return numCores;
  }

  public void setNumCores(int numCores) {
    this.numCores = numCores;
  }

  public int getRamSize() {
    return ramSize;
  }

  public void setRamSize(int ramSize) {
    this.ramSize = ramSize;
  }

  public DeviceBenchmark getBenchmark() {
    return benchmark;
  }
}
