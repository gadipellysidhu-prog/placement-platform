package com.college.placement.shared.settings.domain;

/**
 * The declared type of an {@link AppSetting} value, used to parse the stored
 * string representation into a typed value and to validate updates.
 */
public enum SettingValueType {
    STRING,
    INTEGER,
    LONG,
    BOOLEAN,
    DECIMAL,
    JSON
}
