package org.graph;

import org.onlab.util.Identifier;

/**
 * ACL rule identifier suitable as an external key.
 * <p>This class is immutable.</p>
 */
public final class RuleId extends Identifier<Long> {
    /**
     * Creates an ACL rule identifier from the specified long value.
     *
     * @param value long value
     * @return ACL rule identifier
     */
    public static RuleId valueOf(long value) {
        return new RuleId(value);
    }

    /**
     * Constructor for serializer.
     */
    RuleId() {
        super(0L);
    }

    /**
     * Constructs the ID corresponding to a given long value.
     *
     * @param value the underlying value of this ID
     */
    RuleId(long value) {
        super(value);
    }

    /**
     * Returns the backing value.
     *
     * @return the value
     */
    public long fingerprint() {
        return identifier;
    }

    @Override
    public String toString() {
        return "0x" + Long.toHexString(identifier);
    }
}