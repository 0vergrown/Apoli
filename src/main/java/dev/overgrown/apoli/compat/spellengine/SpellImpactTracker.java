package dev.overgrown.apoli.compat.spellengine;

import net.minecraft.core.Holder;
import net.spell_engine.api.spell.Spell;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public final class SpellImpactTracker {
    private SpellImpactTracker() {}

    private static final class Stack {
        @SuppressWarnings("unchecked")
        private Holder<Spell>[] spells = new Holder[4];
        private int size;
    }

    private static final ThreadLocal<Stack> STACK = ThreadLocal.withInitial(Stack::new);

    public static void push(Holder<Spell> spell) {
        Stack stack = STACK.get();
        if (stack.size == stack.spells.length) stack.spells = Arrays.copyOf(stack.spells, stack.size * 2);
        stack.spells[stack.size++] = spell;
    }

    public static void pop() {
        Stack stack = STACK.get();
        if (stack.size > 0) stack.spells[--stack.size] = null;
    }

    public static @Nullable Holder<Spell> current() {
        Stack stack = STACK.get();
        return stack.size == 0 ? null : stack.spells[stack.size - 1];
    }
}
