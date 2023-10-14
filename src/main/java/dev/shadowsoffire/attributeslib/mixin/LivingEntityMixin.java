package dev.shadowsoffire.attributeslib.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.shadowsoffire.attributeslib.api.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LivingEntity.class, priority = 900)
public abstract class LivingEntityMixin extends Entity {

    @Final
    @Shadow
    private AttributeMap attributes;

    @Shadow
    protected int useItemRemaining;

    public LivingEntityMixin(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    /**
     * @author Shadows
     * @reason Injection of the Sundering potion effect, which is applied during resistance calculations.
     * @param value  Damage modifier percentage after resistance has been applied [1.0, -inf]
     * @param max    Zero
     * @param source The damage source
     * @param damage The initial damage amount
     */
    @Redirect(at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"), method = "getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F")
    public float zenith_sunderingApplyEffect(float value, float max, DamageSource source, float damage) {
        if (this.hasEffect(ALObjects.MobEffects.SUNDERING) && !source.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
            int level = this.getEffect(ALObjects.MobEffects.SUNDERING).getAmplifier() + 1;
            value += damage * level * 0.2F;
        }
        return Math.max(value, max);
    }

    /**
     * @author Shadows
     * @reason Used to enter an if-condition so the above mixin always triggers.
     */
    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/world/effect/MobEffect;)Z"), method = "getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F")
    public boolean zenith_sunderingHasEffect(LivingEntity ths, MobEffect effect) {
        return true;
    }

    /**
     * @author Shadows
     * @reason Used to prevent an NPE since we're faking true on hasEffect
     */
    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffectInstance;getAmplifier()I"), method = "getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F")
    public int zenith_sunderingGetAmplifier(@Nullable MobEffectInstance inst) {
        return inst == null ? -1 : inst.getAmplifier();
    }

    @Shadow
    public abstract boolean hasEffect(MobEffect ef);

    @Shadow
    public abstract MobEffectInstance getEffect(MobEffect ef);

    @ModifyExpressionValue(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterAbsorb(FFF)F"), method = "getDamageAfterArmorAbsorb", require = 1)
    public float zenith_applyArmorPen(float amount, DamageSource src) {
        return ALCombatRules.getDamageAfterArmor((LivingEntity) (Object) this, src, amount,((LivingEntity)(Object) this).getArmorValue(), (float) ((LivingEntity)(Object) this).getAttributeValue(Attributes.ARMOR_TOUGHNESS));
    }

    @ModifyExpressionValue(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterMagicAbsorb(FF)F"), method = "getDamageAfterMagicAbsorb", require = 1)
    public float zenith_applyProtPen(float amount, DamageSource src) {
        return ALCombatRules.getDamageAfterProtection((LivingEntity) (Object) this, src, amount, EnchantmentHelper.getDamageProtection(((LivingEntity)(Object) this).getArmorSlots(), src));
    }

    @ModifyVariable(method = "heal", at = @At(value = "HEAD"))
    private float healEvent(float value){
        float amount = HealEvent.EVENT.invoker().onLivingHeal(this, value);
        //AttributesLib.LOGGER.info("Heal event initialized, old heal {}, new heal {}", value, amount);
        return amount >= 0 ? amount : 0;
    }

    @Inject(method = "updateUsingItem", at = @At("HEAD"))
    private void useItemEvent(ItemStack usingItem, CallbackInfo ci){
        if (!usingItem.isEmpty())
            this.useItemRemaining = UseItemTickEvent.EVENT.invoker().onLivingUse((LivingEntity) (Object) this, usingItem, this.useItemRemaining);
    }

    @Inject(method = "createLivingAttributes", at = @At("RETURN"), require = 1, allow = 1)
    private static void zenith_attributes(CallbackInfoReturnable<AttributeSupplier.Builder> cir){
        cir.getReturnValue().add(ALObjects.Attributes.DRAW_SPEED)
        .add(ALObjects.Attributes.CRIT_CHANCE)
        .add(ALObjects.Attributes.COLD_DAMAGE)
        .add(ALObjects.Attributes.FIRE_DAMAGE)
        .add(ALObjects.Attributes.LIFE_STEAL)
        .add(ALObjects.Attributes.CURRENT_HP_DAMAGE)
        .add(ALObjects.Attributes.OVERHEAL)
        .add(ALObjects.Attributes.GHOST_HEALTH)
        .add(ALObjects.Attributes.MINING_SPEED)
        .add(ALObjects.Attributes.ARROW_VELOCITY)
        .add(ALObjects.Attributes.HEALING_RECEIVED)
        .add(ALObjects.Attributes.ARMOR_PIERCE)
        .add(ALObjects.Attributes.ARMOR_SHRED)
        .add(ALObjects.Attributes.PROT_PIERCE)
        .add(ALObjects.Attributes.PROT_SHRED)
        .add(ALObjects.Attributes.DODGE_CHANCE)
        .add(ALObjects.Attributes.ELYTRA_FLIGHT)
        .add(ALObjects.Attributes.CREATIVE_FLIGHT);
    }

}
