package com.github.seregamorph.maven.halflife;

import com.github.seregamorph.maven.halflife.graph.ConcurrencyDependencyGraph2;
import com.github.seregamorph.maven.halflife.graph.MavenProjectPart;
import java.util.List;
import java.util.Map;
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

    static final String BUILDER_NAME = "half-life";

    private final LifecycleModuleBuilder lifecycleModuleBuilder;

    @Inject
    public HalfLifeBuilder(LifecycleModuleBuilder lifecycleModuleBuilder) {
        this.lifecycleModuleBuilder = lifecycleModuleBuilder;
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
        OrderedCompletionService<MavenProjectPart> service = new OrderedCompletionService<>(executor);

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
        // todo update reactorContext.getResult() buildSummary (append MAIN and TEST)
    }

    private class Scheduler {

        private final MavenSession rootSession;
        private final ReactorContext reactorContext;
        private final OrderedCompletionService<MavenProjectPart> service;
        private final ConcurrencyDependencyGraph2 analyzer;
        private final TaskSegment taskSegment;
        private final Map<MavenProject, ProjectSegment> projectBuildMap;

        private Scheduler(
            MavenSession rootSession,
            ReactorContext reactorContext,
            OrderedCompletionService<MavenProjectPart> service,
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
        }

        private void multiThreadedProjectTaskSegmentBuild() {
            scheduleProjects(analyzer.getRootSchedulableBuilds());
            for (int i = 0; i < analyzer.getNumberOfBuilds(); i++) {
                try {
                    MavenProjectPart project = service.take();
                    if (reactorContext.getReactorBuildStatus().isHalted()) {
                        break;
                    }

                    if (analyzer.getNumberOfBuilds() > 1) {
                        List<MavenProjectPart> newItemsThatCanBeBuilt = analyzer.markAsFinished(project);
                        scheduleProjects(newItemsThatCanBeBuilt);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    rootSession.getResult().addException(e);
                    break;
                }
            }
        }

        private void scheduleProjects(List<MavenProjectPart> projects) {
            for (MavenProjectPart mavenProjectPart : projects) {
                MavenProject mavenProject = mavenProjectPart.getProject();
                ProjectSegment projectSegment = projectBuildMap.get(mavenProject);
                logger.debug("Scheduling: {}", projectSegment);
                service.submit(0, () -> {
                    Thread currentThread = Thread.currentThread();
                    String originalThreadName = currentThread.getName();
                    MavenProject project = projectSegment.getProject();

                    String threadNameSuffix = mavenProject.getGroupId() + ":" + mavenProject.getArtifactId();
                    currentThread.setName("mvn-builder-" + threadNameSuffix);
                    try {
                        // Implementation notice:
                        // Before Maven 3.9 projectSegment.getSession() != rootSession
                        // Since  Maven 3.9 projectSegment.getSession() == rootSession (based on ThreadLocal)
                        lifecycleModuleBuilder.buildProject(
                            projectSegment.getSession(), rootSession, reactorContext, project, taskSegment);
                    } finally {
                        currentThread.setName(originalThreadName);
                    }

                    return mavenProjectPart;
                });
            }
        }
    }
}
