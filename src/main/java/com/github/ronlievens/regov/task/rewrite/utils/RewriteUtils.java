package com.github.ronlievens.regov.task.rewrite.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.openrewrite.*;
import org.openrewrite.config.CompositeRecipe;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.marker.Generated;
import org.openrewrite.maven.AbstractRewriteBaseRunMojo;
import org.openrewrite.maven.RewriteMojoRun;
import org.openrewrite.xml.XmlParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import static com.github.ronlievens.regov.util.PathUtils.pathFilter;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RewriteUtils {

    public static Recipe loadRecipe(final String recipeLocation, final String recipe) {
        val result = new CompositeRecipe(new ArrayList<>());
        if (isNotBlank(recipeLocation)) {
            for (val recipeName : recipeLocation.split(",")) {
                try {
                    val file = new File(recipeLocation);
                    val fileInputStream = new FileInputStream(file);
                    result.getRecipeList().add(new CompositeRecipe(Environment.builder().load(new YamlResourceLoader(fileInputStream, file.toURI(), new Properties())).build().listRecipes()));
                } catch (FileNotFoundException e) {
                    log.warn("Unable to find recipe file: {}", recipeName);
                }
            }
        }

        if (isNotBlank(recipe)) {
            for (val recipeName : recipe.split(",")) {
                try {
                    result.getRecipeList().add(Environment.builder().scanRuntimeClasspath().build().activateRecipes(recipeName));
                } catch (RecipeException re) {
                    log.trace("RecipeException: {}", re.getMessage());
                }
            }
        }

        return result;
    }

    public static boolean rewrite(@NonNull final Path path, final Recipe recipe) throws IOException {
        log.trace("Running openrewrite on path: {}", path.toAbsolutePath());
        val env = Environment.builder().scanYamlResources().build();
        val context = new InMemoryExecutionContext();
        val javaParser = JavaParser.fromJavaVersion();
        javaParser.styles(env.listStyles()).logCompilationWarningsAndErrors(false);
        val sourceSet = directoryListing(context, javaParser, path);

        val results = new AbstractRewriteBaseRunMojo.ResultsContainer(path, runRecipe(recipe, sourceSet, context));
        try {
            val mojoRunner = new RewriteMojoRun(context, results);
            mojoRunner.execute();
        } catch (MojoExecutionException | MojoFailureException e) {
            throw new RuntimeException(e);
        }

        for (val recipeItem : recipe.getRecipeList()) {
            val validation = recipeItem.validate();
            if (validation.isInvalid()) {
                log.error("{}", validation.getValue());
            }
        }
        return results.isNotEmpty();
    }

    private static LargeSourceSet directoryListing(final InMemoryExecutionContext ctx, final JavaParser.Builder<? extends JavaParser, ?> javaParser, final Path path) throws IOException {
        val javaFiles = pathFilter(path, List.of(".java"), null, null);
        val xmlFiles = pathFilter(path, List.of(".xjb", ".xml"), null, null);

        log.trace("Found xml files: {}", xmlFiles);
        log.trace("Found java files: {}", javaFiles);

        val xmlParser = new XmlParser.Builder().build();
        val bindingSource = xmlParser.parse(xmlFiles, path, ctx);
        val javaSourceFiles = javaParser.build().parse(javaFiles, path, ctx);

        return new InMemoryLargeSourceSet(Stream.of(javaSourceFiles, bindingSource).flatMap(s -> s).toList());
    }

    private static List<Result> runRecipe(final Recipe recipe, final LargeSourceSet sourceSet, final ExecutionContext context) {
        return recipe.run(sourceSet, context).getChangeset().getAllResults().stream().filter(source -> {
            if (source.getBefore() != null) {
                return source.getBefore().getMarkers().findFirst(Generated.class).isEmpty();
            }
            return true;
        }).toList();
    }
}
