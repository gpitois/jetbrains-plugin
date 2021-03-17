package com.code_inspector.plugins.intellij.annotators;

import com.code_inspector.api.GetFileDataQuery;
import com.code_inspector.plugins.intellij.cache.AnalysisDataCache;
import com.code_inspector.plugins.intellij.git.CodeInspectorGitUtils;
import com.code_inspector.plugins.intellij.settings.application.AppSettingsState;
import com.code_inspector.plugins.intellij.settings.project.ProjectSettingsState;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

import static com.code_inspector.plugins.intellij.Constants.INVALID_PROJECT_ID;
import static com.code_inspector.plugins.intellij.Constants.LOGGER_NAME;
import static com.code_inspector.plugins.intellij.Constants.NO_ANNOTATION;
import static com.code_inspector.plugins.intellij.graphql.CodeInspectorApiUtils.getAnnotationsFromQueryResult;
import static com.code_inspector.plugins.intellij.ui.UIConstants.ANNOTATION_PREFIX;

public class CodeInspectorExternalAnnotator extends ExternalAnnotator<PsiFile, List<CodeInspectionAnnotation>> {

    public static final Logger LOGGER = Logger.getInstance(LOGGER_NAME);

    /**
     * This function collects all the information at startup (see the doc of the abstract class).
     * For now, we are not doing anything, we rather use doAnnotate that runs the long running
     * computation.
     * @param psiFile - the file where information is being collected
     * @param editor - the editor where the function is triggered
     * @param hasErrors - report if it has error
     * @return the PsiFile - return the initial file, no modification is being done.
     */
    @Override
    @Nullable
    public PsiFile collectInformation(
        @NotNull PsiFile psiFile, @NotNull Editor editor, boolean hasErrors) {
        LOGGER.debug("call collectInformation()");
        return psiFile;
    }

    /**
     * Gather all the annotations from the Code Inspector API and generates a list of annotation
     * to surface later in the UI.
     * @param psiFile - the file to inspect.
     * @return the list of annotation to surface.
     */
    @Nullable
    @Override
    public List<CodeInspectionAnnotation> doAnnotate(PsiFile psiFile) {
        final ProjectSettingsState PROJECT_SETTINGS = ProjectSettingsState.getInstance(psiFile.getProject());
        if (!PROJECT_SETTINGS.isEnabled) {
            return NO_ANNOTATION;
        }

        ProgressManager.checkCanceled();

        LOGGER.debug(String.format("calling doAnnotate on file %s, type %s", psiFile.getVirtualFile().getPath(), psiFile.getLanguage().toString()));

        ProjectSettingsState settings = ProjectSettingsState.getInstance(psiFile.getProject());
        if (settings.projectId.equals(INVALID_PROJECT_ID)) {
            // TODO show an information in the project status that it's not configured
            LOGGER.info("project not configured");
            return NO_ANNOTATION;
        }

        Optional<String> revision = CodeInspectorGitUtils.getGitRevision(psiFile);

        if (!revision.isPresent()) {
            LOGGER.info("cannot get file revision");
            return NO_ANNOTATION;
        }

        Optional<String> filePath = CodeInspectorGitUtils.getFilePathInRepository(psiFile);

        if (!filePath.isPresent()) {
            LOGGER.info("cannot get file patch");
            return NO_ANNOTATION;
        }

        Optional<GetFileDataQuery.Project> query = AnalysisDataCache.getInstance().getData(settings.projectId, revision.get(), filePath.get());

        if (!query.isPresent()) {
            LOGGER.info("no data from query");
            return NO_ANNOTATION;
        }

        // If the API took too long, check that this is still okay to proceed.
        ProgressManager.checkCanceled();

        return getAnnotationsFromQueryResult(query.get(), psiFile);
    }

    /**
     * Get the HighlightSeverity for an annotation
     * @param annotation - the annotation we want to inspect
     * @return the corresponding HighlightSeverity value
     */
    private HighlightSeverity getHighlightSeverityForViolation(CodeInspectionAnnotation annotation) {
        if (annotation.getSeverity().isPresent()) {
            switch (annotation.getSeverity().get().intValue()) {
                case 1:
                    return HighlightSeverity.ERROR;
                case 2:
                    return HighlightSeverity.WARNING;
                case 3:
                    return HighlightSeverity.WEAK_WARNING;
                default:
                    return HighlightSeverity.WEAK_WARNING;
            }
        }
        return HighlightSeverity.WEAK_WARNING;
    }

    /**
     * Get the ProblemHighlightType for an annotation
     * @param annotation - the annotation we want to inspect
     * @return the corresponding ProblemHighlightType value
     */
    private ProblemHighlightType getProblemHighlightTypeForViolation(CodeInspectionAnnotation annotation) {
        if (annotation.getSeverity().isPresent()) {
            switch (annotation.getSeverity().get().intValue()) {
                case 1:
                    return ProblemHighlightType.ERROR;
                case 2:
                    return ProblemHighlightType.WARNING;
                default:
                    return ProblemHighlightType.WEAK_WARNING;
            }
        }
        return ProblemHighlightType.WEAK_WARNING;
    }

    /**
     * Create all the UI elements to create an annotation.
     * @param psiFile - the file to annotate
     * @param annotations - the list of annotations previously reported by doAnnotate
     * @param holder object to add annotations
     */
    @Override
    public void apply(
        @NotNull PsiFile psiFile,
        List<CodeInspectionAnnotation> annotations,
        @NotNull AnnotationHolder holder) {
        // No annotation = nothing to do, just return now. If not enabled for this project, we return no annotations
        // and will stop here.
        if (annotations == null || annotations.isEmpty()) {
            return;
        }

        ProjectSettingsState settings = ProjectSettingsState.getInstance(psiFile.getProject());
        Long projectId = settings.projectId;

        LOGGER.debug(String.format("Received %s annotations", annotations.size()));
        for (CodeInspectionAnnotation annotation : annotations) {
            final String message = String.format("%s (%s)", annotation.getMessage(), ANNOTATION_PREFIX);

            AnnotationBuilder annotationBuilder = holder
                .newAnnotation(getHighlightSeverityForViolation(annotation), message)
                .highlightType(getProblemHighlightTypeForViolation(annotation))
                .range(annotation.range());

            /*
             * We create two fixes to ignore the violation (if possible): one to ignore the violation
             * at the file level (with Optional.of(filename)) and one without the file (with Optional.empty()).
             */
            if (annotation.getRule().isPresent() && annotation.getTool().isPresent() && annotation.getDescription().isPresent() && annotation.getLanguage().isPresent()) {
                LOGGER.debug("Adding fix for annotation");
                annotationBuilder = annotationBuilder
                    .withFix(
                        new CodeInspectionAnnotationFixIgnore(
                            projectId, Optional.of(annotation.getFilename()), annotation.getRule().get(), annotation.getLanguage().get(), annotation.getTool().get()))
                    .withFix(
                        new CodeInspectionAnnotationFixIgnore(
                            projectId, Optional.empty(), annotation.getRule().get(), annotation.getLanguage().get(), annotation.getTool().get()));
            }

            /*
             * If there is a URL associated with the rule, add an action to send the user to the description
             * of the rule.
             */
            if (annotation.getRuleUrl().isPresent()) {
                annotationBuilder = annotationBuilder.withFix(new CodeInspectionAnnotationFixLearnMore(annotation.getRuleUrl().get()));
            }

            annotationBuilder = annotationBuilder.withFix(new CodeInspectionAnnotationFixOpenBrowser(projectId, annotation.getAnalysisId(), annotation.getFilename()));

            annotationBuilder.create();
        }
    }
}
