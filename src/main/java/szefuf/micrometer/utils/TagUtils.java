package szefuf.micrometer.utils;

import io.micrometer.core.instrument.util.StringUtils;
import szefuf.micrometer.TimerType;

import java.util.ArrayList;
import java.util.Arrays;

public class TagUtils {

    //TODO: add function checking for duplicates

    public static String[] addTypeTag(String[] tags, TimerType type) {
        return addTypeTag(tags, type, null);
    }

    public static String[] addTypeTag(String[] tags, TimerType type, String pauseTagName) {
        var list = new ArrayList<>(Arrays.asList(tags));
        list.add("timer.type");
        list.add(type.name());
        if (StringUtils.isNotBlank(pauseTagName)) {
            list.add("timer.pause.reason");
            list.add(pauseTagName);
        }
        return list.toArray(new String[list.size()]);
    }

}
