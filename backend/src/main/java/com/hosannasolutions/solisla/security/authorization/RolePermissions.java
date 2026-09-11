package com.hosannasolutions.solisla.security.authorization;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Static role-to-permission map for the MVP, mirroring SweetHome's approach (a single-business
 * storefront needs no dynamic permission editor): the authoritative grant list lives in code,
 * not in editable tables. Revisit only once a second admin with a narrower role actually exists.
 */
public final class RolePermissions {

    private static final Map<Role, Set<Permission>> GRANTS = buildGrants();

    private RolePermissions() {
    }

    public static Set<Permission> permissionsFor(Role role) {
        return GRANTS.getOrDefault(role, Set.of());
    }

    private static Map<Role, Set<Permission>> buildGrants() {
        Map<Role, Set<Permission>> grants = new EnumMap<>(Role.class);

        grants.put(Role.ADMIN, EnumSet.allOf(Permission.class));

        grants.put(Role.STAFF, EnumSet.of(
                Permission.PRODUCT_VIEW,
                Permission.CATEGORY_VIEW,
                Permission.INVENTORY_VIEW, Permission.INVENTORY_ADJUST,
                Permission.ORDER_VIEW, Permission.ORDER_UPDATE_STATUS,
                Permission.PAYMENT_VIEW,
                Permission.DELIVERY_MANAGE
        ));

        return Collections.unmodifiableMap(grants);
    }
}
