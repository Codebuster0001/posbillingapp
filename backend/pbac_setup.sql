-- PBAC Setup Script
-- Run this in your MySQL database (pos_db2) to ensure tables and permissions are correctly initialized.

-- 1. Create permissions table
CREATE TABLE IF NOT EXISTS permissions (
    Id INT NOT NULL AUTO_INCREMENT,
    PermissionKey VARCHAR(100) NOT NULL,
    Description VARCHAR(255),
    PRIMARY KEY (Id),
    UNIQUE KEY (PermissionKey)
) ENGINE=InnoDB;

-- 2. Create role_permissions table
CREATE TABLE IF NOT EXISTS role_permissions (
    RoleId INT NOT NULL,
    PermissionId INT NOT NULL,
    CompanyId BIGINT NOT NULL,
    PRIMARY KEY (RoleId, PermissionId, CompanyId),
    KEY fk_perm_role (RoleId),
    KEY fk_perm_permission (PermissionId),
    KEY fk_perm_company (CompanyId)
) ENGINE=InnoDB;

-- 3. Seed Master Permissions
INSERT IGNORE INTO permissions (PermissionKey, Description) VALUES 
('VIEW_BILLING', 'Ability to access and process billing/POS.'),
('VIEW_EMPLOYEES', 'Ability to view and manage staff details.'),
('MANAGE_EMPLOYEES', 'Full control over employee accounts.'),
('VIEW_PRODUCTS', 'Ability to view inventory and menu items.'),
('MANAGE_PRODUCTS', 'Ability to add, edit or delete products.'),
('VIEW_REPORTS', 'Access to sales and performance reports.');

-- 4. Map 'Full Access' (RoleId 1) to ALL permissions for ALL companies
INSERT IGNORE INTO role_permissions (RoleId, PermissionId, CompanyId)
SELECT 1, p.Id, c.Id 
FROM permissions p, companies c;

-- 5. Map 'Limited Access' (RoleId 2) to 'VIEW_BILLING' only for ALL companies
INSERT IGNORE INTO role_permissions (RoleId, PermissionId, CompanyId)
SELECT 2, p.Id, c.Id 
FROM permissions p, companies c
WHERE p.PermissionKey = 'VIEW_BILLING';

-- 6. Verification: List current mappings (optional)
-- SELECT r.RoleName, p.PermissionKey, c.CompanyName 
-- FROM role_permissions rp 
-- JOIN permissions p ON rp.PermissionId = p.Id 
-- JOIN role r ON rp.RoleId = r.Id 
-- JOIN companies c ON rp.CompanyId = c.Id;
