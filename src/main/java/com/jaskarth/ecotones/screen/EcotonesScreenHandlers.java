package com.jaskarth.ecotones.screen;

import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import com.jaskarth.ecotones.Ecotones;
import com.jaskarth.ecotones.util.RegistryReport;

public final class EcotonesScreenHandlers {
    public static final ScreenHandlerType<SapDistilleryScreenHandler> SAP_DISTILLERY = register("sap_distillery", SapDistilleryScreenHandler::new);
    public static final ScreenHandlerType<FertilizerSpreaderScreenHandler> FERTILIZER_SPREADER = register("fertilizer_spreader", FertilizerSpreaderScreenHandler::new);
    public static final ScreenHandlerType<GrindstoneScreenHandler> GRINDSTONE = register("grindstone", GrindstoneScreenHandler::new);

    public static void init() {
        // NO-OP due to registering in clinit
    }

    private static <T extends ScreenHandler> ScreenHandlerType<T> register(String name, ScreenHandlerRegistry.SimpleClientHandlerFactory<T> screenHandler) {
        RegistryReport.increment("Screen Handler");
        return ScreenHandlerRegistry.registerSimple(Ecotones.id(name), screenHandler);
    }
}
