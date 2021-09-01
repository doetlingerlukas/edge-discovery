package edge.discovery.device;

/**
 * Class representing an edge device.
 *
 * @author Lukas Dötlinger
 */
public class Device {

  private final int id;
  private final String address;
  private final String name;

  private DeviceArch arch;
  private int numCores;
  private int ramSize;
  private String key;

  public Device(int id, String address, String name, String key) {
    this.id = id;
    this.address = address;
    this.name = name;
    this.key = key;
  }

  public int getId() {
    return id;
  }

  public String getAddress() {
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
}
