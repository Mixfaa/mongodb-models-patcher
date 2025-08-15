package com.mixfa.mongopatcher.processor;

public sealed interface ParameterTransformingResult {
    Skipped SKIPPED = new Skipped();
    CallNext CALL_NEXT = new CallNext();

    static Skipped skipped() {
        return SKIPPED;
    }

    static CallNext callNext() {
        return CALL_NEXT;
    }

    static Transformed transformed(String transformedTo) {
        return new Transformed(transformedTo);
    }

    final class Skipped implements ParameterTransformingResult {
        private Skipped() {
        }
    }

    final class CallNext implements ParameterTransformingResult {
        private CallNext() {
        }
    }

    record Transformed(String transformedTo) implements ParameterTransformingResult {
    }
}