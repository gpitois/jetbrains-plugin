package io.codiga.plugins.jetbrains.annotators;

import io.codiga.api.GetFileAnalysisQuery;
import io.codiga.plugins.jetbrains.cache.AnalysisDataCache;
import io.codiga.plugins.jetbrains.graphql.GraphQlQueryException;
import io.codiga.plugins.jetbrains.settings.application.AppSettingsState;
import io.codiga.plugins.jetbrains.settings.project.ProjectSettingsState;
import com.google.common.collect.ImmutableList;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

import static io.codiga.plugins.jetbrains.Constants.INVALID_PROJECT_ID;
import static io.codiga.plugins.jetbrains.Constants.LOGGER_NAME;
import static io.codiga.plugins.jetbrains.graphql.ApiUtils.getAnnotationsFromFileAnalysisQueryResult;
import static io.codiga.plugins.jetbrains.parameters.AnalysisParameters.getAnalysisParameters;
import static io.codiga.plugins.jetbrains.ui.UIConstants.ANNOTATION_PREFIX;

public class ExternalAnnotator extends com.intellij.lang.annotation.ExternalAnnotator<PsiFile, List<Annotation>> {

    public static final Logger LOGGER = Logger.getInstance(LOGGER_NAME);

    @Nullable
    public PsiFile collectInformation(@NotNull PsiFile file) {
        return file;
    }

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
     * Get annotations for a file
     * @param psiFile - the file opened in IntelliJ
     * @param projectId - the related projectId if selected in IntelliJ
     * @return - the list of annotations to add.
     */
    @Nullable
    private List<Annotation> getAnnotationFromFileAnalysis(PsiFile psiFile, Optional<Long> projectId) {
        final String filename = psiFile.getName();
        final String code = psiFile.getText();

        LOGGER.debug(String.format("calling doAnnotate on file %s, type %s", filename, psiFile.getLanguage()));

        if (code.isEmpty()) {
            return ImmutableList.of();
        }

        final long startAnalysisTimeMillis = System.currentTimeMillis();

        /**
         * TODO: add the parameters
         */
        Optional<String> parameters = getAnalysisParameters(psiFile);
        if(parameters.isPresent()){
            LOGGER.info(String.format("parameters: %s", parameters.get()));
        } else {
            LOGGER.info("parameters absent");
        }
        Optional<GetFileAnalysisQuery.GetFileAnalysis> queryResult;
        try {
            queryResult = AnalysisDataCache
                .getInstance()
                .getViolationsFromFileAnalysis(projectId, filename, code, parameters);
        } catch (GraphQlQueryException e) {
            LOGGER.debug("receive invalid graphql call, sending notification");
            queryResult = Optional.empty();
        }

        final long endAnalysisTimeMillis = System.currentTimeMillis();

        LOGGER.debug(String.format("Analysis time for file %s: %s ms",
            filename,
            endAnalysisTimeMillis - startAnalysisTimeMillis));

        if (queryResult.isPresent()){
            List<Annotation> res = getAnnotationsFromFileAnalysisQueryResult(
                queryResult.get(), psiFile, projectId);
            LOGGER.debug(String.format("number of annotations for file %s: %s", filename, res.size()));
            return res;
        } else {
            LOGGER.debug(String.format("No result for file %s", filename));
            return ImmutableList.of();
        }

    }


    /**
     * Gather all the annotations from the Codiga API and generates a list of annotation
     * to surface later in the UI.
     * @param psiFile - the file to inspect.
     * @return the list of annotation to surface.
     */
    @Nullable
    @Override
    public List<Annotation> doAnnotate(PsiFile psiFile) {
        LOGGER.info(String.format("calling doAnnotate on file: %s", psiFile.getName()));

        final ProjectSettingsState PROJECT_SETTINGS = ProjectSettingsState.getInstance(psiFile.getProject());

        final ProjectSettingsState settings = ProjectSettingsState.getInstance(psiFile.getProject());

        if (!settings.isEnabled) {
            LOGGER.debug("Codiga is disabled on this project");
            return ImmutableList.of();
        }

        ProgressManager.checkCanceled();

        Optional<Long> projectId = Optional.empty();
        if(PROJECT_SETTINGS.isProjectAssociated && !settings.projectId.equals(INVALID_PROJECT_ID)) {
            projectId = Optional.of(settings.projectId);
        }
        return getAnnotationFromFileAnalysis(psiFile, projectId);
    }

