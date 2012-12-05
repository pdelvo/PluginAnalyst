package net.md_5.analyst;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * <pre>
 * This file is part of GoofAround.
 *
 * GoofAround is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GoofAround is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GoofAround. If not, see < http://www.gnu.org/licenses/ >.
 * </pre>
 *
 * @author Wolvereness
 */
public class MapSorter {

    private static final Comparator COMPARATOR = new Comparator<Map.Entry<?, Comparable>>() {
        @Override
        public int compare(final Entry<?, Comparable> o1, final Entry<?, Comparable> o2) {
            final Comparable v1 = o1.getValue();
            final Comparable v2 = o2.getValue();
            if (v1 == v2) {
                return 0;
            }
            if (v2 == null) {
                return -v1.compareTo(v2);
            }
            return v2.compareTo(v1);
        }
    };

    public static <K, V extends Comparable<? super V>> LinkedHashMap<K, V> valueSortedMap(final Map<? extends K, ? extends V> old) {
        return valueSortedMap(new LinkedHashMap<K, V>(), old);
    }

    public static <K, V extends Comparable<? super V>, R extends Map<K, V>> R valueSortedMap(final R out, final Map<? extends K, ? extends V> old) {
        final List<Map.Entry<? extends K, ? extends V>> entries = new ArrayList<Map.Entry<? extends K, ? extends V>>(old.entrySet());
        Collections.sort(entries, MapSorter.<K, V>comparator());
        return build(entries, out);
    }

    public static <K, V> LinkedHashMap<K, V> valueSortedMap(final Map<? extends K, ? extends V> old, final Comparator<? super V> comparator) {
        return valueSortedMap(new LinkedHashMap<K, V>(), old, comparator);
    }

    public static <K, V, R extends Map<K, V>> R valueSortedMap(final R out, final Map<? extends K, ? extends V> old, final Comparator<? super V> comparator) {
        final List<Map.Entry<? extends K, ? extends V>> entries = new ArrayList<Map.Entry<? extends K, ? extends V>>(old.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<? extends K, ? extends V>>() {
            @Override
            public int compare(Entry<? extends K, ? extends V> o1, Entry<? extends K, ? extends V> o2) {
                return comparator.compare(o1.getValue(), o2.getValue());
            }
        });
        return build(entries, out);
    }

    private static <K, V, M extends Map<K, V>> M build(final List<Map.Entry<? extends K, ? extends V>> sortedEntries, final M out) {
        for (Map.Entry<? extends K, ? extends V> entry : sortedEntries) {
            out.put(entry.getKey(), entry.getValue());
        }
        return out;
    }

    private static <K, T extends Comparable<? super T>> Comparator<Map.Entry<? extends K, ? extends T>> comparator() {
        return COMPARATOR;
    }
}
