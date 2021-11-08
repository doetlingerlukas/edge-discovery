package edge.discovery.device;

import java.time.Duration;

/**
 * Class representing a device benchmark run.
 *
 * @author Lukas Dötlinger
 */
public class DeviceBenchmark {

  private long deployment;
  private long invocations;

  public DeviceBenchmark() {}

  public DeviceBenchmark(long deployment, long invocations) {
    this.deployment = deployment;
    this.invocations = invocations;
  }

  public Duration getDeployment() {
    return Duration.ofNanos(deployment);
  }

  public Duration getInvocations() {
    return Duration.ofNanos(invocations);
  }

  public void setDeployment(long deployment) {
    this.deployment = deployment;
  }

  public void setInvocations(long invocations) {
    this.invocations = invocations;
  }
}
