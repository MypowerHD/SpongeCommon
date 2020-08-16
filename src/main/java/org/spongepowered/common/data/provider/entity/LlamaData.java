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
package org.spongepowered.common.data.provider.entity;

import net.minecraft.entity.passive.horse.LlamaEntity;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.LlamaType;
import org.spongepowered.common.accessor.entity.passive.horse.LlamaEntityAccessor;
import org.spongepowered.common.data.provider.DataProviderRegistrator;
import org.spongepowered.common.data.type.SpongeLlamaType;

public final class LlamaData {

    private LlamaData() {
    }

    // @formatter:off
    public static void register(final DataProviderRegistrator registrator) {
        registrator
                .asMutable(LlamaEntity.class)
                    .create(Keys.LLAMA_TYPE)
                        .get(h -> {
                            final int type = h.getVariant()
                                    ;
                            return Sponge.getRegistry().getCatalogRegistry().streamAllOf(LlamaType.class)
                                    .filter(t -> ((SpongeLlamaType)t).getMetadata() == type)
                                    .findFirst().orElse(null);
                        })
                        .set((h, v) -> h.setVariant(((SpongeLlamaType) v).getMetadata()))
                    .create(Keys.STRENGTH)
                        .get(LlamaEntity::getStrength)
                        .set((h, v) -> ((LlamaEntityAccessor) h).accessor$setStrength(v));
    }
    // @formatter:on
}