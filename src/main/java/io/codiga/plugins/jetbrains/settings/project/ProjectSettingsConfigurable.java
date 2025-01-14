package io.codiga.plugins.jetbrains.settings.project;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static io.codiga.plugins.jetbrains.Constants.LOGGER_NAME;

public class ProjectSettingsConfigurable implements Configurable {
    private ProjectSettingsComponent mySettingsComponent;
    private final Project project;

    public static final Logger LOGGER = Logger.getInstance(LOGGER_NAME);

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Codiga Project Settings";
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return mySettingsComponent.getPreferredFocusedComponent();
    }

    public ProjectSettingsConfigurable(Project p){
        this.project = p;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        mySettingsComponent = new ProjectSettingsComponent();
        return mySettingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        ProjectSettingsState settings = ProjectSettingsState.getInstance(project);
        LOGGER.debug(settings.isEnabled.toString());
        LOGGER.debug(this.mySettingsComponent.isEnabled().toString());
        return (
            !this.mySettingsComponent.getSelectedProjectId().equals(settings.projectId) ||
            !this.mySettingsComponent.isProjectAssociated().equals(settings.isProjectAssociated) ||
            !this.mySettingsComponent.isEnabled().equals(settings.isEnabled)
        );
    }

    @Override
    public void apply() {
        ProjectSettingsState settings = ProjectSettingsState.getInstance(project);
        settings.projectId = this.mySettingsComponent.getSelectedProjectId();
        settings.isEnabled = this.mySettingsComponent.isEnabled();
        settings.isProjectAssociated = this.mySettingsComponent.isProjectAssociated();
    }

    @Override
    public void reset() {
        ProjectSettingsState settings = ProjectSettingsState.getInstance(project);
        this.mySettingsComponent.setSelectedProjectId(settings.projectId);
        this.mySettingsComponent.setIsProjectAssociatedCheckbox(settings.isProjectAssociated);
        this.mySettingsComponent.setIsEnabledCheckbox(settings.isEnabled);
    }

    @Override
    public void disposeUIResources() {
        mySettingsComponent = null;
    }

}
