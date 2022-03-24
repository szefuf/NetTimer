package szefuf.micrometer;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

public class NetTimerTest {

    @Test
    public void testPauseFunction() throws InterruptedException {
        //given
        var meterRegistry = new SimpleMeterRegistry();
        var netTimer = NetTimer.instance(meterRegistry);

        //when
        netTimer.start("test", "tag1", "value1");

        netTimer.pause("Thread wait");
        Thread.sleep(100);
        netTimer.resume();

        netTimer.pause("Different thread wait");
        Thread.sleep(50);
        netTimer.resume();

        netTimer.stop();

        //then
        var meters = meterRegistry.getMeters();
        var totalMap = new HashMap<String, Double>();

        for (Meter m:meters) {
            var iterator = m.measure().iterator();
            while(iterator.hasNext()) {
                var next = iterator.next();
                var tagValueRepresentation = next.getStatistic().getTagValueRepresentation();
                System.out.println(m.getId() + " - " + tagValueRepresentation + " - " + next.getValue());
                if (tagValueRepresentation.equals("total")) {
                    var tag = m.getId().getTag("timer.type");
                    if (tag.equals("PAUSE")) {
                        var pause = totalMap.get("PAUSE");
                        if (pause != null) {
                            pause += next.getValue();
                        } else {
                            pause = next.getValue();
                        }
                        totalMap.put(tag, pause);
                    } else {
                        totalMap.put(tag, next.getValue());
                    }
                }
            }
        }

        Assertions.assertEquals(4, meters.size());
        Assertions.assertEquals(totalMap.get("GROSS"), totalMap.get("NET") + totalMap.get("PAUSE"));
    }
}
