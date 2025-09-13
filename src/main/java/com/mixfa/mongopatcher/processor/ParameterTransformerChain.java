package com.mixfa.mongopatcher.processor;

import com.squareup.javapoet.MethodSpec;

import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Optional;

public record ParameterTransformerChain(List<ParameterTransformer> parameterProcessors) {
    Optional<String> transformParameter(MethodGenerationContext context, Parameter parameter, MethodSpec.Builder methodSpecBuilder) {
        for (var processor : parameterProcessors) {
            var result = processor.transform(context, parameter, methodSpecBuilder);

            if (result instanceof ParameterTransformingResult.Transformed(String transformedTo))
                return Optional.ofNullable(transformedTo);
            else if (result instanceof ParameterTransformingResult.Skipped)
                break;
            else if (result instanceof ParameterTransformingResult.CallNext)
                continue;
        }

        return Optional.empty();
    }
}
