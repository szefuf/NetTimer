package szefuf.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;

import static szefuf.micrometer.TimerType.*;
import static szefuf.micrometer.utils.TagUtils.addTypeTag;

//TODO: make it implement io.micrometer.core.instrument.Timer
//TODO 2: make it reusable on "start" level - there is no point in calling meterRegistry,timer every time
public class NetTimer {

    MeterRegistry meterRegistry;
    Timer grossTimer = null;
    Timer.Sample grossSample = null;
    Timer temporaryTimer = null;
    Timer.Sample temporarySample = null;
    Long pausedTime = 0L;
    String name;
    String[] tags;

    private NetTimer() { }

    private NetTimer(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public static NetTimer instance(MeterRegistry meterRegistry) {
        var netTimer = new NetTimer(meterRegistry);
        return netTimer;
    }

    public static NetTimer startInstance(MeterRegistry meterRegistry, String name, String... tags) {
        assert (tags == null || tags.length % 2 == 0);
        var netTimer = instance(meterRegistry);
        netTimer.start(name, tags);
        return netTimer;
    }

    public NetTimer start(String name, String... tags) {
        if (grossTimer != null) {
            throw new IllegalStateException("Timer is already running");
        }
        this.name = name;
        this.tags = tags;
        this.grossTimer = meterRegistry.timer(name, addTypeTag(tags, GROSS));
        this.grossSample = Timer.start(meterRegistry);
        return this;
    }

    public void stop() {
        if (grossTimer == null) {
            throw new IllegalStateException("Timer has not been started");
        }
        if (temporaryTimer != null) {
            var stop = this.temporarySample.stop(temporaryTimer);
            this.pausedTime += stop;
            this.temporaryTimer = null;
            this.temporarySample = null;
        }
        var stop = this.grossSample.stop(grossTimer);
        var netTimer = meterRegistry.timer(name, addTypeTag(tags, NET));
        netTimer.record(stop - pausedTime, TimeUnit.NANOSECONDS);
        this.grossSample = null;
        this.grossTimer = null;
        this.pausedTime = 0L;
    }

    public NetTimer pause(String reason) {
        if (temporaryTimer != null) {
            throw new IllegalStateException("Timer is already paused");
        } else if (grossTimer == null) {
            throw new IllegalStateException("Timer has not been started");
        }
        this.temporaryTimer = meterRegistry.timer(this.name, addTypeTag(this.tags, PAUSE, reason));
        this.temporarySample = Timer.start(meterRegistry);
        return this;
    }

    public NetTimer resume() {
        if (temporaryTimer == null) {
            throw new IllegalStateException("Timer is not paused");
        }
        var stop = this.temporarySample.stop(temporaryTimer);
        this.pausedTime += stop;
        this.temporaryTimer = null;
        this.temporarySample = null;
        return this;
    }

}
