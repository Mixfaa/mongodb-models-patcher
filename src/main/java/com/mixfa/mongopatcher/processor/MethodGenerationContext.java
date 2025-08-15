package com.mixfa.mongopatcher.processor;

import com.mixfa.mongopatcher.Patch;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.TypeElement;

public record MethodGenerationContext(String compName, TypeName compType, TypeName parameterizedType,
                               TypeElement originalClass, TypeName parameterizedPatchTypeName) {

    public MethodGenerationContext(String compName, TypeName compType, TypeName parameterizedType, TypeElement originalClass) {
        this(compName, compType, parameterizedType, originalClass, ParameterizedTypeName.get(ClassName.get(Patch.class), TypeName.get(originalClass.asType())));
    }

}
