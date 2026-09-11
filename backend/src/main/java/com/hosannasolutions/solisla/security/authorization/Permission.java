package com.hosannasolutions.solisla.security.authorization;

public enum Permission {
    PRODUCT_VIEW,
    PRODUCT_CREATE,
    PRODUCT_UPDATE,
    PRODUCT_ARCHIVE,

    CATEGORY_VIEW,
    CATEGORY_MANAGE,

    INVENTORY_VIEW,
    INVENTORY_ADJUST,

    ORDER_VIEW,
    ORDER_UPDATE_STATUS,
    ORDER_CANCEL,

    PAYMENT_VIEW,

    DELIVERY_MANAGE,

    REPORT_VIEW,

    CONFIGURATION_MANAGE,

    AUDIT_VIEW,

    USER_MANAGE
}
