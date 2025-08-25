package com.github.seregamorph.maven.halflife;

import com.github.seregamorph.maven.halflife.graph.ConcurrencyDependencyGraph2;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.BuildThreadFactory;
import org.apache.maven.lifecycle.internal.LifecycleModuleBuilder;
import org.apache.maven.lifecycle.internal.ProjectBuildList;
import org.apache.maven.lifecycle.internal.ProjectSegment;
import org.apache.maven.lifecycle.internal.ReactorBuildStatus;
import org.apache.maven.lifecycle.internal.ReactorContext;
import org.apache.maven.lifecycle.internal.TaskSegment;
import org.apache.maven.lifecycle.internal.builder.Builder;
import org.apache.maven.plugin.MojosExecutionStrategy;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sergey Chernov
 */
@Singleton
@Named(HalfLifeBuilder.BUILDER_NAME)
public class HalfLifeBuilder implements Builder {

    private static final Logger logger = LoggerFactory.getLogger(HalfLifeBuilder.class);

    public static final String BUILDER_NAME = "half-life";

    private final LifecycleModuleBuilder lifecycleModuleBuilder;

    @Inject
    public HalfLifeBuilder(
        LifecycleModuleBuilder lifecycleModuleBuilder,
        MojosExecutionStrategy mojosExecutionStrategy
    ) {
        this.lifecycleModuleBuilder = lifecycleModuleBuilder;
        if (!(mojosExecutionStrategy instanceof DelegatingMojosExecutionStrategy)) {
            throw new IllegalStateException("Unexpected mojosExecutionStrategy: " + mojosExecutionStrategy
                + ", expected type " + DelegatingMojosExecutionStrategy.class.getName());
        }
    }

    @Override
    public void build(
        MavenSession session,
        ReactorContext reactorContext,
        ProjectBuildList projectBuilds,
        List<TaskSegment> taskSegments,
        ReactorBuildStatus reactorBuildStatus
    ) throws ExecutionException, InterruptedException {
        int nThreads = Math.min(
            session.getRequest().getDegreeOfConcurrency(),
            session.getProjects().size());
        boolean parallel = nThreads > 1;
        session.setParallel(parallel);
        for (ProjectSegment segment : projectBuilds) {
            segment.getSession().setParallel(parallel);
        }
        // executor supporting task ordering, prioritize building modules that have more downstream dependencies
        ExecutorService executor = new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS,
            new PriorityBlockingQueue<>(), new BuildThreadFactory()) {
            @Override
            protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
                return new OrderedFutureTask<>((OrderedCallable<T>) callable);
            }
        };
        OrderedCompletionService<MavenProject> service = new OrderedCompletionService<>(executor);

        for (TaskSegment taskSegment : taskSegments) {
            ProjectBuildList segmentProjectBuilds = projectBuilds.getByTaskSegment(taskSegment);
            Map<MavenProject, ProjectSegment> projectBuildMap = projectBuilds.selectSegment(taskSegment);
            try {
                ConcurrencyDependencyGraph2 analyzer =
                    new ConcurrencyDependencyGraph2(segmentProjectBuilds, session.getProjectDependencyGraph());
                new Scheduler(session, reactorContext, service, analyzer, taskSegment, projectBuildMap)
                    .multiThreadedProjectTaskSegmentBuild();
                if (reactorContext.getReactorBuildStatus().isHalted()) {
                    break;
                }
            } catch (Exception e) {
                session.getResult().addException(e);
                break;
            }
        }

        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }

    private class Scheduler {

        private final MavenSession rootSession;
        private final ReactorContext reactorContext;
        private final OrderedCompletionService<MavenProject> service;
        private final ConcurrencyDependencyGraph2 analyzer;
        private final TaskSegment taskSegment;
        private final Map<MavenProject, ProjectSegment> projectBuildMap;

        private final Set<String> duplicateArtifactIds;

        private Scheduler(
            MavenSession rootSession,
            ReactorContext reactorContext,
            OrderedCompletionService<MavenProject> service,
            ConcurrencyDependencyGraph2 analyzer,
            TaskSegment taskSegment,
            Map<MavenProject, ProjectSegment> projectBuildMap
        ) {
            this.rootSession = rootSession;
            this.reactorContext = reactorContext;
            this.service = service;
            this.analyzer = analyzer;
            this.taskSegment = taskSegment;
            this.projectBuildMap = projectBuildMap;

            duplicateArtifactIds = gatherDuplicateArtifactIds(projectBuildMap.keySet());
        }

        private void multiThreadedProjectTaskSegmentBuild() {
            scheduleProjects(analyzer.getRootSchedulableBuilds());
            for (int i = 0; i < analyzer.getNumberOfBuilds(); i++) {
                try {
                    MavenProject project = service.take();
                    if (reactorContext.getReactorBuildStatus().isHalted()) {
                        break;
                    }

                    if (analyzer.getNumberOfBuilds() > 1) {
                        List<MavenProject> newItemsThatCanBeBuilt = analyzer.markAsFinished(project);
                        scheduleProjects(newItemsThatCanBeBuilt);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    rootSession.getResult().addException(e);
                    break;
                }
            }
        }

        private void scheduleProjects(List<MavenProject> projects) {
            for (MavenProject mavenProject : projects) {
                ProjectSegment projectSegment = projectBuildMap.get(mavenProject);
                logger.debug("Scheduling: {}", projectSegment);
                service.submit(0, () -> {
                    Thread currentThread = Thread.currentThread();
                    String originalThreadName = currentThread.getName();
                    MavenProject project = projectSegment.getProject();

                    String threadNameSuffix = duplicateArtifactIds.contains(project.getArtifactId())
                        ? project.getGroupId() + ":" + project.getArtifactId()
                        : project.getArtifactId();
                    currentThread.setName("mvn-builder-" + threadNameSuffix);
                    try {
                        lifecycleModuleBuilder.buildProject(
                            projectSegment.getSession(), rootSession, reactorContext, project, taskSegment);
                    } finally {
                        currentThread.setName(originalThreadName);
                    }

                    return project;
                });
            }
        }
    }

    private static Set<String> gatherDuplicateArtifactIds(Set<MavenProject> projects) {
        Set<String> artifactIds = new HashSet<>(projects.size());
        Set<String> duplicateArtifactIds = new HashSet<>();
        for (MavenProject project : projects) {
            if (!artifactIds.add(project.getArtifactId())) {
                duplicateArtifactIds.add(project.getArtifactId());
            }
        }
        return duplicateArtifactIds;
    }
}
