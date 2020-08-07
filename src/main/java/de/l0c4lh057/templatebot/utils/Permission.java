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
	
	private Permission(@NonNull String permissionName, @NonNull discord4j.rest.util.Permission... defaultPermissions){
		this.permissionName = permissionName;
		this.defaultPermissions = PermissionSet.of(defaultPermissions);
	}
	
	public static Permission of(@NonNull String permissionName, @NonNull discord4j.rest.util.Permission... defaultPermissions){
		Permission permission = new Permission(permissionName, defaultPermissions);
		if(allPermissions.stream().anyMatch(p -> p.permissionName.equals(permissionName) && p.defaultPermissions.equals(PermissionSet.of(defaultPermissions))))
			logger.warn("Permission {} already exists with another list of default permissions.", permissionName);
		allPermissions.add(permission);
		return permission;
	}
	
	/**
	 *
	 * @return
	 */
	public static Set<Permission> getAllPermissions(){
		return Collections.unmodifiableSet(allPermissions);
	}
	
	/**
	 * @return
	 */
	@NonNull
	public String getPermissionName(){
		return permissionName;
	}
	
	/**
	 * @return
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
