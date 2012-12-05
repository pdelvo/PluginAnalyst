package net.md_5.analyst;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

public class IntegerComparator<K> implements Comparator<K> {

    private final Map<K, ? extends Number> toSort;

    public IntegerComparator(Map<K, ? extends Number> toSort) {
        this.toSort = toSort;
    }

    @Override
    public int compare(K key1, K key2) {
        return toSort.get(key1).longValue() < toSort.get(key2).longValue() ? 1 : -1;
    }

    public static <K, V extends Number> Map<K, V> sort(Map<K, V> input) {
        TreeMap<K, V> tree = new TreeMap<>(new IntegerComparator<>(input));
        tree.putAll(input);
        return tree;
    }
}
