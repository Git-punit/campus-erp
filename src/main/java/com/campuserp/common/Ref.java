package com.campuserp.common;

/** A short pointer to another record, used inside API responses. */
public record Ref(Long id, String code, String name) {
}
