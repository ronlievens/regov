package com.github.ronlievens.regov.task.config;

import com.github.ronlievens.regov.task.config.model.SettingModel;
import com.github.ronlievens.regov.util.PropertyUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Settings {

    private static Settings instance;

    private SettingModel properties;

    public synchronized static Settings getInstance(final String profile) {
        if (instance == null) {
            instance = new Settings();
            instance.properties = PropertyUtils.load(profile);
        }
        return instance;
    }

    public static SettingModel properties() {
        return instance.properties;
    }
}
