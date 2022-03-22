package io.codiga.plugins.jetbrains.actions.use_recipe;

import com.intellij.ide.util.gotoByName.ChooseByNameModel;
import com.intellij.ide.util.gotoByName.SimpleChooseByNameModel;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.psi.PsiElement;
import io.codiga.api.GetRecipesForClientSemanticQuery;
import io.codiga.api.type.LanguageEnumeration;
import io.codiga.plugins.jetbrains.graphql.CodigaApi;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.codiga.plugins.jetbrains.actions.ActionUtils.*;

public class RecipeChooseByNameModel implements ChooseByNameModel {

    private final AnActionEvent anActionEvent;
    private final CodeInsertionContext codeInsertionContext;
    private List<GetRecipesForClientSemanticQuery.AssistantRecipesSemanticSearch> recipes = new ArrayList<>();


    public RecipeChooseByNameModel(AnActionEvent anActionEvent, CodeInsertionContext codeInsertionContext) {
        this.anActionEvent = anActionEvent;
        this.codeInsertionContext = codeInsertionContext;
    }



    @Override
    public @Nls(capitalization = Nls.Capitalization.Sentence) String getPromptText() {
        return "Codiga: recipe search";
    }

    @Override
    public @NotNull @NlsContexts.Label String getNotInMessage() {
        return "Recipe not found";
    }

    @Override
    public @NotNull @NlsContexts.Label String getNotFoundMessage() {
        return "Recipes found";
    }

    @Override
    public @Nullable @NlsContexts.Label String getCheckBoxName() {
        return null;
    }

    @Override
    public boolean loadInitialCheckBoxState() {
        return true;
    }

    @Override
    public void saveInitialCheckBoxState(boolean b) {

    }

    @Override
    public @NotNull ListCellRenderer getListCellRenderer() {
        return (list, value, index, isSelected, cellHasFocus) -> {

            if (value instanceof GetRecipesForClientSemanticQuery.AssistantRecipesSemanticSearch){


                GetRecipesForClientSemanticQuery.AssistantRecipesSemanticSearch recipe = (GetRecipesForClientSemanticQuery.AssistantRecipesSemanticSearch) value;
                long recipeId = ((BigDecimal)recipe.id()).longValue();
                if(isSelected && (!codeInsertionContext.getCurrentRecipeId().isPresent() || codeInsertionContext.getCurrentRecipeId().get().longValue() != recipeId)){
                    removeAddedCode(anActionEvent, codeInsertionContext);
                    addRecipeToEditor(anActionEvent,
                        codeInsertionContext,
                        recipe.imports(),
                        recipe.jetbrainsFormat(),
                        recipe.language());
                    codeInsertionContext.setCurrentRecipeId(((BigDecimal)recipe.id()).longValue());

                }
                JLabel newLabel =  new JLabel(recipe.name());

                return newLabel;
            }
            return new JLabel("unknown");
        };
    }

    @Override
    public String @NotNull @Nls [] getNames(boolean b) {
        return new String[0];
    }

    @Override
    public Object @NotNull [] getElementsByName(@NotNull String s, boolean b, @NotNull String s1) {
        return new Object[0];
    }

    @Override
    public @Nullable String getElementName(@NotNull Object value) {
        if (value instanceof GetRecipesForClientSemanticQuery.AssistantRecipesSemanticSearch) {
            GetRecipesForClientSemanticQuery.AssistantRecipesSemanticSearch recipe = (GetRecipesForClientSemanticQuery.AssistantRecipesSemanticSearch) value;
            return recipe.name();
        }
        return null;
    }

    @Override
    public String @NotNull [] getSeparators() {
        return new String[0];
    }

    @Override
    public @Nullable String getFullName(@NotNull Object o) {
        return "fill name";
    }

    @Override
    public @Nullable @NonNls String getHelpId() {
        return "help";
    }

    @Override
    public boolean willOpenEditor() {
        return false;
    }

    @Override
    public boolean useMiddleMatching() {
        return false;
    }
}
