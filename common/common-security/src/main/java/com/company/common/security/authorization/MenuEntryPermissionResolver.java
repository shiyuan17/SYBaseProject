package com.company.common.security.authorization;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Resolves the single entry permission granted by a menu selection.
 * Menu grants decide page entry; explicit permissions decide in-page actions.
 */
public final class MenuEntryPermissionResolver {

    private static final Comparator<MenuPermissionBinding> ENTRY_PERMISSION_COMPARATOR =
        Comparator.comparingInt(MenuEntryPermissionResolver::priorityOf)
            .thenComparingInt(MenuPermissionBinding::sortOrder)
            .thenComparing(binding -> normalize(binding.permissionCode()));

    private static final Map<String, Integer> ACTION_PRIORITIES = Map.ofEntries(
        Map.entry("QUERY", 0),
        Map.entry("CREATE", 1),
        Map.entry("OPERATE", 2),
        Map.entry("REGISTER", 3),
        Map.entry("VERIFY", 4),
        Map.entry("HANDOVER", 5),
        Map.entry("RECEIVE", 6),
        Map.entry("ASSIGN", 7),
        Map.entry("ACCEPT", 8),
        Map.entry("START", 9),
        Map.entry("IMPORT", 10)
    );

    private MenuEntryPermissionResolver() {
    }

    public static Set<String> resolveEffectivePermissionCodes(
        Collection<String> explicitPermissionCodes,
        Collection<MenuPermissionBinding> menuPermissionBindings
    ) {
        Set<String> effectivePermissionCodes = new TreeSet<>();
        if (explicitPermissionCodes != null) {
            explicitPermissionCodes.stream()
                .filter(MenuEntryPermissionResolver::hasText)
                .forEach(effectivePermissionCodes::add);
        }

        resolveEntryPermissions(menuPermissionBindings).values().stream()
            .map(MenuPermissionBinding::permissionCode)
            .filter(MenuEntryPermissionResolver::hasText)
            .forEach(effectivePermissionCodes::add);
        return effectivePermissionCodes;
    }

    public static Set<String> resolveEntryPermissionIds(
        Collection<MenuPermissionBinding> menuPermissionBindings
    ) {
        Set<String> entryPermissionIds = new LinkedHashSet<>();
        resolveEntryPermissions(menuPermissionBindings).values().stream()
            .map(MenuPermissionBinding::permissionId)
            .filter(MenuEntryPermissionResolver::hasText)
            .forEach(entryPermissionIds::add);
        return entryPermissionIds;
    }

    public static Map<String, MenuPermissionBinding> resolveEntryPermissions(
        Collection<MenuPermissionBinding> menuPermissionBindings
    ) {
        Map<String, MenuPermissionBinding> entryPermissionsByMenuId = new LinkedHashMap<>();
        if (menuPermissionBindings == null) {
            return entryPermissionsByMenuId;
        }

        menuPermissionBindings.stream()
            .filter(Objects::nonNull)
            .filter(MenuEntryPermissionResolver::isEligibleEntryPermission)
            .forEach(binding -> entryPermissionsByMenuId.merge(
                binding.menuId(),
                binding,
                (current, candidate) ->
                    ENTRY_PERMISSION_COMPARATOR.compare(candidate, current) < 0 ? candidate : current
            ));
        return entryPermissionsByMenuId;
    }

    private static boolean isEligibleEntryPermission(MenuPermissionBinding binding) {
        return hasText(binding.menuId())
            && hasText(binding.permissionCode())
            && hasText(binding.actionKey())
            && binding.menuEnabled()
            && binding.permissionEnabled()
            && "MENU".equals(normalize(binding.menuType()));
    }

    private static int priorityOf(MenuPermissionBinding binding) {
        return ACTION_PRIORITIES.getOrDefault(normalize(binding.actionKey()), Integer.MAX_VALUE);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    public record MenuPermissionBinding(
        String menuId,
        String menuType,
        String permissionId,
        String permissionCode,
        String actionKey,
        int sortOrder,
        boolean menuEnabled,
        boolean permissionEnabled
    ) {
    }
}
