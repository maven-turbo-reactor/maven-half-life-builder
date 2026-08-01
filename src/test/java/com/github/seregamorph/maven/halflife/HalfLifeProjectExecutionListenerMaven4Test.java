package com.github.seregamorph.maven.halflife;

import static com.github.seregamorph.maven.halflife.graph.ProjectPart.MAIN;
import static com.github.seregamorph.maven.halflife.graph.ProjectPart.TEST;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashSet;
import java.util.List;
import org.apache.maven.internal.impl.DefaultLifecycleRegistry;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.junit.jupiter.api.Test;

class HalfLifeProjectExecutionListenerMaven4Test {

    @Test
    public void shouldExecuteMojoInMainNoTestJar() {
        var mainPhases = List.of(
            "before:clean",
            "clean",
            "after:clean",
            "before:all",
            "before:initialize",
            "before:validate",
            "validate",
            "after:validate",
            "initialize",
            "after:initialize",
            "before:build",
            "before:sources",
            "sources",
            "after:sources",
            "before:resources",
            "resources",
            "after:resources",
            "before:compile",
            "compile",
            "after:compile",
            "before:ready",
            "ready",
            "after:ready",
            "before:package",
            "package",
            "after:package",
            "build",
            "after:build",

            "before:site",
            "site",
            "after:site",
            "before:site-deploy",
            "site-deploy",
            "after:site-deploy"
        );
        var testPhases = List.of(
            "before:test-sources",
            "test-sources",
            "after:test-sources",
            "before:test-resources",
            "test-resources",
            "after:test-resources",
            "before:test-compile",
            "test-compile",
            "after:test-compile",
            "before:test",
            "test",
            "after:test",
            "before:unit-test",
            "unit-test",
            "after:unit-test",
            "before:verify",
            "before:integration-test",
            "integration-test",
            "after:integration-test",
            "verify",
            "after:verify",
            "before:install",
            "install",
            "after:install",
            "before:deploy",
            "deploy",
            "after:deploy",
            "all",
            "after:all"
            );

        for (var mainPhase : mainPhases) {
            assertEquals(MAIN, HalfLifeProjectExecutionListener.getProjectPart(false,
                    mojoExecution(mainPhase)),
                "Should execute mojo phase " + mainPhase + " for part MAIN");
        }
        for (var testPhase : testPhases) {
            assertEquals(TEST, HalfLifeProjectExecutionListener.getProjectPart(false,
                    mojoExecution(testPhase)),
                "Should execute mojo phase " + testPhase + " for part TEST");
        }

        shouldAllPhasesListedInTest(mainPhases, testPhases);
    }

    private static void shouldAllPhasesListedInTest(List<String> mainPhases, List<String> testPhases) {
        var defaultLifecycleRegistry = new DefaultLifecycleRegistry();
        var allLifecyclePhases = new LinkedHashSet<String>();
        for (var lifecycle : defaultLifecycleRegistry) {
            var phases = defaultLifecycleRegistry.computePhases(lifecycle);
            allLifecyclePhases.addAll(phases);
        }

        var allCheckedPhases = new LinkedHashSet<>(mainPhases);
        allCheckedPhases.addAll(testPhases);
        assertEquals(allCheckedPhases, allLifecyclePhases);
    }

    private static MojoExecution mojoExecution(String phase) {
        var mojoExecution = new MojoExecution(new MojoDescriptor());
        mojoExecution.setLifecyclePhase(phase);
        return mojoExecution;
    }
}
