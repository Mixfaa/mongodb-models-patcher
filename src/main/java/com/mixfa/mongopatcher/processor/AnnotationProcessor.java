package com.mixfa.mongopatcher.processor;

import com.google.auto.service.AutoService;
import com.mixfa.mongopatcher.MongoPatcher;
import com.mixfa.mongopatcher.patch.ListPatches;
import com.mixfa.mongopatcher.patch.NumberPatches;
import com.mixfa.mongopatcher.patch.ValuePatches;
import com.mixfa.mongopatcher.processor.annotations.IgnoreMethod;
import com.squareup.javapoet.*;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;


@SupportedAnnotationTypes("com.mixfa.mongopatcher.MongoPatcher.Patchable")
@SupportedSourceVersion(SourceVersion.RELEASE_23)
@AutoService(Processor.class)
public class AnnotationProcessor extends AbstractProcessor {
    private Messager messager;
    private Filer filer;
    private Elements elements;
    private Types types;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.messager = processingEnv.getMessager();
        this.filer = processingEnv.getFiler();
        this.types = processingEnv.getTypeUtils();
        this.elements = processingEnv.getElementUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver())
            return false;

        Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(MongoPatcher.Patchable.class);

        for (Element annotatedElement : annotatedElements) {
            if (!(annotatedElement instanceof TypeElement typeElement))
                continue;

            messager.printMessage(Diagnostic.Kind.NOTE, "Processing " + typeElement.getSimpleName());

            try {
                makePatchesClass(typeElement, PatchClassMakingSettings.empty())
                        .ifPresent(patchesClass -> {
                            String originalClassName = typeElement.getSimpleName().toString();
                            String patcherClassName = originalClassName + "Patches";

                            String packageName = processingEnv.getElementUtils()
                                    .getPackageOf(typeElement)
                                    .getQualifiedName()
                                    .toString();
                            String fullyQualifiedPatchesName = packageName.isEmpty() ? patcherClassName : packageName + "." + patcherClassName;

                            var javaFile = JavaFile.builder(packageName, patchesClass.build())
                                    .build();

                            try {
                                javaFile.writeTo(filer); // Filer handles the output directory
                                messager.printMessage(Diagnostic.Kind.NOTE,
                                        "Successfully generated enhanced class: " + fullyQualifiedPatchesName);
                            } catch (
                                    IOException e) {
                                messager.printMessage(Diagnostic.Kind.ERROR,
                                        "Failed to write enhanced class " + fullyQualifiedPatchesName + ": " + e.getMessage());
                                // Re-throw as a runtime exception in an annotation processor to indicate failure
                                throw new RuntimeException("Failed to write generated class", e);
                            }
                        });
            } catch (Exception ex) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "Failed to process " + typeElement.getQualifiedName() + ": " + ex.getMessage());
            }
        }

        return true;
    }

    private MethodSpec makePatchMethod(MethodGenerationContext context, Method method,
                                       ParameterTransformerChain parameterProcessorChain,
                                       MethodNamingPolicy namingPolicy) {
        var methodSpec = MethodSpec.methodBuilder(namingPolicy.nameMethod(context, method))
                .returns(context.parameterizedPatchTypeName());

        methodSpec.addModifiers(Modifier.PUBLIC, Modifier.STATIC);

        var params = new ArrayList<String>();

        for (var param : method.getParameters())
            parameterProcessorChain.transformParameter(context, param, methodSpec).ifPresent(params::add);

        methodSpec.addStatement(
                MessageFormat.format("return $T.{0}({1})", method.getName(), String.join(", ", params)),
                ClassName.get(method.getDeclaringClass())
        );

        return methodSpec.build();
    }

    private static final ParameterTransformerChain FORWARDING_CHAIN = new ParameterTransformerChain(
            List.of(
                    ParameterTransformer.PatchParameterTransformer.forwarding(),
                    ParameterTransformer.FieldParameterTransformer.instance(),
                    ParameterTransformer.TakeParameterizedTypeAnnotationTransformer.instance(),
                    ParameterTransformer.ParameterizeByParameterizedTypeAnnotationTransformer.instance(),
                    ParameterTransformer.ObjectTypeParameterTransformer.instance(),
                    ParameterTransformer.DefaultParameterTransformer.instance()
            )
    );
    private static final ParameterTransformerChain SKIPPING_CHAIN = new ParameterTransformerChain(
            List.of(
                    ParameterTransformer.PatchParameterTransformer.skipping(),
                    ParameterTransformer.FieldParameterTransformer.instance(),
                    ParameterTransformer.TakeParameterizedTypeAnnotationTransformer.instance(),
                    ParameterTransformer.ParameterizeByParameterizedTypeAnnotationTransformer.instance(),
                    ParameterTransformer.ObjectTypeParameterTransformer.instance(),
                    ParameterTransformer.DefaultParameterTransformer.instance()
            )
    );

    private void addPatchMethods(MethodGenerationContext context, Method[] methods, List<MethodSpec> methodSpecs) {
        for (var method : methods) {
            if (method.isAnnotationPresent(IgnoreMethod.class)) continue;

            methodSpecs.add(makePatchMethod(context, method, FORWARDING_CHAIN, MethodNamingPolicy.PostfixNamingPolicy.exPostfix()));
            methodSpecs.add(makePatchMethod(context, method, SKIPPING_CHAIN, MethodNamingPolicy.DefaultNamingPolicy.instance()));
        }
    }


    private Optional<TypeSpec.Builder> makePatchesClass(Element originalClass, PatchClassMakingSettings settings) {
        String originalClassName = originalClass.getSimpleName().toString();
        String patcherClassName = originalClassName + "Patches";

        var patchesClass = TypeSpec.classBuilder(patcherClassName)
                .addModifiers(Modifier.PUBLIC);

        var superVisitor = new ElementVisitor<Void, Object>() {

            @Override
            public Void visit(Element e, Object o) {
                return null;
            }

            @Override
            public Void visitPackage(PackageElement e, Object o) {
                return null;
            }

            @Override
            public Void visitType(TypeElement typeElement, Object o) {
                for (RecordComponentElement component : typeElement.getRecordComponents()) {
                    if (component.getAnnotation(MongoPatcher.IgnoreField.class) != null) continue;

                    var cmpType = component.asType();

                    var fieldAnnotation = component.getAnnotation(Field.class);
                    var fieldAnnotationValue = fieldAnnotation == null ? null : fieldAnnotation.value();

                    var cmpName = fieldAnnotationValue == null ? component.getSimpleName().toString() : fieldAnnotationValue;
                    var cmpTypeName = TypeName.get(cmpType);
                    var patchClass = TypeSpec.classBuilder(cmpName)
                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

                    if (cmpType instanceof DeclaredType declaredType) {
                        var cmpTypeElement = declaredType.asElement();

                        if (cmpTypeElement.getAnnotation(MongoPatcher.Patchable.class) != null) {

                            var componentsList = new ArrayList<RecordComponentElement>();
                            component.accept(new ElementVisitor<Void, List<RecordComponentElement>>() {
                                @Override
                                public Void visitRecordComponent(RecordComponentElement e, List<RecordComponentElement> recordComponentElements) {
                                    recordComponentElements.add(e);
                                    return null;
                                }

                                @Override
                                public Void visit(Element e, List<RecordComponentElement> recordComponentElements) {
                                    return null;
                                }

                                @Override
                                public Void visitPackage(PackageElement e, List<RecordComponentElement> recordComponentElements) {
                                    return null;
                                }

                                @Override
                                public Void visitType(TypeElement e, List<RecordComponentElement> recordComponentElements) {
                                    return null;
                                }

                                @Override
                                public Void visitVariable(VariableElement e, List<RecordComponentElement> recordComponentElements) {
                                    return null;
                                }

                                @Override
                                public Void visitExecutable(ExecutableElement e, List<RecordComponentElement> recordComponentElements) {
                                    return null;
                                }

                                @Override
                                public Void visitTypeParameter(TypeParameterElement e, List<RecordComponentElement> recordComponentElements) {
                                    return null;
                                }

                                @Override
                                public Void visitUnknown(Element e, List<RecordComponentElement> recordComponentElements) {
                                    return null;
                                }

                            }, componentsList);

                            for (RecordComponentElement recordComponentElement : componentsList) {
                                messager.printNote(recordComponentElement.toString());
                                makePatchesClass(((DeclaredType) recordComponentElement.asType()).asElement(),
                                        new PatchClassMakingSettings(
                                                new PatchClassMakingSetting.InnerFieldTweak(cmpName),
                                                new PatchClassMakingSetting.ReplaceOriginalClass(typeElement)
                                        ))
                                        .ifPresent(it -> patchClass.addTypes(it.typeSpecs));
                            }
                        }
                    }

                    var context = getMethodGenerationContext(originalClass, cmpTypeName, cmpName, settings);
                    var methodSpecs = new ArrayList<MethodSpec>();

                    if (cmpTypeName.isPrimitive() || cmpTypeName.isBoxedPrimitive())
                        addPatchMethods(context, NumberPatches.class.getDeclaredMethods(), methodSpecs);

                    var listPatchesAdded = false;
                    try {
                        if (cmpTypeName instanceof ParameterizedTypeName parameterizedTypeName) {
                            var cmpTypeClass = Class.forName(parameterizedTypeName.rawType.canonicalName());
                            if (Iterable.class.isAssignableFrom(cmpTypeClass)) {
                                addPatchMethods(context, ListPatches.class.getDeclaredMethods(), methodSpecs);
                                listPatchesAdded = true;
                            }
                        }
                        if (cmpTypeName instanceof ArrayTypeName) {
                            addPatchMethods(context, ListPatches.class.getDeclaredMethods(), methodSpecs);
                            listPatchesAdded = true;
                        }
                    } catch (ClassNotFoundException e) {
                        messager.printMessage(Diagnostic.Kind.NOTE, "Class not found: " + cmpTypeName);
                    }

                    if (!listPatchesAdded)
                        addPatchMethods(context, ValuePatches.class.getDeclaredMethods(), methodSpecs);

                    methodSpecs.forEach(patchClass::addMethod);
                    patchesClass.addType(patchClass.build());

                }

                return null;
            }

            @Override
            public Void visitRecordComponent(RecordComponentElement component, Object unused) {
                return null;
            }

            @Override
            public Void visitVariable(VariableElement e, Object o) {
                return null;
            }

            @Override
            public Void visitExecutable(ExecutableElement e, Object o) {
                return null;
            }

            @Override
            public Void visitTypeParameter(TypeParameterElement e, Object o) {
                return null;
            }

            @Override
            public Void visitUnknown(Element e, Object o) {
                return null;
            }
        };

        originalClass.accept(superVisitor, null);

        return Optional.of(patchesClass);
    }

    private static TypeName getParameterTypeName(TypeName cmpTypeName) {
        TypeName parameterTypeName = switch (cmpTypeName) {
            case ArrayTypeName arrayTypeName -> arrayTypeName.componentType;
            case ParameterizedTypeName parameterizedTypeName -> parameterizedTypeName.typeArguments.getFirst();
            default -> null;
        };

        if (parameterTypeName != null && parameterTypeName.isBoxedPrimitive())
            parameterTypeName = parameterTypeName.unbox();

        return parameterTypeName;
    }

    private static MethodGenerationContext getMethodGenerationContext(Element originalClass, TypeName
            cmpTypeName, String cmpName, PatchClassMakingSettings settings) {
        return new MethodGenerationContext(cmpName, cmpTypeName, getParameterTypeName(cmpTypeName), originalClass, settings);
    }
}