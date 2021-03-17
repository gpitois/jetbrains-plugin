package com.code_inspector.plugins.intellij.ui;

public class UIConstants {
    public final static String SETTINGS_BUTTON_REMOVE_IGNORED_VIOLATION = "Remove Selected";
    public final static String SETTINGS_LABEL_REMOVE_IGNORED_VIOLATION = "Ignored Violations";
    public final static String SETTINGS_LABEL_CONFIGURE_PROJECT = "Configure Project on Code Inspector";
    public final static String SETTINGS_BUTTON_CONFIGURE_PROJECT = "Configure Project";
    public final static String SETTINGS_TEST_API_BUTTON_TEXT = "Test API connection";
    public final static String SETTINGS_GET_API_KEYS_BUTTON_TEXT = "Get API keys";
    public final static String SETTINGS_ACCESS_KEY_LABEL = "Access Key:";
    public final static String SETTINGS_SECRET_KEY_LABEL = "Secret Key:";
    public final static String SETTINGS_CHOOSE_PROJECT = "Choose Project:";
    public final static String SETTINGS_IS_ENABLED = "Enable Code Inspector for this project";

    public final static String API_STATUS_TITLE = "API Connection status";
    public final static String API_STATUS_TEXT_OK = "Connection Successful";
    public final static String API_STATUS_TEXT_FAIL =
            "Connection Failed. Check your access/secret keys and make sure you Apply changes before testing";
    public final static String SETTINGS_API_NOT_WORKING =
        "API is not configured correctly. Check your access and secret keys";

    public final static String ANNOTATION_PREFIX = "Code Inspector";

    public final static String ANNOTATION_FIX_IGNORE_PROJECT = "Ignore this violation for this project";
    public final static String ANNOTATION_FIX_IGNORE_FILE = "Ignore this violation for this file only";
    public final static String ANNOTATION_FIX_OPEN_BROWSER = "See this violation on code inspector";
    public final static String ANNOTATION_FIX_URL_LEARN_MORE = "Learn more about this violation";
}
