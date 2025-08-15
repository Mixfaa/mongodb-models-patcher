package disabled;

import com.mixfa.mongopatcher.*;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodDelegation;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static net.bytebuddy.description.modifier.Ownership.STATIC;
import static net.bytebuddy.description.modifier.Visibility.PUBLIC;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class GeneratorMojo extends AbstractMojo {
    /**
     * The Maven project.
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    public static Implementation resolveDelegator(int paramsCount, Method method, String field) throws IllegalAccessException {
        final var lookup = MethodHandles.lookup();

        if (paramsCount == 0)
            return MethodDelegation.to(new Delegators.Delegator0(field, lookup.unreflect(method)));
        else if (paramsCount == 1)
            return MethodDelegation.to(new Delegators.Delegator1(field, lookup.unreflect(method)));
        else if (paramsCount == 2)
            return MethodDelegation.to(new Delegators.Delegator2(field, lookup.unreflect(method)));
        else if (paramsCount == 3)
            return MethodDelegation.to(new Delegators.Delegator3(field, lookup.unreflect(method)));

        throw new RuntimeException("Invalid params count");
    }

    public static <T> DynamicType.Builder<T> processMethods(DynamicType.Builder<T> typeBuilder, List<Method> methods, String compName) throws IllegalAccessException {
        for (Method method : methods) {
            var params = Arrays.stream(method.getParameters())
                    .filter(param -> !param.getType().equals(Patch.class) ||
                            !(param.getType().equals(String.class) && param.getName().equals("field")))
                    .map(java.lang.reflect.Parameter::getType)
                    .toList();
            typeBuilder = typeBuilder.defineMethod(method.getName(), method.getReturnType(), PUBLIC, STATIC)
                    .withParameters(params)
                    .intercept(resolveDelegator(params.size(), method, compName));
        }

        return typeBuilder;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final var log = getLog();
        getLog().info("Starting internal class generation with ByteBuddy...");

        // 1. Determine output directory for generated classes
        File outputDirectory = new File(project.getBuild().getOutputDirectory());
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }
        getLog().info("Generated classes will be placed in: " + outputDirectory.getAbsolutePath());

        // 2. Prepare classpath for Reflections to scan compiled classes
        List<URL> classpathUrls = new ArrayList<>();
        try {
            classpathUrls.add(new File(project.getBuild().getOutputDirectory()).toURI().toURL());
            getLog().info("Scanning classes from: " + project.getBuild().getOutputDirectory());

            for (String element : project.getCompileClasspathElements()) {
                classpathUrls.add(new File(element).toURI().toURL());
            }
        } catch (MalformedURLException | org.apache.maven.artifact.DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Error building classpath for Reflections", e);
        }

        URLClassLoader classLoader = new URLClassLoader(classpathUrls.toArray(new URL[0]), Thread.currentThread().getContextClassLoader());

        // 3. Use Reflections to find annotated classes
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forClassLoader(classLoader))
                .setScanners(Scanners.TypesAnnotated)
                .addClassLoaders(classLoader)
        );

        Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(Patchable.class);

        if (annotatedClasses.isEmpty()) {
            getLog().info("No classes found with @Patchable annotation. Nothing to generate.");
            return;
        }

        getLog().info("Found " + annotatedClasses.size() + " annotated classes. Generating internal classes...");

        ByteBuddy byteBuddy = new ByteBuddy();

        for (Class<?> annotatedClass : annotatedClasses) {
            String originalClassName = annotatedClass.getSimpleName();
            String originalPackageName = annotatedClass.getPackage().getName();

            log.info("Processing annotated class: " + annotatedClass.getName());
            log.info("  Original Class Name: " + originalClassName);
            log.info("  Original Package Name: " + originalPackageName);

            try {
                // Create the main Patches nested class
                String patchesClassName = originalPackageName + "." + originalClassName + "$Patches";

                var patchesBuilder = byteBuddy
                        .subclass(Object.class)
                        .modifiers(PUBLIC, STATIC)
                        .name(patchesClassName);

                // Create nested classes for each record component
                List<DynamicType.Unloaded<?>> nestedClasses = new ArrayList<>();

                final var forAllMethods = new ArrayList<>(List.of(ValuePatches.class.getDeclaredMethods()));
                final var forListMethods = new ArrayList<>(List.of(ListPatches.class.getDeclaredMethods()));
                final var forNumberMethods = new ArrayList<>(List.of(NumberPatches.class.getDeclaredMethods()));

                for (var recordComponent : annotatedClass.getRecordComponents()) {
                    var compType = recordComponent.getType();
                    var compName = recordComponent.getName();

                    // Create nested class name: OuterClass$Patches$fieldName
                    String nestedClassName = patchesClassName + "$" + compName;

                    var typeBuilder = byteBuddy
                            .subclass(Object.class)
                            .modifiers(PUBLIC, STATIC)
                            .name(nestedClassName);

                    typeBuilder = processMethods(typeBuilder, forAllMethods, compName);

                    if (compType.isArray() || Iterable.class.isAssignableFrom(compType)) {
                        typeBuilder = processMethods(typeBuilder, forListMethods, compName);
                    } else if (Number.class.isAssignableFrom(compType) || compType.isPrimitive()) {
                        typeBuilder = processMethods(typeBuilder, forNumberMethods, compName);
                    }

                    var nestedClassType = typeBuilder.make();
                    nestedClasses.add(nestedClassType);

                    // Save the nested class
                    nestedClassType.saveIn(outputDirectory);

                    log.info("Generated nested class: " + nestedClassName);
                }

                // Create and save the main Patches class
                var patchesType = patchesBuilder.make();
                patchesType.saveIn(outputDirectory);

                log.info("Generated Patches class: " + patchesClassName);

            } catch (Exception e) {
                log.error("Error generating classes for " + annotatedClass.getName(), e);
                throw new MojoExecutionException("Failed to generate classes", e);
            }
        }

        getLog().info("Class generation completed successfully!");
    }
}