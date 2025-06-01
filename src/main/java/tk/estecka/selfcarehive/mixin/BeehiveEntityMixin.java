package tk.estecka.selfcarehive.mixin;

import java.util.*;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.joml.Math;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import tk.estecka.selfcarehive.BeehiveUtil;
import tk.estecka.selfcarehive.Config;
import tk.estecka.selfcarehive.IBeeColonyTracker;
import tk.estecka.selfcarehive.SelfCareHive;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.animal.Bee;

import static  net.minecraft.world.level.block.entity.BeehiveBlockEntity.MAX_OCCUPANTS;


@Unique
@Mixin(net.minecraft.world.level.block.entity.BeehiveBlockEntity.class)
public class BeehiveEntityMixin
extends BlockEntity
implements IBeeColonyTracker
{
	static private final String KNOWNBEES_KEY = "selfcare-hive:KnownBees";

	/**
	 * The UUID of bees that have left the nest, and the amount of ticks since
	 * they left. These values are only updated during garbage collection.
	 */
	private final Map<UUID,Long> knownBees = new HashMap<>(MAX_OCCUPANTS + 1);
	
	/**
	 * Ticks since the previous garbage collection.
	 */
	private long elapsedTicks = 0;


	private BeehiveEntityMixin(){ super(null, null, null); }
	@Shadow public int	getOccupantCount(){ throw new AssertionError(); }


/******************************************************************************/
/* # Colony Tracker                                                           */
	@Shadow @Final public static int MAX_OCCUPANTS;

	/******************************************************************************/

	private void GarbageCollectBees() {
		// Updates absence times, and removes bees that are deemed missing.
		final int maxAbsence = Config.tracking_duration;
		var iterator = knownBees.entrySet().iterator();
		while (iterator.hasNext()) {
			var entry = iterator.next();
			long absenceTime = this.elapsedTicks + entry.getValue();

			if (absenceTime < maxAbsence)
				entry.setValue(absenceTime);
			else {
				iterator.remove();
				if (Config.debugMode)
					SelfCareHive.LOGGER.warn("A bee has gone missing: {}", entry.getKey());
			}
		}
		this.elapsedTicks = 0;

		// Removes bees that were pushed out by new inhabitants.
		final int maxKnownBees = Math.max(0, MAX_OCCUPANTS - this.getOccupantCount());
		if (knownBees.size() > maxKnownBees) {
			// Sorts from newest (smallest) to oldest (largest)
			final var sortedEntries = new ArrayList<>(knownBees.entrySet());
			sortedEntries.sort(Comparator.comparingLong(Map.Entry::getValue));

			// Skips the first few bees (the newest), removes the rest.
			for (int i=maxKnownBees; i<sortedEntries.size(); ++i){
				if (Config.debugMode)
					SelfCareHive.LOGGER.warn("Superfluous bee was pruned: {}", sortedEntries.get(i).getKey());
				this.knownBees.remove(sortedEntries.get(i).getKey());
			}
		}
	}

	public void selfcarehive$LogColony(){
		StringBuilder string = new StringBuilder();
		string.append("Inside: ").append(this.getOccupantCount())
		      .append(", Outside: ").append(this.knownBees.size())
		      ;

		for (var entry : this.knownBees.entrySet())
			string.append("\n - ").append(entry.getKey()).append(' ').append(entry.getValue());

		SelfCareHive.LOGGER.info(string.toString());
	}

	public boolean selfcarehive$isColonyFull(){
		this.GarbageCollectBees();
		return (this.getOccupantCount() + this.knownBees.size()) >= MAX_OCCUPANTS;
	}

	public void selfcarehive$RememberBee(UUID uuid){
		knownBees.put(uuid, 0L);
	}


/******************************************************************************/
/* # Serialization                                                            */
/******************************************************************************/

	@Inject( method="saveAdditional", at=@At("TAIL") )
	private void WriteCustomNBT(CompoundTag nbt, HolderLookup.Provider registries, CallbackInfo ci){
		if (!knownBees.isEmpty()){
			CompoundTag list = new CompoundTag();
			for (var entry : knownBees.entrySet())
				list.putLong(entry.getKey().toString(), entry.getValue());
			nbt.put(KNOWNBEES_KEY, list);
		}
	}

	@Inject( method="loadAdditional", at=@At("TAIL") )
	private void ReadCustomNBT(CompoundTag nbt, HolderLookup.Provider registries, CallbackInfo ci){
		if (nbt.contains(KNOWNBEES_KEY, Tag.TAG_COMPOUND)){
			CompoundTag list = nbt.getCompound(KNOWNBEES_KEY);
			for (String key : list.getAllKeys()){
				UUID uuid;
				long time;
				try {
					uuid = UUID.fromString(key);
					time = list.getLong(key);
				} catch (IllegalArgumentException|ClassCastException e){
					SelfCareHive.LOGGER.error("Invalid last-seen data in behive at {}:\nKey: {}, Value:\n{}", this.worldPosition, key, nbt.get(key).getAsString());
					continue;
				}
				knownBees.put(uuid, time);
			}
		}
	}


/******************************************************************************/
/* # Lifecycle                                                                */
/******************************************************************************/

	@Inject(method="serverTick", at=@At("HEAD"))
	static private void tick(Level world, BlockPos pos, BlockState state, BeehiveBlockEntity blockEntity, CallbackInfo info){
		++((BeehiveEntityMixin)(Object)blockEntity).elapsedTicks;
	}

	@Inject(
		require = 1,
		method = {
			"Lnet/minecraft/world/level/block/entity/BeehiveBlockEntity;addOccupant(Lnet/minecraft/world/entity/Entity;)V", // 1.20.5
//			"method_21848(Lnet/minecraft/entity/passive/BeeEntity;)V" // 1.21.4
		},
		at = @At("TAIL")
	)
	private void OnBeeEntrance(@Coerce Entity bee, CallbackInfo ci){
		UUID uuid = bee.getUUID();
		if (Config.debugMode && !this.knownBees.containsKey(uuid))
			SelfCareHive.LOGGER.warn("An unknown bee joined the hive: {}", uuid);
		// Bees lose their UUID when returning to the nest.
		this.knownBees.remove(uuid);
	}

	/**
	 * @implNote At this point, the BeeEntity that is being released has not yet
	 * been  removed  from the  hive's  own internal  counter. For  this  reason
	 * `tryCreateBaby` must  be called  BEFORE `rememberBee`, otherwise  it will
	 * count one bee too many, and refuse to create an offspring.
	 * 
	 * @implNote This handler is intentionally injected  before the released bee
	 * has deposited its nectar, so that bee may attempt to consume it first and
	 * avoid  overflow. However, the  bee's  position  in the  world  is not yet
	 * properly set, so babies  need to have  their position  updated at a later
	 * time.
	 */
	@ModifyExpressionValue( method="releaseOccupant", expect=1, at=@At(value="INVOKE", target="Lnet/minecraft/world/level/block/entity/BeehiveBlockEntity$Occupant;createEntity(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/entity/Entity;") )
	static private Entity OnBeeEntityCreated(Entity original, Level world, BlockPos pos, @Local(argsOnly=true) LocalRef<BlockState> stateRef, @Share("baby") LocalRef<Bee> babyRef)
	{
		if (original instanceof Bee bee && !world.isClientSide && world.getBlockEntity(pos) instanceof BeehiveBlockEntity hive){
			IBeeColonyTracker colony = IBeeColonyTracker.Of(hive);
			BlockState hiveState = stateRef.get();
			Bee baby = null;

			var result = BeehiveUtil.TryCreateBaby(bee, colony, (ServerLevel) world, hiveState, pos);
			baby = result.getLeft();
			hiveState = result.getRight();

			hiveState = BeehiveUtil.TryHeal(bee, world, hiveState, pos);

			colony.selfcarehive$RememberBee(bee.getUUID());

			babyRef.set(baby);
			stateRef.set(hiveState);
		}
		
		return original;
	}

	@WrapOperation( method="releaseOccupant", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/Entity;moveTo(DDDFF)V") )
	static private void	OnBeePositionUpdated(Entity bee, double x, double y, double z, float yaw, float pitch, Operation<Void> original, @Share("baby") LocalRef<Bee> baby){
		Bee babyEntity = baby.get();
		if (babyEntity != null){
			babyEntity.moveTo(x, y, z, yaw, pitch);
			babyEntity.level().addFreshEntity(babyEntity);
		}

		original.call(bee, x, y, z, yaw, pitch);
	}

	/**
	 * Makes bees leave the nest instantly for testing purposes
	 */
	@ModifyArg(
		require = 1,
		method = {
			"Lnet/minecraft/world/level/block/entity/BeehiveBlockEntity;addOccupant(Lnet/minecraft/world/entity/Entity;)V", // 1.20.5
		},
		at=@At( value="INVOKE", target="Lnet/minecraft/world/level/block/entity/BeehiveBlockEntity;storeBee(Lnet/minecraft/world/level/block/entity/BeehiveBlockEntity$Occupant;)V" )
	)
	private BeehiveBlockEntity.Occupant ReduceExitDelay(BeehiveBlockEntity.Occupant original){
		if (!Config.debugMode)
			return original;

		return new BeehiveBlockEntity.Occupant(original.entityData(), 0, 20);
	}
}
