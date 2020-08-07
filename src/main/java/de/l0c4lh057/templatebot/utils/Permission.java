package de.l0c4lh057.templatebot.utils;

import discord4j.rest.util.PermissionSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.util.annotation.NonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Permission {
	
	private static final Logger logger = LogManager.getLogger("Permissions");
	
	private static final Set<Permission> allPermissions = new HashSet<>();
	
	private final String permissionName;
	private final PermissionSet defaultPermissions;
	
	private Permission(@NonNull String permissionName, @NonNull PermissionSet defaultPermissions){
		this.permissionName = permissionName;
		this.defaultPermissions = defaultPermissions;
	}
	
	/**
	 * TODO add description
	 *
	 * @param permissionName     The permission name
	 * @param defaultPermissions The list of permissions you need to have this permission by default
	 * @return The {@link Permission} object
	 */
	@NonNull
	public static Permission of(@NonNull String permissionName, @NonNull discord4j.rest.util.Permission... defaultPermissions){
		return of(permissionName, PermissionSet.of(defaultPermissions));
	}
	
	/**
	 * TODO add description
	 *
	 * @param permissionName     The permission name
	 * @param defaultPermissions The set of permissions you need to have this permission by default
	 * @return The {@link Permission} object
	 */
	@NonNull
	public static Permission of(@NonNull String permissionName, @NonNull PermissionSet defaultPermissions){
		Permission perm = allPermissions.stream().filter(p -> p.permissionName.equals(permissionName)).findAny().orElse(null);
		if(perm != null){
			if(perm.defaultPermissions.equals(defaultPermissions)) return perm;
			logger.warn("Permission {} already exists with another list of default permissions.", permissionName);
		}
		Permission permission = new Permission(permissionName, defaultPermissions);
		allPermissions.add(permission);
		return permission;
	}
	
	/**
	 * @return The set of all {@link Permission}s instantiated before the call of this function
	 */
	@NonNull
	public static Set<Permission> getAllPermissions(){
		return Collections.unmodifiableSet(allPermissions);
	}
	
	/**
	 * @return The name of the permission used for database lookup
	 */
	@NonNull
	public String getPermissionName(){
		return permissionName;
	}
	
	/**
	 * @return The {@link PermissionSet} of all {@link discord4j.rest.util.Permission}s you need to have the permission
	 * by default
	 */
	@NonNull
	public PermissionSet getDefaultPermissions(){
		return defaultPermissions;
	}
	
	@Override
	public int hashCode() {
		return permissionName.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		return (obj instanceof Permission) && (((Permission)obj).permissionName.equals(permissionName));
	}
}
