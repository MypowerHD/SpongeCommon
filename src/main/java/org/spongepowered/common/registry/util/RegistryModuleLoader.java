/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.registry.util;

import static com.google.common.base.Preconditions.checkNotNull;

import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.type.Career;
import org.spongepowered.api.data.type.Careers;
import org.spongepowered.api.data.type.Profession;
import org.spongepowered.api.data.type.Professions;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.common.Sponge;
import org.spongepowered.common.registry.RegistrationPhase;
import org.spongepowered.common.registry.RegistryHelper;
import org.spongepowered.common.registry.RegistryModule;
import org.spongepowered.common.registry.type.BlockTypeRegistryModule;
import org.spongepowered.common.registry.type.CareerRegistryModule;
import org.spongepowered.common.registry.type.ItemTypeRegistryModule;
import org.spongepowered.common.registry.type.ProfessionRegistryModule;
import org.spongepowered.common.registry.type.SoundRegistryModule;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public final class RegistryModuleLoader {

    private RegistryModuleLoader() { }

    @SuppressWarnings("unchecked")
    public static void tryModulePhaseRegistration(RegistryModule module) {
        try {
            if (requiresCustomRegistration(module)) {
                if (isCustomProperPhase(module)) {
                    Method method = getCustomRegistration(module);
                    invokeCustomRegistration(module, checkNotNull(method, "Custom registration module was null!"));

                }
            } else {
                if (hasCatalogRegistration(module) && isDefaultProperPhase(module)) {
                    module.registerDefaults();
                    Map<String, ?> map = getCatalogMap(module);
                    if (map.isEmpty()) {
                        return;
                    }
                    if (module instanceof ItemTypeRegistryModule) {
                        Map<String, ItemType> itemTypeMap = new HashMap<>();
                        for (Map.Entry<String, ItemType> entry : ((Map<String, ItemType>) map).entrySet()) {
                            itemTypeMap.put(entry.getKey().replace("minecraft:", ""), entry.getValue());
                        }
                        RegistryHelper.mapFields(ItemTypes.class, itemTypeMap);
                    } else if (module instanceof BlockTypeRegistryModule) {
                        Map<String, BlockType> blockMap = new HashMap<>();
                        for (Map.Entry<String, BlockType> entry : ((Map<String, BlockType>) map).entrySet()) {
                            blockMap.put(entry.getKey().replace("minecraft:", ""), entry.getValue());
                        }
                        RegistryHelper.mapFields(BlockTypes.class, blockMap);
                    } else {
                        RegistryHelper.mapFields(getCatalogClass(module), map);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error trying to initialize module: " + module.getClass().getCanonicalName(), e);
        }
    }

    private static Method getCustomRegistration(RegistryModule module) {
        for (Method method : module.getClass().getMethods()) {
            CustomCatalogRegistration registration = method.getDeclaredAnnotation(CustomCatalogRegistration.class);
            if (registration != null) {
                return method;
            }
        }
        return null;
    }

    private static boolean requiresCustomRegistration(RegistryModule module) {
        for (Method method : module.getClass().getMethods()) {
            CustomCatalogRegistration registration = method.getDeclaredAnnotation(CustomCatalogRegistration.class);
            if (registration != null) {
                return true;
            }
        }
        return false;
    }


    private static boolean hasCatalogRegistration(RegistryModule module) {
        for (Field field : module.getClass().getDeclaredFields()) {
            RegisterCatalog annotation = field.getAnnotation(RegisterCatalog.class);
            if (annotation != null) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDefaultProperPhase(RegistryModule module) {
        try {
            Method method = module.getClass().getMethod("registerDefaults");
            DelayedRegistration delay = method.getDeclaredAnnotation(DelayedRegistration.class);
            if (delay == null) {
                return Sponge.getRegistry().getPhase() == RegistrationPhase.PRE_REGISTRY;
            } else {
                return Sponge.getRegistry().getPhase() == delay.value();
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static boolean isCustomProperPhase(RegistryModule module) {
        for (Method method : module.getClass().getMethods()) {
            CustomCatalogRegistration registration = method.getDeclaredAnnotation(CustomCatalogRegistration.class);
            DelayedRegistration delay = method.getDeclaredAnnotation(DelayedRegistration.class);
            if (registration != null) {
                if (delay == null) {
                    return Sponge.getRegistry().getPhase() == RegistrationPhase.PRE_REGISTRY;
                } else {
                    return Sponge.getRegistry().getPhase() == delay.value();
                }
            }
        }
        return false;
    }


    @SuppressWarnings("unchecked")
    private static Map<String, ?> getCatalogMap(RegistryModule module) {
        for (Field field : module.getClass().getDeclaredFields()) {
            RegisterCatalog annotation = field.getAnnotation(RegisterCatalog.class);
            if (annotation != null) {
                try {
                    field.setAccessible(true);
                    return (Map<String, ?>) field.get(module);
                } catch (Exception e) {
                    Sponge.getLogger().error("Failed to retrieve a registry field from module: " + module.getClass().getCanonicalName());
                }
            }
        }
        throw new IllegalStateException("Registry module does not have a catalog map! Registry: " + module.getClass().getCanonicalName());
    }

    private static Class<?> getCatalogClass(RegistryModule module) {
        for (Field field : module.getClass().getDeclaredFields()) {
            RegisterCatalog annotation = field.getAnnotation(RegisterCatalog.class);
            if (annotation != null) {
                return annotation.value();
            }
        }
        throw new IllegalArgumentException("The module does not have a registry to register! " + module.getClass().getCanonicalName());
    }

    private static void invokeCustomRegistration(RegistryModule module, Method method) {
        try {
            if (isCustomProperPhase(module)) {
                method.invoke(module);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            Sponge.getLogger().error("Error when calling custom catalog registration for module: "
                                     + module.getClass().getCanonicalName(), e);
        }
    }

    public static void tryAdditionalRegistration(RegistryModule module) {
        Method additionalRegistration = getAdditionalMethod(module);
        if (additionalRegistration != null) {
            try {
                additionalRegistration.invoke(module);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    private static Method getAdditionalMethod(RegistryModule module) {
        for (Method method : module.getClass().getMethods()) {
            AdditionalRegistration registration = method.getDeclaredAnnotation(AdditionalRegistration.class);
            if (registration != null) {
                return method;
            }
        }
        return null;
    }
}
