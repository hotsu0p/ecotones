package com.jaskarth.ecotones.world.entity;

import com.google.common.collect.ImmutableList;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import com.jaskarth.ecotones.world.entity.ai.action.PickBerriesAction;
import com.jaskarth.ecotones.world.entity.ai.action.RoostAtNestAction;
import com.jaskarth.ecotones.world.entity.ai.action.WanderToWaterAction;
import com.jaskarth.ecotones.world.entity.ai.system.*;

import java.util.List;

public class DuckEntity extends ChickenEntity implements ActionableEntity {
    private static final TrackedData<Boolean> ROOSTING = DataTracker.registerData(DuckEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private double food;
    private double energyPoints;
    private int eggLayingTicks;
    private final Ai ai;

    public DuckEntity(EntityType<? extends DuckEntity> entityType, World world) {
        super(entityType, world);

        this.ai = new Ai(ImmutableList.of(), getActions(), new AiState(new Stomach(0, 0, 50, 50, 0.002)));
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(ROOSTING, false);
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityTag) {
        this.food = 25;
        resetEggTimer();

        return super.initialize(world, difficulty, spawnReason, entityData, entityTag);
    }

    @Override
    protected void mobTick() {
        this.ai.tick();

        super.mobTick();

        if (this.eggLayingTicks > 0) {
            this.eggLayingTicks--;
        }

//        Stomach stomach = this.ai.getState().getStomach();
//        this.setCustomName(Text.literal("F: " + stomach.getFood() + " E: " + stomach.getEnergy() + " R: " + stomach.getMetabolismRate()));
    }

    public boolean shouldLayEgg() {
        return this.eggLayingTicks == 0 && this.energyPoints > 8.5;
    }

    public void resetEggTimer() {
        this.eggLayingTicks = this.random.nextInt(6000) + 6000;
    }

    @Override
    protected void initGoals() {
        super.initGoals();
//        this.goalSelector.add(4, new WanderToWaterGoal(this));
//        this.goalSelector.add(3, new RoostAtNestGoal(this));
//        this.goalSelector.add(3, new PickBerriesGoal(this));
    }

    @Override
    public List<? extends Action> getActions() {
        return ImmutableList.of(
                new WanderToWaterAction(this),
                new RoostAtNestAction(this),
                new PickBerriesAction(this)
        );
    }

    public void addFood(double food) {
        this.food += food;

        if (this.food > 50) {
            this.food = 50;
        }
    }

    public boolean exhaustFood(double food) {
        if (this.food > food) {
            this.food -= food;
            return true;
        } else {
            return false;
        }
    }

    public void addEnergyPoints(double energyPoints) {
        this.energyPoints += energyPoints;

        if (this.energyPoints > 50) {
            this.energyPoints = 50;
        }

    }

    public boolean useEnergyPoints(double energyPoints) {
        if (this.energyPoints > energyPoints) {
            this.energyPoints -= energyPoints;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public DuckEntity createChild(ServerWorld world, PassiveEntity passiveEntity) {
        return EcotonesEntities.DUCK.create(world);
    }

    // TODO: roosting gets stuck when reloading world

    public boolean isRoosting() {
        return this.dataTracker.get(ROOSTING);
    }

    public void setRoosting(boolean roosting) {
        this.dataTracker.set(ROOSTING, roosting);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound tag) {
        super.readCustomDataFromNbt(tag);
        setRoosting(tag.getBoolean("roosting"));
        this.food = tag.getDouble("food");
        this.energyPoints = tag.getDouble("energy_points");
        this.eggLayingTicks = tag.getInt("egg_laying_ticks");
        this.ai.deserialize(tag.getCompound("Ai"));
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound tag) {
        super.writeCustomDataToNbt(tag);
        tag.putBoolean("roosting", isRoosting());
        tag.putDouble("food", this.food);
        tag.putDouble("energy_points", this.energyPoints);
        tag.putInt("egg_laying_ticks", this.eggLayingTicks);
        tag.put("Ai", this.ai.serialize(new NbtCompound()));
    }
}
