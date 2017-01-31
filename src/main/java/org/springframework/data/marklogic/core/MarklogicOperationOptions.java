package org.springframework.data.marklogic.core;

/**
 * --Description--
 *
 * @author Stéphane Toussaint
 */
public interface MarklogicOperationOptions {

    default String defaultCollection() {
        return null;
    }

    default boolean idInPropertyFragment() {
        return false;
    }

}
