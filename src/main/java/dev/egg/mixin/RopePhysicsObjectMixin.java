package dev.egg.mixin;

import dev.egg.DimensionalSable;
import dev.egg.RopePhysicsObjectAccessor;
import dev.egg.SubLevelConnectionManager;
import dev.ryanhcode.sable.api.physics.object.rope.RopeHandle;
import dev.ryanhcode.sable.api.physics.object.rope.RopePhysicsObject;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(RopePhysicsObject.class)
public abstract class RopePhysicsObjectMixin implements RopePhysicsObjectAccessor {

    @Shadow protected RopeHandle handle;

    @Unique private UUID dimensionalSable$startLevel;
    @Unique private UUID dimensionalSable$endLevel;

    @Unique
    public UUID dimensionalSable$startLevel() { return dimensionalSable$startLevel; }
    @Unique
    public UUID dimensionalSable$endLevel() {
        return dimensionalSable$endLevel;
    }

    @Inject(method = "setAttachment", at = @At("HEAD"))
    private void dimensionalsable$captureAttachment(RopeHandle.AttachmentPoint attachmentPoint, Vector3dc location, ServerSubLevel subLevel, CallbackInfo ci)
    {
        if (subLevel == null) return;

        if (attachmentPoint == RopeHandle.AttachmentPoint.START)
            dimensionalSable$startLevel = subLevel.getUniqueId();
        else
            dimensionalSable$endLevel = subLevel.getUniqueId();

        if (handle == null || dimensionalSable$startLevel == null || dimensionalSable$endLevel == null) return;

        SubLevelConnectionManager.AddRopeConnection((RopePhysicsObject)(Object)this);

        DimensionalSable.LOGGER.info("Added Rope Connection " + handle);
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void dimensionalsable$onRemove(CallbackInfo ci)
    {
        if (handle == null || dimensionalSable$startLevel == null || dimensionalSable$endLevel == null) return;

        SubLevelConnectionManager.RemoveRopeConnection((RopePhysicsObject)(Object)this);

        DimensionalSable.LOGGER.info("Removed Rope Connection " + handle);
    }
}