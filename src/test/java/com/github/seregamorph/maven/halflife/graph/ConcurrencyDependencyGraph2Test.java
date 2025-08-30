package com.github.seregamorph.maven.halflife.graph;

import static com.github.seregamorph.maven.halflife.graph.ProjectPart.MAIN;
import static com.github.seregamorph.maven.halflife.graph.ProjectPart.TEST;
import static com.github.seregamorph.maven.halflife.graph.TestUtils.jacksonCoreCompileDependency;
import static com.github.seregamorph.maven.halflife.graph.TestUtils.junitJupiterTestDependency;
import static com.github.seregamorph.maven.halflife.graph.TestUtils.moduleDependency;
import static com.github.seregamorph.maven.halflife.graph.TestUtils.parentModel;
import static com.github.seregamorph.maven.halflife.graph.TestUtils.project;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.apache.maven.project.DuplicateProjectException;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.junit.jupiter.api.Test;

class ConcurrencyDependencyGraph2Test {

    @Test
    public void shouldSort() throws CycleDetectedException, DuplicateProjectException {
        var parent = project("parent");
        var app = project("app");
        var core = project("core");
        var testUtils = project("test-utils");

        app.getModel().setParent(parentModel(parent));
        core.getModel().setParent(parentModel(parent));
        testUtils.getModel().setParent(parentModel(parent));

        app.getDependencies().addAll(List.of(
            moduleDependency("core", Scope.COMPILE),
            junitJupiterTestDependency()
        ));
        core.getDependencies().addAll(List.of(
            jacksonCoreCompileDependency(),
            moduleDependency("test-utils", Scope.TEST),
            junitJupiterTestDependency()
        ));

        var projects = List.of(parent, app, core, testUtils);
        var defaultProjectDependencyGraph = new DefaultProjectDependencyGraph2(projects);
        var filteredProjectDependencyGraph = new FilteredProjectDependencyGraph2(defaultProjectDependencyGraph,
            projects);
        var analyzer = new ConcurrencyDependencyGraph2(projects, filteredProjectDependencyGraph);

        assertEquals(8, analyzer.getNumberOfBuilds());

        assertEquals(List.of(
            new MavenProjectPart(parent, MAIN)
        ), analyzer.getRootSchedulableBuilds());

        assertEquals(List.of(
            new MavenProjectPart(parent, TEST),
            new MavenProjectPart(core, MAIN),
            new MavenProjectPart(testUtils, MAIN)
        ), analyzer.markAsFinished(new MavenProjectPart(parent, MAIN)));
        assertEquals(List.of(
            new MavenProjectPart(app, MAIN)
        ), analyzer.markAsFinished(new MavenProjectPart(core, MAIN)));
        assertEquals(List.of(
            new MavenProjectPart(app, TEST)
        ), analyzer.markAsFinished(new MavenProjectPart(app, MAIN)));
        assertEquals(List.of(
        ), analyzer.markAsFinished(new MavenProjectPart(app, TEST)));
        assertEquals(List.of(
            new MavenProjectPart(core, TEST),
            new MavenProjectPart(testUtils, TEST)
        ), analyzer.markAsFinished(new MavenProjectPart(testUtils, MAIN)));
        assertEquals(List.of(), analyzer.markAsFinished(new MavenProjectPart(core, TEST)));
        assertEquals(List.of(), analyzer.markAsFinished(new MavenProjectPart(parent, TEST)));
        assertEquals(List.of(), analyzer.markAsFinished(new MavenProjectPart(testUtils, TEST)));
    }
}
