package org.mockserver.collections.multimap;

import org.junit.Test;
import org.mockserver.collections.CaseInsensitiveRegexMultiMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.mockserver.collections.CaseInsensitiveRegexMultiMap.entry;
import static org.mockserver.collections.CaseInsensitiveRegexMultiMap.multiMap;
import static org.mockserver.model.NottableString.string;

/**
 * @author jamesdbloom
 */
public class CaseInsensitiveRegexMultiMapTestKeysAndValue {

    @Test
    public void shouldReturnKeys() {
        // given
        CaseInsensitiveRegexMultiMap multiMap = multiMap(
            true, new String[]{"keyOne", "keyOne_valueOne"},
                new String[]{"keyTwo", "keyTwo_valueOne", "keyTwo_valueTwo"}
        );

        // then
        assertThat(multiMap.keySet(), containsInAnyOrder(string("keyOne"), string("keyTwo")));
    }

    @Test
    public void shouldReturnValues() {
        // given
        CaseInsensitiveRegexMultiMap multiMap = multiMap(
            true, new String[]{"keyOne", "keyOne_valueOne"},
                new String[]{"keyTwo", "keyTwo_valueOne", "keyTwo_valueTwo"}
        );

        // then
        assertThat(multiMap.values(), containsInAnyOrder(string("keyOne_valueOne"), string("keyTwo_valueOne"), string("keyTwo_valueTwo")));
    }

    @Test
    public void shouldReturnEntrySet() {
        // given
        CaseInsensitiveRegexMultiMap multiMap = multiMap(
            true, new String[]{"keyOne", "keyOne_valueOne"},
                new String[]{"keyTwo", "keyTwo_valueOne", "keyTwo_valueOne", "keyTwo_valueTwo"}
        );

        // then
        assertThat(multiMap.entrySet(), containsInAnyOrder(
                entry("keyOne", "keyOne_valueOne"),
                entry("keyTwo", "keyTwo_valueOne"),
                entry("keyTwo", "keyTwo_valueTwo")
        ));
    }

    @Test
    public void shouldReturnEntryList() {
        // given
        CaseInsensitiveRegexMultiMap multiMap = multiMap(
            true, new String[]{"keyOne", "keyOne_valueOne"},
                new String[]{"keyTwo", "keyTwo_valueOne", "keyTwo_valueOne", "keyTwo_valueTwo"}
        );

        // then
        assertThat(multiMap.entryList(), containsInAnyOrder(
                entry("keyOne", "keyOne_valueOne"),
                entry("keyTwo", "keyTwo_valueOne"),
                entry("keyTwo", "keyTwo_valueOne"),
                entry("keyTwo", "keyTwo_valueTwo")
        ));
    }
}