    /**
     * Get the HighlightSeverity for an annotation
     * @param annotation - the annotation we want to inspect
     * @return the corresponding HighlightSeverity value
     */
    private HighlightSeverity getHighlightSeverityForViolation(Annotation annotation) {
        if (annotation.getSeverity().isPresent()) {
            switch (annotation.getSeverity().get().intValue()) {
                case 1:
                    return HighlightSeverity.ERROR;
                case 2:
                    return HighlightSeverity.WARNING;
                case 3:
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
    private ProblemHighlightType getProblemHighlightTypeForViolation(Annotation annotation) {
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
     * Generate an annotation for a violation
     * @param psiFile - the file to annotate
     * @param annotation - the annotation we need
     * @param holder - the holder of the annotation
     */
    private void generateAnnotationForViolation(
        @NotNull final PsiFile psiFile,
        @NotNull final Annotation annotation,
        @NotNull AnnotationHolder holder) {

        AppSettingsState settings = AppSettingsState.getInstance();

        final Optional<Long> projectId = annotation.getProjectId();

        final String message = String.format("%s (%s)", annotation.getMessage(), ANNOTATION_PREFIX);

        final TextRange textRange = psiFile.getTextRange();

        if (!textRange.contains(annotation.range().getEndOffset()) ||
            !textRange.contains(annotation.range().getStartOffset())) {
            LOGGER.debug("range outside of the scope");
            return;
        }

        AnnotationBuilder annotationBuilder = holder
            .newAnnotation(getHighlightSeverityForViolation(annotation), message)
            .highlightType(getProblemHighlightTypeForViolation(annotation))
            .range(annotation.range());

        /*
         * We create two fixes to ignore the violation (if possible): one to ignore the violation
         * at the file level (with Optional.of(filename)) and one without the file (with Optional.empty()).
         */
        if (annotation.getRule().isPresent() && annotation.getTool().isPresent() &&
            annotation.getDescription().isPresent() && annotation.getLanguage().isPresent() &&
            projectId.isPresent() && (settings.hasApiKeys() || settings.hasApiToken())) {
            LOGGER.debug("Adding fix for annotation");
            annotationBuilder = annotationBuilder
                .withFix(
                    new AnnotationFixIgnore(
                        psiFile, projectId.get(), Optional.of(annotation.getFilename()), annotation.getRule().get(),
                        annotation.getLanguage().get(), annotation.getTool().get()))
                .withFix(
                    new AnnotationFixIgnore(
                        psiFile, projectId.get(), Optional.empty(), annotation.getRule().get(),
                        annotation.getLanguage().get(), annotation.getTool().get()));
        }

        /*
         * If there is a URL associated with the rule, add an action to send the user to the description
         * of the rule.
         */
        if (annotation.getRuleUrl().isPresent()) {
            annotationBuilder = annotationBuilder.withFix(
                new AnnotationFixLearnMore(annotation.getRuleUrl().get()));
        }

        if (projectId.isPresent() && annotation.getAnalysisId().isPresent()) {
            annotationBuilder = annotationBuilder.withFix(
                new AnnotationFixOpenBrowser(
                    projectId.get(),
                    annotation.getAnalysisId().get(),
                    annotation.getFilename()));

        }

        annotationBuilder.create();
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
        List<Annotation> annotations,
        @NotNull AnnotationHolder holder) {
        // No annotation = nothing to do, just return now. If not enabled for this project, we return no annotations
        // and will stop here.
        if (annotations == null || annotations.isEmpty()) {
            return;
        }

        LOGGER.debug(String.format("Received %s annotations", annotations.size()));
        for (Annotation annotation : annotations) {
            generateAnnotationForViolation(psiFile, annotation, holder);
        }
    }
}
