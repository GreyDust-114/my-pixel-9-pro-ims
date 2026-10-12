/*
 * my-pixel-9-pro-ims — Pixel 9 Pro VoNR / IMS carrier config overrides
 *
 * LSPosed (libxposed API 102) module. Scope: com.android.phone.
 *
 * Strategy ("read-side override", same approach as NKU100/carrier-ims, Apache-2.0):
 *   Intercept CarrierConfigLoader.getConfigForSubIdWithFeature and merge our
 *   overrides into the PersistableBundle it returns. Nothing is written to disk,
 *   so dumpsys carrier_config keeps showing the carrier's own values while every
 *   CarrierConfigManager caller sees the overridden ones.
 *
 * Stage C == carrier config only. No IMS provisioning write yet.
 */
package dev.greydust.ims;

import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.util.Log;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class VoNrModule extends XposedModule {

    private static final String TAG = "Pixel9ProIms";
    private static final String PHONE_PACKAGE = "com.android.phone";
    private static final String LOADER_CLASS = "com.android.phone.CarrierConfigLoader";

    @Override
    public void onPackageReady(PackageReadyParam param) {
        if (!PHONE_PACKAGE.equals(param.getPackageName())) {
            return;
        }

        Class<?> loader;
        try {
            loader = Class.forName(LOADER_CLASS, false, param.getClassLoader());
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "class not found: " + LOADER_CLASS, e);
            return;
        }

        Method target = findReader(loader, PersistableBundle.class,
                "getConfigForSubIdWithFeature", "getConfigForSubId");
        if (target == null) {
            Log.e(TAG, "no carrier config reader found. " + describe(loader));
            return;
        }

        PersistableBundle overrides = buildOverrides();

        hook(target)
                // Highest priority runs first in the chain, so our post-proceed()
                // mutation is the last one applied.
                .setPriority(XposedInterface.PRIORITY_HIGHEST)
                // This method is on the hottest path in the whole system; an
                // exception thrown here would take down telephony.
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object result = chain.proceed();
                    if (result instanceof PersistableBundle) {
                        try {
                            ((PersistableBundle) result).putAll(overrides);
                        } catch (Throwable t) {
                            Log.w(TAG, "failed to merge overrides", t);
                        }
                    }
                    return result;
                });

        Log.i(TAG, "hooked " + target);
    }

    private static PersistableBundle buildOverrides() {
        PersistableBundle b = new PersistableBundle();

        // --- Stage C target: let the IMS stack actually use NR for voice ---
        b.putBoolean(CarrierConfigManager.KEY_VONR_ENABLED_BOOL, true);
        b.putBoolean(CarrierConfigManager.KEY_VONR_SETTING_VISIBILITY_BOOL, true);

        // --- companion keys: keep VoLTE / VT / UT consistent so we don't
        //     introduce a half-enabled IMS profile ---
        b.putBoolean(CarrierConfigManager.KEY_CARRIER_VOLTE_AVAILABLE_BOOL, true);
        b.putBoolean(CarrierConfigManager.KEY_CARRIER_VT_AVAILABLE_BOOL, true);
        b.putBoolean(CarrierConfigManager.KEY_CARRIER_SUPPORTS_SS_OVER_UT_BOOL, true);

        // IMS registration state in status bar — useful for verifying the effect
        b.putBoolean(CarrierConfigManager.KEY_SHOW_IMS_REGISTRATION_STATUS_BOOL, true);

        return b;
    }

    /**
     * Match by name + return type and ignore the parameter list: Android keeps
     * adding parameters to these private methods, and a fixed signature would be
     * a time bomb. When several overloads match, take the one with the fewest
     * parameters so the result is deterministic.
     */
    private static Method findReader(Class<?> owner, Class<?> returnType, String... namesByPriority) {
        for (String name : namesByPriority) {
            Method best = null;
            for (Method m : owner.getDeclaredMethods()) {
                if (Modifier.isStatic(m.getModifiers())) continue;
                if (!name.equals(m.getName()) || m.getReturnType() != returnType) continue;
                if (best == null || m.getParameterCount() < best.getParameterCount()) best = m;
            }
            if (best != null) return best;
        }
        return null;
    }

    private static String describe(Class<?> owner) {
        StringBuilder sb = new StringBuilder(owner.getName()).append(" declares:");
        for (Method m : owner.getDeclaredMethods()) {
            sb.append("\n  ").append(m.getReturnType().getSimpleName())
                    .append(' ').append(m.getName()).append('(');
            Class<?>[] p = m.getParameterTypes();
            for (int i = 0; i < p.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(p[i].getSimpleName());
            }
            sb.append(')');
        }
        return sb.toString();
    }
}
