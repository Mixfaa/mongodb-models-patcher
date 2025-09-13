package com.mixfa.mongopatcher.processor;

import com.mixfa.mongopatcher.Patch;
import com.mixfa.mongopatcher.processor.annotations.FieldNameParam;
import com.mixfa.mongopatcher.processor.annotations.ParameterizeByParameterizedType;
import com.mixfa.mongopatcher.processor.annotations.TakeParameterizedType;
import com.squareup.javapoet.*;

import java.lang.reflect.Parameter;

interface ParameterTransformer {
    ParameterTransformingResult transform(MethodGenerationContext context, Parameter parameter, MethodSpec.Builder methodSpecBuilder);

    class PatchParameterTransformer implements ParameterTransformer {
        private final boolean skipParam;
        private static final PatchParameterTransformer SKIPPING = new PatchParameterTransformer(true);
        private static final PatchParameterTransformer FORWARDING = new PatchParameterTransformer(false);

        public static PatchParameterTransformer skipping() {
            return SKIPPING;
        }

        public static PatchParameterTransformer forwarding() {
            return FORWARDING;
        }

        private PatchParameterTransformer(boolean skipParam) {
            this.skipParam = skipParam;
        }

        @Override
        public ParameterTransformingResult transform(MethodGenerationContext context, Parameter parameter, MethodSpec.Builder methodSpecBuilder) {
            if (parameter.getType().equals(Patch.class)) {
                if (skipParam)
                    return ParameterTransformingResult.transformed("new Patch<>()");
                else {

                    methodSpecBuilder.addParameter(context.parameterizedPatchTypeName(), "patch");
                    return ParameterTransformingResult.transformed("patch");
                }
            }

            return ParameterTransformingResult.callNext();
        }
    }


    class FieldParameterTransformer implements ParameterTransformer {
        private final static FieldParameterTransformer INSTANCE = new FieldParameterTransformer();

        public static FieldParameterTransformer instance() {
            return INSTANCE;
        }

        private FieldParameterTransformer() {
        }

        @Override
        public ParameterTransformingResult transform(MethodGenerationContext context, Parameter parameter, MethodSpec.Builder methodSpecBuilder) {
            if (!parameter.isAnnotationPresent(FieldNameParam.class))
                return ParameterTransformingResult.callNext();

            var name = context.settings().findFirstByType(PatchClassMakingSetting.InnerFieldTweak.class)
                    .stream().map(tweak -> tweak.change(context.compName()))
                    .findFirst().orElse(context.compName());

            return ParameterTransformingResult.transformed('"' + name + '"');
        }
    }

    class TakeParameterizedTypeAnnotationTransformer implements ParameterTransformer {
        private final static TakeParameterizedTypeAnnotationTransformer INSTANCE = new TakeParameterizedTypeAnnotationTransformer();

        public static TakeParameterizedTypeAnnotationTransformer instance() {
            return INSTANCE;
        }

        private TakeParameterizedTypeAnnotationTransformer() {
        }

        @Override
        public ParameterTransformingResult transform(MethodGenerationContext context, Parameter parameter, MethodSpec.Builder methodSpecBuilder) {
            if (!parameter.isAnnotationPresent(TakeParameterizedType.class))
                return ParameterTransformingResult.callNext();

            methodSpecBuilder.addParameter(context.parameterizedType(), parameter.getName());
            return ParameterTransformingResult.transformed(parameter.getName());
        }
    }

    class ParameterizeByParameterizedTypeAnnotationTransformer implements ParameterTransformer {
        private final static ParameterizeByParameterizedTypeAnnotationTransformer INSTANCE = new ParameterizeByParameterizedTypeAnnotationTransformer();

        public static ParameterizeByParameterizedTypeAnnotationTransformer instance() {
            return INSTANCE;
        }

        private ParameterizeByParameterizedTypeAnnotationTransformer() {
        }

        @Override
        public ParameterTransformingResult transform(MethodGenerationContext context, Parameter parameter, MethodSpec.Builder methodSpecBuilder) {
            if (!parameter.isAnnotationPresent(ParameterizeByParameterizedType.class))
                return ParameterTransformingResult.callNext();

            var paramType = parameter.getType();
            var parameterType = context.parameterizedType();
            TypeName parameterizedParamType;

            if (paramType.isArray())
                parameterizedParamType = ArrayTypeName.of(parameterType);
            else
                parameterizedParamType = ParameterizedTypeName.get(ClassName.get(paramType), parameterType.box());

            methodSpecBuilder.addParameter(parameterizedParamType, parameter.getName());
            return ParameterTransformingResult.transformed(parameter.getName());
        }
    }

    class ObjectTypeParameterTransformer implements ParameterTransformer {
        private final static ObjectTypeParameterTransformer INSTANCE = new ObjectTypeParameterTransformer();

        public static ObjectTypeParameterTransformer instance() {
            return INSTANCE;
        }

        private ObjectTypeParameterTransformer() {
        }

        @Override
        public ParameterTransformingResult transform(MethodGenerationContext context, Parameter parameter, MethodSpec.Builder methodSpecBuilder) {
            if (!parameter.getType().equals(Object.class))
                return ParameterTransformingResult.callNext();

            methodSpecBuilder.addParameter(context.compType(), parameter.getName());
            return ParameterTransformingResult.transformed(parameter.getName());
        }
    }

    class DefaultParameterTransformer implements ParameterTransformer {
        private final static DefaultParameterTransformer INSTANCE = new DefaultParameterTransformer();

        public static DefaultParameterTransformer instance() {
            return INSTANCE;
        }

        private DefaultParameterTransformer() {
        }

        @Override
        public ParameterTransformingResult transform(MethodGenerationContext context, Parameter parameter, MethodSpec.Builder methodSpecBuilder) {
            methodSpecBuilder.addParameter(parameter.getType(), parameter.getName());
            return ParameterTransformingResult.transformed(parameter.getName());
        }
    }
}
