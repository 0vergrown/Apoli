package dev.overgrown.apoli.compat.icarus;

import dev.cammiescorner.icarus.api.IcarusPlayerValues;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public final class WingsValues {
    private WingsValues() {}

    public static IcarusPlayerValues of(WingsPower.Config cfg, IcarusPlayerValues fallback) {
        return (IcarusPlayerValues) Proxy.newProxyInstance(
            IcarusPlayerValues.class.getClassLoader(),
            new Class<?>[]{IcarusPlayerValues.class},
            new Handler(cfg, fallback));
    }

    private record Handler(WingsPower.Config cfg, IcarusPlayerValues fallback) implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            switch (method.getName()) {
                case "wingsSpeed" -> { if (cfg.wingsSpeed().isPresent()) return (float) cfg.wingsSpeed().getAsDouble(); }
                case "maxSlowedMultiplier" -> { if (cfg.maxSlowedMultiplier().isPresent()) return (float) cfg.maxSlowedMultiplier().getAsDouble(); }
                case "armorSlows" -> { if (cfg.armorSlows().isPresent()) return cfg.armorSlows().get(); }
                case "exhaustionAmount" -> { if (cfg.exhaustionAmount().isPresent()) return (float) cfg.exhaustionAmount().getAsDouble(); }
                case "maxHeightAboveWorld" -> { if (cfg.maxHeightAboveWorld().isPresent()) return cfg.maxHeightAboveWorld().getAsInt(); }
                case "maxHeightEnabled" -> { if (cfg.maxHeightAboveWorld().isPresent()) return true; }
                default -> { }
            }
            try {
                return method.invoke(fallback, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }
}
