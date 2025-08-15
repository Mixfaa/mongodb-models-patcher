package com.mixfa.mongopatcher.processor;

import java.lang.reflect.Method;

interface MethodNamingPolicy {
    String nameMethod(MethodGenerationContext context, Method method);

    class DefaultNamingPolicy implements MethodNamingPolicy {
        private DefaultNamingPolicy() {
        }

        private static final DefaultNamingPolicy INSTANCE = new DefaultNamingPolicy();

        public static DefaultNamingPolicy instance() {
            return INSTANCE;
        }

        @Override
        public String nameMethod(MethodGenerationContext context, Method method) {
            return method.getName();
        }
    }

    record PostfixNamingPolicy(String postfix) implements MethodNamingPolicy {
        private static final PostfixNamingPolicy EX_POSTFIX = new PostfixNamingPolicy("Ex");

        public static PostfixNamingPolicy exPostfix() {
            return EX_POSTFIX;
        }

        @Override
        public String nameMethod(MethodGenerationContext context, Method method) {
            return method.getName() + postfix;
        }
    }
}
