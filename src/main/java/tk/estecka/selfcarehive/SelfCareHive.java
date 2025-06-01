package tk.estecka.selfcarehive;
//
//import net.fabricmc.api.ModInitializer;
//import net.fabricmc.fabric.api.gamerule.v1.CustomGameRuleCategory;
//import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
//import net.fabricmc.fabric.api.gamerule.v1.rule.DoubleRule;
//import net.minecraft.text.Text;
//import net.minecraft.util.Formatting;
//import net.minecraft.util.Identifier;
//import net.minecraft.world.GameRules;
//import net.minecraft.world.GameRules.BooleanRule;
//import net.minecraft.world.GameRules.IntRule;
//import net.minecraft.world.GameRules.Key;
import net.neoforged.bus.api.IEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import static net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory.createIntRule;
//import static net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory.createBooleanRule;
//import static net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory.createDoubleRule;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(SelfCareHive.MOD_ID)
public class SelfCareHive
{

	public static final String MOD_ID = "selfcarehive";
	public static final Logger LOGGER = LoggerFactory.getLogger("selfcare-hive");

//	static public final GameRules.Category CATEGORY = GameRules.Category.MOBS;
//
//	static public final Key<BooleanRule> CAN_HEAL      = GameRuleRegistry.register("selfcarehive.healing",         CATEGORY, createBooleanRule(true));
//	static public final Key<IntRule> HEALING_COST      = GameRuleRegistry.register("selfcarehive.healing.cost",    CATEGORY, createIntRule(1, 0));
//	static public final Key<DoubleRule> HEALING_AMOUNT = GameRuleRegistry.register("selfcarehive.healing.potency", CATEGORY, createDoubleRule(2.0, 0.0));
//
//	static public final Key<BooleanRule> CAN_BREED     = GameRuleRegistry.register("selfcarehive.breeding",          CATEGORY, createBooleanRule(true));
//	static public final Key<IntRule> BREEDING_COST     = GameRuleRegistry.register("selfcarehive.breeding.cost",     CATEGORY, createIntRule(5, 0));
//	static public final Key<IntRule> TRACKING_DURATION = GameRuleRegistry.register("selfcarehive.tracking.duration", CATEGORY, createIntRule(12_000, 0));
	public SelfCareHive(IEventBus modEventBus, ModContainer modContainer) {
		modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
	}
}