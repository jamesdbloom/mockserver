
package org.mockserver.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Joiner;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.regex.Pattern;

/**
 * @author jamesdbloom
 */
public class NottableString extends ObjectWithJsonToString implements Comparable<NottableString> {

    private static final char NOT_CHAR = '!';
    private final String value;
    private final boolean isBlank;
    private final Boolean not;
    private final int hashCode;
    private final String json;
    private Pattern pattern;
    private Pattern lowercasePattern;

    private NottableString(String value, Boolean not) {
        this.value = value;
        this.isBlank = StringUtils.isBlank(value);
        if (not != null) {
            this.not = not;
        } else {
            this.not = Boolean.FALSE;
        }
        this.hashCode = Objects.hash(this.value, this.not);
        if (this.not) {
            this.json = NOT_CHAR + this.value;
        } else {
            this.json = this.value;
        }
    }

    private NottableString(String value) {
        this.isBlank = StringUtils.isBlank(value);
        if (!this.isBlank && value.charAt(0) == NOT_CHAR) {
            this.value = value.substring(1);
            this.not = Boolean.TRUE;
        } else {
            this.value = value;
            this.not = Boolean.FALSE;
        }
        this.hashCode = Objects.hash(this.value, this.not);
        if (this.not) {
            this.json = NOT_CHAR + this.value;
        } else {
            this.json = this.value;
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
        return nottableString.toString();
    }

    public static List<String> serialiseNottableString(List<NottableString> nottableStrings) {
        List<String> strings = new LinkedList<>();
        for (NottableString nottableString : nottableStrings) {
            strings.add(nottableString.toString());
        }
        return strings;
    }

    public static NottableString string(String value, Boolean not) {
        return new NottableString(value, not);
    }

    public static NottableString string(String value) {
        return new NottableString(value);
    }

    public static NottableString not(String value) {
        return new NottableString(value, Boolean.TRUE);
    }

    public static List<NottableString> strings(String... values) {
        List<NottableString> nottableValues = new ArrayList<>();
        if (values != null) {
            for (String value : values) {
                nottableValues.add(string(value));
            }
        }
        return nottableValues;
    }

    public static List<NottableString> strings(Collection<String> values) {
        List<NottableString> nottableValues = new ArrayList<>();
        if (values != null) {
            for (String value : values) {
                nottableValues.add(string(value));
            }
        }
        return nottableValues;
    }

    public String getValue() {
        return value;
    }

    @JsonIgnore
    public boolean isNot() {
        return not;
    }

    NottableString capitalize() {
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
                return not != ((String) other).equalsIgnoreCase(value);
            } else {
                return not != other.equals(value);
            }
        } else if (other instanceof NottableString) {
            NottableString that = (NottableString) other;
            if (that.getValue() == null) {
                return value == null;
            }
            boolean reverse = (that.not != this.not) && (that.not || this.not);
            if (ignoreCase) {
                return reverse != that.getValue().equalsIgnoreCase(value);
            } else {
                return reverse != that.getValue().equals(value);
            }
        }
        return false;
    }

    public boolean isBlank() {
        return isBlank;
    }

    public boolean matches(String input) {
        if (pattern == null) {
            pattern = Pattern.compile(getValue());
        }
        return pattern.matcher(input).matches();
    }

    public boolean matchesIgnoreCase(String input) {
        if (lowercasePattern == null) {
            lowercasePattern = Pattern.compile(getValue().toLowerCase());
        }
        return lowercasePattern.matcher(input.toLowerCase()).matches();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof String) {
            return not != other.equals(value);
        } else if (other instanceof NottableString) {
            NottableString that = (NottableString) other;
            if (that.getValue() == null) {
                return this.value == null;
            }
            boolean reverse = (that.not != this.not) && (that.not || this.not);
            return reverse != that.getValue().equals(this.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return json;
    }

    @Override
    public int compareTo(NottableString other) {
        return other.getValue().compareTo(this.getValue());
    }
}
