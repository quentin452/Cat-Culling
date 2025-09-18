package net.tclproject.mysteriumlib.math;

public class Stopwatch {
  final String name;
  long timeStart;
  double calls;
  double timeTotal;
  double timeMax;
  double timeMin;
  double timeTaken;

  public Stopwatch(String stopwatchName) {
    this.name = stopwatchName;
    this.timeMax = -1.0;
    this.timeMin = -1.0;
  }

  public void start() {
    this.timeStart = System.nanoTime();
  }

  public void stop() {
    this.calls += 1.0;
    double timeTaken = (double) (System.nanoTime() - this.timeStart) / 1000000.0;
    this.timeTotal += timeTaken;
    this.timeMax = this.timeMax == -1.0 ? timeTaken : Math.max(timeTaken, this.timeMax);
    this.timeMin = this.timeMin == -1.0 ? timeTaken : Math.min(timeTaken, this.timeMin);
  }

  public String toString() {
    return String.format(
        "[%s]: Time [avg]: %3.2f ms, [min]: %3.2f ms, [max]: %3.2f ms",
        this.name, this.timeTotal / this.calls, this.timeMin, this.timeMax);
  }
}
