package dev.overgrown.apoli.tick;

public final class TickState {
    public static final int INHERIT = -1;
    public static final long PERMANENT = -1L;

    private int rate;
    private boolean frozen;
    private boolean exempt;
    private int stepTicks;
    private int sprintTicks;
    private long expiresAtTick;

    public TickState() {
        this.rate = INHERIT;
        this.expiresAtTick = PERMANENT;
    }

    public int rate() {
        return rate;
    }

    public boolean frozen() {
        return frozen;
    }

    public boolean exempt() {
        return exempt;
    }

    public void setExempt(boolean exempt) {
        this.exempt = exempt;
    }

    public int stepTicks() {
        return stepTicks;
    }

    public int sprintTicks() {
        return sprintTicks;
    }

    public void setRate(int rate) {
        this.rate = rate < 0 ? INHERIT : rate;
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
        if (frozen) this.sprintTicks = 0;
    }

    public void setStepTicks(int ticks) {
        this.stepTicks = Math.max(0, ticks);
    }

    public void setSprintTicks(int ticks) {
        this.sprintTicks = Math.max(0, ticks);
        if (this.sprintTicks > 0) this.stepTicks = 0;
    }

    public void keepUntil(long tick) {
        if (tick == PERMANENT || this.expiresAtTick == PERMANENT) {
            this.expiresAtTick = tick;
        } else if (tick > this.expiresAtTick) {
            this.expiresAtTick = tick;
        }
    }

    public void setExpiry(long tick) {
        this.expiresAtTick = tick;
    }

    public boolean expired(long tick) {
        return expiresAtTick != PERMANENT && tick >= expiresAtTick;
    }

    public boolean isDefault() {
        return rate == INHERIT && !frozen && stepTicks == 0 && sprintTicks == 0 && !exempt;
    }

    public void consumeStep() {
        if (stepTicks > 0) stepTicks--;
    }

    public void consumeSprint() {
        if (sprintTicks > 0) sprintTicks--;
    }

    public int effectiveRate(int base) {
        if (sprintTicks > 0) return base;
        if (frozen) return stepTicks > 0 ? base : 0;
        return rate == INHERIT ? base : Math.min(rate, base);
    }

    public void countDown() {
        if (sprintTicks > 0) sprintTicks--;
        else if (frozen && stepTicks > 0) stepTicks--;
    }
}
