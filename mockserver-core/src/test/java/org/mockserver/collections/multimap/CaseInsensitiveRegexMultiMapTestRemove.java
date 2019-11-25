package org.mockserver.collections.multimap;

import org.junit.Test;
import org.mockserver.collections.CaseInsensitiveRegexMultiMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.mockserver.collections.CaseInsensitiveRegexMultiMap.multiMap;
import static org.mockserver.model.NottableString.string;

/**
 * @author jamesdbloom
 */
public class CaseInsensitiveRegexMultiMapTestRemove {

    @Test
    public void shouldRemoveSingleValueEntry() {
        // given
        CaseInsensitiveRegexMultiMap multiMap = multiMap(
            true, new String[]{"keyOne", "keyOne_valueOne"},
                new String[]{"keyTwo", "keyTwo_valueOne", "keyTwo_valueTwo"},
                new String[]{"keyThree", "keyThree_valueOne", "keyThree_valueTwo", "keyThree_valueThree"}
        );

        // when
        assertThat(multiMap.remove("keyOne"), is(string("keyOne_valueOne")));

        // then
        assertThat(multiMap.size(), is(2));
        assertThat(multiMap.getAll("keyOne"), empty());
        assertThat(multiMap.getAll("keyTwo"), containsInAnyOrder(string("keyTwo_valueOne"), string("keyTwo_valueTwo")));
        assertThat(multiMap.getAll("keyThree"), containsInAnyOrder(string("keyThree_valueOne"), string("keyThree_valueTwo"), string("keyThree_valueThree")));
    }

    @Test
    public void shouldRemoveMultiValueEntry() {
        // given
        CaseInsensitiveRegexMultiMap multiMap = multiMap(
            true, new String[]{"keyOne", "keyOne_valueOne"},
                new String[]{"keyTwo", "keyTwo_valueOne", "keyTwo_valueTwo"},
                new String[]{"keyThree", "keyThree_valueOne", "keyThree_valueTwo", "keyThree_valueThree"}
        );

        // when
        assertThat(multiMap.remove("keyTwo"), is(string("keyTwo_valueOne")));

        // then
        assertThat(multiMap.size(), is(3));
        assertThat(multiMap.getAll("keyOne"), containsInAnyOrder(string("keyOne_valueOne")));
        assertThat(multiMap.getAll("keyTwo"), containsInAnyOrder(string("keyTwo_valueTwo")));
        assertThat(multiMap.getAll("keyThree"), containsInAnyOrder(string("keyThree_valueOne"), string("keyThree_valueTwo"), string("keyThree_valueThree")));
    }

    @Test
    public void shouldRemoveNoMatchingEntry() {
        // given
        CaseInsensitiveRegexMultiMap multiMap = multiMap(
            true, new String[]{"keyOne", "keyOne_valueOne"},
                new String[]{"keyTwo", "keyTwo_valueOne", "keyTwo_valueTwo"},
                new String[]{"keyThree", "keyThree_valueOne", "keyThree_valueTwo", "keyThree_valueThree"}
        );

        // when
        assertThat(multiMap.remove("keyFour"), is(nullValue()));

        // then
        assertThat(multiMap.size(), is(3));
        assertThat(multiMap.getAll("keyOne"), containsInAnyOrder(string("keyOne_valueOne")));
        assertThat(multiMap.getAll("keyTwo"), containsInAnyOrder(string("keyTwo_valueOne"), string("keyTwo_valueTwo")));
        assertThat(multiMap.getAll("keyThree"), containsInAnyOrder(string("keyThree_valueOne"), string("keyThree_valueTwo"), string("keyThree_valueThree")));
    }

    @Test
    public void shouldRemoveAllSingleValueEntry() {
        // given
        CaseInsensitiveRegexMultiMap multiMap = multiMap(
            true, new String[]{"keyOne", "keyOne_valueOne"},
                new String[]{"keyTwo", "keyTwo_valueOne", "keyTwo_valueTwo"},
                new String[]{"keyThree", "keyThree_valueOne", "keyThree_valueTwo", "keyThree_valueThree"}
        );

        // when
        assertThat(multiMap.removeAll("keyOne"), containsInAnyOrder(string("keyOne_valueOne")));

        // then
        assertThat(multiMap.size(), is(2));
        assertThat(multiMap.getAll("keyOne"), empty());
        assertThat(multiMap.getAll("keyTwo"), containsInAnyOrder(string("keyTwo_valueOne"), string("keyTwo_valueTwo")));
        assertThat(multiMap.getAll("keyThree"), containsInAnyOrder(string("keyThree_valueOne"), string("keyThree_valueTwo"), string("keyThree_valueThree")));
    }

    @Test
    public void shouldRemoveAllMultiValueEntry() {
        // given
        CaseInsensitiveRegexMultiMap multiMap = multiMap(
            true, new String[]{"keyOne", "keyOne_valueOne"},
                new String[]{"keyTwo", "keyTwo_valueOne", "keyTwo_valueTwo"},
                new String[]{"keyThree", "keyThree_valueOne", "keyThree_valueTwo", "keyThree_valueThree"}
        );

        // when
        assertThat(multiMap.removeAll("keyTwo"), containsInAnyOrder(string("keyTwo_valueOne"), string("keyTwo_valueTwo")));

        // then
        assertThat(multiMap.size(), is(2));
        assertThat(multiMap.getAll("keyOne"), containsInAnyOrder(string("keyOne_valueOne")));
        assertThat(multiMap.getAll("keyTwo"), empty());
        assertThat(multiMap.getAll("keyThree"), containsInAnyOrder(string("keyThree_valueOne"), string("keyThree_valueTwo"), string("keyThree_valueThree")));
    }

    @Test
    public void shouldRemoveAllNoMatchingEntry() {
        // given
        CaseInsensitiveRegexMultiMap multiMap = multiMap(
            true, new String[]{"keyOne", "keyOne_valueOne"},
                new String[]{"keyTwo", "keyTwo_valueOne", "keyTwo_valueTwo"},
                new String[]{"keyThree", "keyThree_valueOne", "keyThree_valueTwo", "keyThree_valueThree"}
        );

        // when
        assertThat(multiMap.removeAll("keyFour"), empty());

        // then
        assertThat(multiMap.size(), is(3));
        assertThat(multiMap.getAll("keyOne"), containsInAnyOrder(string("keyOne_valueOne")));
        assertThat(multiMap.getAll("keyTwo"), containsInAnyOrder(string("keyTwo_valueOne"), string("keyTwo_valueTwo")));
        assertThat(multiMap.getAll("keyThree"), containsInAnyOrder(string("keyThree_valueOne"), string("keyThree_valueTwo"), string("keyThree_valueThree")));
    }
}
