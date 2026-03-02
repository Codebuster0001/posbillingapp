import { useSelector } from 'react-redux';

/**
 * PermissionGuard component to conditionally render children based on user permissions.
 * 
 * @param {Object} props
 * @param {string|string[]} props.permission - Required permission key or array of keys.
 * @param {boolean} props.requireAll - Whether all permissions in the array are required (default: false).
 * @param {import('react').ReactNode} props.children - Content to render if permission is granted.
 * @param {import('react').ReactNode} props.fallback - Optional content to render if permission is denied.
 */
const PermissionGuard = ({ permission, requireAll = false, children, fallback = null }) => {
    const { permissions, role } = useSelector((state) => state.auth);

    // Admin always has all permissions
    if (role === 'Admin') return children;

    if (!permission) return children;

    const requiredPermissions = Array.isArray(permission) ? permission : [permission];

    const hasPermission = requireAll
        ? requiredPermissions.every(p => permissions.includes(p))
        : requiredPermissions.some(p => permissions.includes(p));

    if (hasPermission) {
        return children;
    }

    return fallback;
};

export default PermissionGuard;
