package com.mixfa.mongopatcher.processor;

import com.mixfa.mongopatcher.Patch;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.Element;

public record MethodGenerationContext(
        String compName,
        TypeName compType,
        TypeName parameterizedType,
        TypeName parameterizedPatchTypeName,
        PatchClassMakingSettings settings
) {

    public MethodGenerationContext(
            String compName,
            TypeName compType,
            TypeName parameterizedType,
            Element originalClass,
            PatchClassMakingSettings settings) {
        this(
                compName,
                compType,
                parameterizedType,
                ParameterizedTypeName.get(ClassName.get(Patch.class), TypeName.get(
                        settings.findFirstByType(PatchClassMakingSetting.ReplaceOriginalClass.class)
                                .map(PatchClassMakingSetting.ReplaceOriginalClass::originalClass)
                                .orElse(originalClass).asType())),
                settings);
    }

}
