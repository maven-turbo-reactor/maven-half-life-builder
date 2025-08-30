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
            new MavenProjectPart(parent, MAIN),
            new MavenProjectPart(core, MAIN),
            new MavenProjectPart(app, MAIN),
            new MavenProjectPart(app, TEST),
            new MavenProjectPart(testUtils, MAIN),
            new MavenProjectPart(core, TEST),
            new MavenProjectPart(parent, TEST),
            new MavenProjectPart(testUtils, TEST)
        ), projectSorter.getSortedProjectParts());

        assertEquals(List.of(), projectSorter.getDependencies(id(parent, MAIN)));
        assertEquals(List.of(
            "groupId:parent:1.0-SNAPSHOT(main)"
        ), projectSorter.getDependencies(id(parent, TEST)));
        assertEquals(List.of(
            "groupId:parent:1.0-SNAPSHOT(test)",
            "groupId:app:1.0-SNAPSHOT(main)",
            "groupId:app:1.0-SNAPSHOT(test)",
            "groupId:core:1.0-SNAPSHOT(main)",
            "groupId:core:1.0-SNAPSHOT(test)",
            "groupId:test-utils:1.0-SNAPSHOT(main)",
            "groupId:test-utils:1.0-SNAPSHOT(test)"
        ), projectSorter.getDependents(id(parent, MAIN)));
        assertEquals(List.of(), projectSorter.getDependents(id(parent, TEST)));

        assertEquals(List.of(
            "groupId:parent:1.0-SNAPSHOT(main)"
        ), projectSorter.getDependencies(id(testUtils, MAIN)));
        assertEquals(List.of(
            "groupId:test-utils:1.0-SNAPSHOT(main)",
            "groupId:parent:1.0-SNAPSHOT(main)"
        ), projectSorter.getDependencies(id(testUtils, TEST)));
        assertEquals(List.of(
            "groupId:test-utils:1.0-SNAPSHOT(test)",
            "groupId:core:1.0-SNAPSHOT(test)"
        ), projectSorter.getDependents(id(testUtils, MAIN)));
        assertEquals(List.of(), projectSorter.getDependents(id(testUtils, TEST)));

        assertEquals(List.of(
            "groupId:parent:1.0-SNAPSHOT(main)"
        ), projectSorter.getDependencies(id(core, MAIN)));
        assertEquals(List.of(
            "groupId:core:1.0-SNAPSHOT(main)",
            "groupId:test-utils:1.0-SNAPSHOT(main)",
            "groupId:parent:1.0-SNAPSHOT(main)"
        ), projectSorter.getDependencies(id(core, TEST)));
        assertEquals(List.of(
            "groupId:core:1.0-SNAPSHOT(test)",
            "groupId:app:1.0-SNAPSHOT(main)"
        ), projectSorter.getDependents(id(core, MAIN)));
        assertEquals(List.of(), projectSorter.getDependents(id(core, TEST)));

        assertEquals(List.of(
            "groupId:core:1.0-SNAPSHOT(main)",
            "groupId:parent:1.0-SNAPSHOT(main)"
        ), projectSorter.getDependencies(id(app, MAIN)));
        assertEquals(List.of(
            "groupId:app:1.0-SNAPSHOT(main)",
            "groupId:parent:1.0-SNAPSHOT(main)"
        ), projectSorter.getDependencies(id(app, TEST)));
        assertEquals(List.of(
            "groupId:app:1.0-SNAPSHOT(test)"
        ), projectSorter.getDependents(id(app, MAIN)));
        assertEquals(List.of(), projectSorter.getDependents(id(app, TEST)));
    }

    private static String id(MavenProject parent, ProjectPart part) {
        return new MavenProjectPart(parent, part).toString();
    }
}
