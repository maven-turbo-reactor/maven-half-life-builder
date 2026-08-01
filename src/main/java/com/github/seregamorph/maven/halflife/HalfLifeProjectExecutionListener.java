package com.github.seregamorph.maven.halflife;

import com.github.seregamorph.maven.halflife.graph.ProjectPart;
import java.util.Arrays;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.execution.ProjectExecutionEvent;
import org.apache.maven.execution.ProjectExecutionListener;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.jspecify.annotations.Nullable;

/**
 * @author Sergey Chernov
 */
@SuppressWarnings("CodeBlock2Expr")
@Named
@Singleton
public class HalfLifeProjectExecutionListener implements ProjectExecutionListener {

    @Override
    public void beforeProjectExecution(ProjectExecutionEvent event) {
    }

    @Override
    public void beforeProjectLifecycleExecution(ProjectExecutionEvent event) {
        CurrentProjectExecution.ifPresent(execution -> {
            boolean hasTestJar = TestJarSupport.hasTestJar(event.getExecutionPlan());
            event.getExecutionPlan().removeIf(mojoExecution -> {
                // TODO if isSkipPartSplit, there should not be TEST project part at all (assert)
                boolean skipPartSplit = SkipPartSplitUtils.isSkipPartSplit(event.getSession(), event.getProject());
                ProjectPart mojoProjectPart = skipPartSplit ? ProjectPart.MAIN :
                    getProjectPart(hasTestJar, mojoExecution);
                return mojoProjectPart != execution.part;
            });
        });
    }

    static ProjectPart getProjectPart(boolean hasTestJar, MojoExecution mojoExecution) {
        String phase = getLifecyclePhase(mojoExecution);
        if (phase == null) {
            // reinsure for CLI goal executions
            return ProjectPart.MAIN;
        }

        // exception: Maven 4 "before:all" in MAIN, but "all" and "after:all" are TEST
        if ("before:all".equals(phase)) {
            return ProjectPart.MAIN;
        }

        // Since Maven 4
        int colonSep = phase.indexOf(':');
        if (colonSep > 0) {
            phase = phase.substring(colonSep + 1);
        }

        if (Arrays.asList(
            // "clean" lifecycle
            "pre-clean",
            "clean",
            "post-clean",
            // "site" lifecycle
            "pre-site",
            "site",
            "post-site",
            "site-deploy",
            // "default" lifecycle
            "validate",
            "initialize",
            "build",
            "sources",
            "resources",
            "generate-sources",
            "process-sources",
            "generate-resources",
            "process-resources",
            "compile",
            "process-classes",
            "prepare-package",
            "ready",
            "package"
        ).contains(phase)) {
            return ProjectPart.MAIN;
        }
        if (hasTestJar) {
            // if project has the "test-jar" goal, it should compile (but not run)
            // the test sources in the MAIN part as well
            if (Arrays.asList(
                "generate-test-sources",
                "process-test-sources",
                "generate-test-resources",
                "process-test-resources",
                "test-compile",
                "process-test-classes"
            ).contains(phase)) {
                return ProjectPart.MAIN;
            }
        }

        return ProjectPart.TEST;
    }

    @Nullable
    private static String getLifecyclePhase(MojoExecution mojoExecution) {
        String phase = mojoExecution.getLifecyclePhase();
        if (phase == null) {
            MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();
            if (mojoDescriptor != null) {
                phase = mojoDescriptor.getPhase();
            }
        }
        return phase;
    }

    @Override
    public void afterProjectExecutionSuccess(ProjectExecutionEvent event) {
    }

    @Override
    public void afterProjectExecutionFailure(ProjectExecutionEvent event) {
    }
}
