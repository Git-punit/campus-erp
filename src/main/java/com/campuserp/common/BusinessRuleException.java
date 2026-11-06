package com.campuserp.common;

/**
 * Thrown when a request is well formed but breaks a rule, e.g. deleting the
 * last admin or marking a training "conducted" before it was accepted.
 * Sent back as 409.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
