package org.mockserver.model;

import com.google.common.base.Joiner;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * @author jamesdbloom
 */
public class NottableString extends Not {

    private final String value;

    private NottableString(String value, Boolean not) {
        this.value = value;
        this.not = not;
    }

    public static NottableString deserializeNottableString(String string) {
        if (string.startsWith("!")) {
            return not(string.replaceFirst("^!", ""));
        } else {
            return string(string.replaceFirst("^!", ""));
        }
    }

    public static List<NottableString> deserializeNottableStrings(String... strings) {
        List<NottableString> nottableStrings = new LinkedList<>();
        for (String string : strings) {
            nottableStrings.add(string(string));
        }
        return nottableStrings;
    }

    public static List<NottableString> deserializeNottableStrings(List<String> strings) {
        List<NottableString> nottableStrings = new LinkedList<>();
        for (String string : strings) {
            nottableStrings.add(string(string));
        }
        return nottableStrings;
    }

    public static String serialiseNottableString(NottableString nottableString) {
        return (nottableString.isNot() ? "!" : "") + nottableString.value;
    }

    public static List<String> serialiseNottableString(List<NottableString> nottableStrings) {
        List<String> strings = new LinkedList<>();
        for (NottableString nottableString : nottableStrings) {
            strings.add(serialiseNottableString(nottableString));
        }
        return strings;
    }

    public static NottableString string(String value, Boolean not) {
        return new NottableString(value, not);
    }

    public static NottableString string(String value) {
        return new NottableString(value, false);
    }

    public static NottableString not(String value) {
        return new NottableString(value, Boolean.TRUE);
    }

    public static List<NottableString> strings(String... values) {
        return strings(Arrays.asList(values));
    }

    public static List<NottableString> strings(Collection<String> values) {
        List<NottableString> nottableValues = new ArrayList<NottableString>();
        if (values != null && !values.isEmpty()) {
            for (String value : values) {
                nottableValues.add(string(value));
            }
        }
        return nottableValues;
    }

    public String getValue() {
        return value;
    }

    public NottableString capitalize() {
        final String[] split = (value + "_").split("-");
        for (int i = 0; i < split.length; i++) {
            split[i] = StringUtils.capitalize(split[i]);
        }
        return new NottableString(StringUtils.substringBeforeLast(Joiner.on("-").join(split), "_"), not);
    }

    public NottableString lowercase() {
        return new NottableString(value.toLowerCase(), not);
    }

    public boolean equalsIgnoreCase(Object other) {
        return equals(other, true);
    }

    private boolean equals(Object other, boolean ignoreCase) {
        if (other instanceof String) {
            if (ignoreCase) {
                return isNot() != ((String) other).equalsIgnoreCase(value);
            } else {
                return isNot() != other.equals(value);
            }
        } else if (other instanceof NottableString) {
            NottableString otherNottableString = (NottableString) other;
            if (otherNottableString.getValue() == null) {
                return value == null;
            }
            if (ignoreCase) {
                return otherNottableString.isNot() == (isNot() == otherNottableString.getValue().equalsIgnoreCase(value));
            } else {
                return otherNottableString.isNot() == (isNot() == otherNottableString.getValue().equals(value));
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object other) {
        return equals(other, false);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, not);
    }
}
