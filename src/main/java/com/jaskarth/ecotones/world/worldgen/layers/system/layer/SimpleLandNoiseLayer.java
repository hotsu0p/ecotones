package com.jaskarth.ecotones.world.worldgen.layers.system.layer;

import com.jaskarth.ecotones.world.worldgen.layers.system.layer.type.IdentitySamplingLayer;
import com.jaskarth.ecotones.world.worldgen.layers.system.layer.util.LayerRandomnessSource;

public enum SimpleLandNoiseLayer implements IdentitySamplingLayer {
   INSTANCE;

   @Override
   public int sample(LayerRandomnessSource context, int value) {
      return BiomeLayers.isShallowOcean(value) ? value : context.nextInt(299999) + 2;
   }
}
