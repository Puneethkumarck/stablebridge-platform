package com.stablecoin.payments.fx.domain.statemachine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("StateMachine")
class StateMachineTest {

    private enum TestState { A, B, C }
    private enum TestTrigger { GO, BACK, FINISH }

    private final StateMachine<TestState, TestTrigger> stateMachine = new StateMachine<>(List.of(
            new StateTransition<>(TestState.A, TestTrigger.GO, TestState.B),
            new StateTransition<>(TestState.B, TestTrigger.FINISH, TestState.C),
            new StateTransition<>(TestState.B, TestTrigger.BACK, TestState.A)
    ));

    @Nested
    @DisplayName("transition()")
    class Transition {

        @Test
        @DisplayName("should return target state for valid transition")
        void should_returnTargetState_when_validTransition() {
            var result = stateMachine.transition(TestState.A, TestTrigger.GO);

            assertThat(result).isEqualTo(TestState.B);
        }

        @Test
        @DisplayName("should support multi-step transitions")
        void should_supportMultiStepTransitions_when_chainedTransitions() {
            var step1 = stateMachine.transition(TestState.A, TestTrigger.GO);
            var step2 = stateMachine.transition(step1, TestTrigger.FINISH);

            assertThat(step2).isEqualTo(TestState.C);
        }

        @Test
        @DisplayName("should support back transitions")
        void should_supportBackTransition_when_backTrigger() {
            var step1 = stateMachine.transition(TestState.A, TestTrigger.GO);
            var step2 = stateMachine.transition(step1, TestTrigger.BACK);

            assertThat(step2).isEqualTo(TestState.A);
        }

        @Test
        @DisplayName("should throw StateMachineException for invalid transition")
        void should_throwStateMachineException_when_invalidTransition() {
            assertThatThrownBy(() -> stateMachine.transition(TestState.A, TestTrigger.FINISH))
                    .isInstanceOf(StateMachineException.class)
                    .hasMessageContaining("Invalid transition")
                    .hasMessageContaining("A")
                    .hasMessageContaining("FINISH");
        }

        @Test
        @DisplayName("should throw StateMachineException when transitioning from terminal state")
        void should_throwStateMachineException_when_terminalState() {
            assertThatThrownBy(() -> stateMachine.transition(TestState.C, TestTrigger.GO))
                    .isInstanceOf(StateMachineException.class)
                    .hasMessageContaining("Invalid transition");
        }
    }

    @Nested
    @DisplayName("canTransition()")
    class CanTransition {

        @Test
        @DisplayName("should return true for valid transition")
        void should_returnTrue_when_validTransition() {
            assertThat(stateMachine.canTransition(TestState.A, TestTrigger.GO)).isTrue();
        }

        @Test
        @DisplayName("should return true for another valid transition")
        void should_returnTrue_when_anotherValidTransition() {
            assertThat(stateMachine.canTransition(TestState.B, TestTrigger.FINISH)).isTrue();
        }

        @Test
        @DisplayName("should return false for invalid transition")
        void should_returnFalse_when_invalidTransition() {
            assertThat(stateMachine.canTransition(TestState.A, TestTrigger.FINISH)).isFalse();
        }

        @Test
        @DisplayName("should return false for terminal state")
        void should_returnFalse_when_terminalState() {
            assertThat(stateMachine.canTransition(TestState.C, TestTrigger.GO)).isFalse();
        }
    }
}
