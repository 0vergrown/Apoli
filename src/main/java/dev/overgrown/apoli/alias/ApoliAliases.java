package dev.overgrown.apoli.alias;

/**
 * Static bootstrap hook for any global, cross-registry aliases an Apoli
 * implementation might need to install before type registrations run.
 *
 * <p>The built-in Apace 1.20.1 → 1.21.1 renames live on the appropriate
 * registry instances (e.g. condition {@code apoli:and} → {@code apoli:all_of}
 * is registered through {@code AliasingOptions} on the entity-condition
 * registration in {@code MetaConditions}). That keeps the rename scoped to
 * meta conditions only — it must NOT bleed into the action registry, where
 * {@code apoli:and} is the canonical id of the {@code AndMetaAction}. So
 * this method is intentionally empty.</p>
 */
public final class ApoliAliases {
    private ApoliAliases() {}

    public static void bootstrap() {
        // Intentionally empty — see class javadoc.
    }
}
