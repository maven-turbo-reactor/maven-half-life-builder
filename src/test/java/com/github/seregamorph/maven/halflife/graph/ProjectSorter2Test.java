package com.github.seregamorph.maven.halflife.graph;

import static com.github.seregamorph.maven.halflife.graph.TestUtils.jacksonCoreCompileDependency;
import static com.github.seregamorph.maven.halflife.graph.TestUtils.junitJupiterTestDependency;
import static com.github.seregamorph.maven.halflife.graph.TestUtils.moduleDependency;
import static com.github.seregamorph.maven.halflife.graph.TestUtils.parentModel;
import static com.github.seregamorph.maven.halflife.graph.TestUtils.project;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.junit.jupiter.api.Test;

class ProjectSorter2Test {

    @Test
    public void shouldSort() throws CycleDetectedException {
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

        var projectSorter = new ProjectSorter2(List.of(app, core, parent, testUtils));
        assertEquals(List.of(
            new MavenProjectPart(parent),
            new MavenProjectPart(testUtils),
            new MavenProjectPart(core),
            new MavenProjectPart(app)
        ), projectSorter.getSortedProjectParts());

        assertEquals(List.of(), projectSorter.getDependencies(id(parent)));
        assertEquals(List.of(
            "groupId:app:1.0-SNAPSHOT",
            "groupId:core:1.0-SNAPSHOT",
            "groupId:test-utils:1.0-SNAPSHOT"
        ), projectSorter.getDependents(id(parent)));

        assertEquals(List.of(
            "groupId:parent:1.0-SNAPSHOT"
        ), projectSorter.getDependencies(id(testUtils)));
        assertEquals(List.of(
            "groupId:core:1.0-SNAPSHOT"
        ), projectSorter.getDependents(id(testUtils)));

        assertEquals(List.of(
            "groupId:test-utils:1.0-SNAPSHOT",
            "groupId:parent:1.0-SNAPSHOT"
        ), projectSorter.getDependencies(id(core)));
        assertEquals(List.of(
            "groupId:app:1.0-SNAPSHOT"
        ), projectSorter.getDependents(id(core)));

        assertEquals(List.of(
            "groupId:core:1.0-SNAPSHOT",
            "groupId:parent:1.0-SNAPSHOT"
        ), projectSorter.getDependencies(id(app)));
        assertEquals(List.of(), projectSorter.getDependents(id(app)));
    }

    private static String id(MavenProject parent) {
        return new MavenProjectPart(parent).toString();
    }
}
