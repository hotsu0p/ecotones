package com.jaskarth.ecotones.util.state;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;
import net.minecraft.world.gen.stateprovider.BlockStateProviderType;
import com.jaskarth.ecotones.world.worldgen.features.FeatureHelper;

public class DaffodilProvider extends BlockStateProvider {
    public static final DaffodilProvider INSTANCE = new DaffodilProvider();
    public static final Codec<DaffodilProvider> CODEC = Codec.unit(INSTANCE);

    @Override
    protected BlockStateProviderType<?> getType() {
        return EcotonesBlockStateProviders.DAFFODIL;
    }

    @Override
    public BlockState get(Random random, BlockPos pos) {
        return Registries.BLOCK.get(id("daffodil"))
                .getDefaultState()
                .with(Properties.HORIZONTAL_FACING, FeatureHelper.randomHorizontal(new java.util.Random(random.nextLong())));
    }

    private Identifier id(String daffodil) {
        return new Identifier("aurorasdeco", daffodil);
    }
}
