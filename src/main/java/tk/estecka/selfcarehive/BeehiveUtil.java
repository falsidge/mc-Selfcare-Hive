package tk.estecka.selfcarehive;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

public class BeehiveUtil
{
	static public BlockState	SetHoneyLevel(int honey, Level world, BlockState hiveState, BlockPos hivePos){
		hiveState = hiveState.setValue(BeehiveBlock.HONEY_LEVEL, honey);
		world.setBlockAndUpdate(hivePos, hiveState);
		return hiveState;
	}

	static public BlockState TryHeal(Bee bee, Level world, BlockState hiveState, BlockPos hivePos){

		boolean canHeal = Config.can_heal;
		int cost = Config.healing_cost;
		float potency = (float)Config.healing_potency;

		int honey = BeehiveBlockEntity.getHoneyLevel(hiveState);
		boolean isHurt = bee.getHealth() < bee.getMaxHealth();
		boolean willOverheal = (bee.getHealth() + potency) >= bee.getMaxHealth();
		boolean willOverflow = bee.hasNectar() && honey >= BeehiveBlock.MAX_HONEY_LEVELS;

		if (canHeal && isHurt && honey>=cost && (willOverflow || !willOverheal)){
			bee.heal(potency);
			return SetHoneyLevel(honey-cost, world, hiveState, hivePos);
		}
		else
			return hiveState;
	}

	static public Pair<@Nullable Bee, BlockState>	TryCreateBaby(Bee parent, IBeeColonyTracker colony, ServerLevel world, BlockState hiveState, BlockPos hivePos){
		boolean canBreed = Config.can_breed;
		int cost = Config.breeding_cost;

		int honey = BeehiveBlockEntity.getHoneyLevel(hiveState);

		// colony.selfcarehive$LogColony();
		if (canBreed
		&&  honey >= cost
		&&  parent.getAge() == 0 // Checks both adulthood and breeding cooldown.
		&&  !colony.selfcarehive$isColonyFull()
		){
			Bee baby = parent.getBreedOffspring(world, parent);
			baby.setBaby(true);
			baby.setPos(parent.position());
			parent.resetLove();
			parent.setAge(6000);
			hiveState = SetHoneyLevel(honey-cost, world, hiveState, hivePos);
			colony.selfcarehive$RememberBee(baby.getUUID());
			return Pair.of(baby, hiveState);
		}
		else
			return Pair.of(null, hiveState);
	}
}
