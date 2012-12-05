package net.md_5.analyst;

import java.util.HashMap;
import java.util.Map;

public class ReferenceData {

    public Map<Ownable, Integer> fields = new HashMap<>();
    public Map<Ownable, Integer> methods = new HashMap<>();

    public static <T> void add(Map<T, Integer> m, T entry) {
        add(m, entry, 1);
    }

    public static <T> void add(Map<T, Integer> m, T entry, int amount) {
        Integer val = m.get(entry);
        if (val == null) {
            val = 0;
        }
        val += amount;
        m.put(entry, val);
    }
}
