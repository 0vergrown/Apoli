package dev.overgrown.apoli.compat.lambdynlights;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.power.builtin.EmissivePower;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public final class LambDynamicLightsCompat {

    private static final String HANDLER = "dev.lambdaurora.lambdynlights.api.DynamicLightHandler";
    private static final String HANDLERS = "dev.lambdaurora.lambdynlights.api.DynamicLightHandlers";

    private LambDynamicLightsCompat() {}

    public static void init() {
        try {
            ClassLoader loader = LambDynamicLightsCompat.class.getClassLoader();
            Class<?> handlerClass = Class.forName(HANDLER, false, loader);
            Class<?> handlersClass = Class.forName(HANDLERS, false, loader);
            Method register = handlersClass.getMethod("registerDynamicLightHandler", EntityType.class, handlerClass);

            Object handler = Proxy.newProxyInstance(loader, new Class<?>[]{handlerClass}, new Bridge());

            int count = 0;
            for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
                register.invoke(null, type, handler);
                count++;
            }
            Apoli.LOGGER.info("[Apoli] LambDynamicLights detected — apoli:emissive wired to {} entity type(s).", count);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            Apoli.LOGGER.warn("[Apoli] LambDynamicLights is present but its dynamic light handler API is not"
                + " (looked for {}). apoli:emissive will still light the entity's own model, but will not"
                + " emit light into the world.", HANDLERS);
        } catch (Exception e) {
            Apoli.LOGGER.error("[Apoli] Failed to register the LambDynamicLights bridge for apoli:emissive", e);
        }
    }

    private static final class Bridge implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "getLuminance" -> EmissivePower.luminanceOf((Entity) args[0]);
                case "isWaterSensitive" -> EmissivePower.isWaterSensitive((Entity) args[0]);
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                case "toString" -> "ApoliEmissiveDynamicLightHandler";
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }
    }
}
