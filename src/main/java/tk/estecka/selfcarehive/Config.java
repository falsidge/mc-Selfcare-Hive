package tk.estecka.selfcarehive;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = SelfCareHive.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue CAN_HEAL = BUILDER
            .comment("Healing Enabled")
            .define("healing", true);

    private static final ModConfigSpec.IntValue HEALING_COST = BUILDER
            .comment("Healing Cost")
            .defineInRange("healingCost", 1, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.DoubleValue HEALING_POTENCY = BUILDER
            .comment("Healing Amount")
            .defineInRange("healingPotency", 2.0, 0, Double.MAX_VALUE);



    private static final ModConfigSpec.BooleanValue CAN_BREED = BUILDER
            .comment("Breeding Enabled")
            .define("breeding", true);

    private static final ModConfigSpec.IntValue BREEDING_COST = BUILDER
            .comment("Breeding Cost")
            .defineInRange("breedingCost", 5, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue TRACKING_DURATION = BUILDER
            .comment("Tracking Duration")
            .defineInRange("trackingDuration", 12_000, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.BooleanValue IS_DEBUG = BUILDER
            .comment("Debug mode")
            .define("debugMode", false);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean can_heal;
    public static int healing_cost;
    public static double healing_potency;
    public static boolean can_breed;
    public static int breeding_cost;
    public static int tracking_duration;
    public static boolean debugMode;


    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        can_heal = CAN_HEAL.get();
        healing_cost = HEALING_COST.get();
        healing_potency = HEALING_POTENCY.get();

        can_breed = CAN_BREED.get();
        breeding_cost = BREEDING_COST.get();
        tracking_duration = TRACKING_DURATION.get();
        debugMode = IS_DEBUG.get();
    }
}