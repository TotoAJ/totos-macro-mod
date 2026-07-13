package totoaj.totosmacromod.event;

public class TimingState {
    public enum State {
        IDLE,
        USING,
        RESET,
    }

    private State state;
    private int tickTimer;

    public TimingState() {
        state = State.IDLE;
        tickTimer = 0;
    }

    public void next() {
        if (state == State.IDLE) {
            state = State.USING;
        } else if (state == State.USING) {
            state = State.RESET;
        } else {
            state = State.IDLE;
        }
    }

    public void prev() {
        if (state == State.IDLE) {
            state = State.RESET;
        } else if (state == State.USING) {
            state = State.IDLE;
        } else {
            state = State.USING;
        }
    }

    public void tick() {
        tickTimer++;
    }

    public void resetTimer() {
        tickTimer = 0;
    }

    public int getTime() {
        return tickTimer;
    }

    public State getState() {
        return state;
    }

    public void advance() {
        next();
        resetTimer();
    }

    public boolean equals(TimingState other) {
        return tickTimer == other.tickTimer && state == other.state;
    }
}