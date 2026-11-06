package com.campuserp.user;

public enum Role {
    /** Everything, including users and expense approval. */
    ADMIN,
    /** Create, edit and delete records in every module; raise expenses. */
    STAFF,
    /** Read only. */
    VIEWER
}
