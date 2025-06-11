package com.github.ronlievens.regov.task.rewrite.recipes;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

@Setter
@Getter
public class RecipeDependencyService {
    private static RecipeDependencyService instance;

    private boolean mockEnabled;
    private final Map<Class<?>, Object> dependencies;

    private RecipeDependencyService() {
        dependencies = new HashMap<>();
    }

    public static synchronized RecipeDependencyService getInstance() {
        if (instance == null) {
            instance = new RecipeDependencyService();
        }
        return instance;
    }

    private <T> void registerDependency(@NonNull final Class<T> clazz, @NonNull final T implementation) {
        dependencies.put(clazz, implementation);
    }

    @SuppressWarnings("unchecked")
    private <T> T resolveDependency(Class<T> clazz) {
        return (T) dependencies.get(clazz);
    }

    public void setMockEnabled(final boolean mockEnabled) {
        this.mockEnabled = mockEnabled;
        dependencies.clear();
    }

    public <T> T getDependency(@NonNull final Class<T> clazz) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if (!dependencies.containsKey(clazz)) {
            if (mockEnabled) {
                registerDependency(clazz, mock(clazz, withSettings().verboseLogging()));
            } else {
                registerDependency(clazz, clazz.getDeclaredConstructor().newInstance());
            }
        }
        return resolveDependency(clazz);
    }
}
