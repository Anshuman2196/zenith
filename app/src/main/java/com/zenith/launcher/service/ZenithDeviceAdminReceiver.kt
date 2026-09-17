package com.zenith.launcher.service
import android.app.admin.DeviceAdminReceiver


/**
 * A launcher can't lock the screen directly - [android.app.admin.DevicePolicyManager.lockNow]
 * is the only way, and that requires the app to be an active Device Admin. This receiver adds no
 * behaviour of its own (no callbacks overridden) beyond what [DeviceAdminReceiver] already does;
 * it exists purely so the system has a component to grant that admin role to. See
 * [com.zenith.launcher.util.SystemActionsHelper.lockScreen] for where it's actually used, and
 * `res/xml/device_admin_policies.xml` for the (empty) policy set requested - this app doesn't ask
 * for password rules, wipe access, or anything else admin apps commonly abuse.
 */
class ZenithDeviceAdminReceiver : DeviceAdminReceiver()
