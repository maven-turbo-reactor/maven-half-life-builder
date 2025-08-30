package com.github.seregamorph.maven.halflife;

import com.github.seregamorph.maven.halflife.graph.ProjectPart;
import java.util.function.Consumer;

/**
 * @author Sergey Chernov
 */
final class CurrentProjectExecution {

    private static final ThreadLocal<CurrentProjectExecution> currentProjectExecution = new ThreadLocal<>();

    final ProjectPart part;

    private CurrentProjectExecution(ProjectPart part) {
        this.part = part;
    }

    static void doWithCurrentProject(ProjectPart part, Runnable task) {
        CurrentProjectExecution execution = new CurrentProjectExecution(part);
        currentProjectExecution.set(execution);
        try {
            task.run();
        } finally {
            currentProjectExecution.remove();
        }
    }

    static void ifPresent(Consumer<CurrentProjectExecution> action) {
        CurrentProjectExecution execution = currentProjectExecution.get();
        if (execution != null) {
            action.accept(execution);
        }
    }
}
