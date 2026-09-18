package dev.overgrown.apoli.condition.context;

public record StaticCtx() {
    public static final StaticCtx INSTANCE = new StaticCtx();
}
