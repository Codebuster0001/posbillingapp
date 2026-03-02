package com.posbillingapp.models;
import java.util.List;
import com.google.gson.annotations.SerializedName;

public class PermissionModels {
    public static class Role {
        @SerializedName("id") public int id;
        @SerializedName("roleName") public String roleName;
        @Override public String toString() { return roleName; }
    }

    public static class Permission {
        @SerializedName("id") public int id;
        @SerializedName("key") public String permissionKey;
        @SerializedName("description") public String description;
    }

    public static class UpdateRolePermissionsRequest {
        @SerializedName("roleId") public int roleId;
        @SerializedName("companyId") public long companyId;
        @SerializedName("permissionKeys") public List<String> permissionKeys;

        public UpdateRolePermissionsRequest(int roleId, long companyId, List<String> permissionKeys) {
            this.roleId = roleId;
            this.companyId = companyId;
            this.permissionKeys = permissionKeys;
        }
    }
}
