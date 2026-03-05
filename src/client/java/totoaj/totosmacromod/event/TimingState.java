package totoaj.totosmacromod.event;

public class TimingState {
    public enum State {
        IDLE,
        USING,
        RESET,
    }

    private State state;

    public TimingState() {
        state = State.IDLE;
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

    public void setState(State s) {
        state = s;
    }

    public State get() {
        return state;
    }

    public boolean equals(State other) {
        return state == other;
    }
}