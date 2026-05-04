package dev.egg;

import dev.ryanhcode.sable.api.physics.constraint.fixed.FixedConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.fixed.FixedConstraintHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.Vector;

public class SubLevelLockManager {
    private final static Vector<Lock> locks = new Vector<>();

    public static void AddLock(ServerSubLevel subLevel, double time) {
        var system = SubLevelPhysicsSystem.get(subLevel.getLevel());
        var handle = system.getPipeline().addConstraint(null, subLevel, new FixedConstraintConfiguration(
                subLevel.logicalPose().position(),
                subLevel.logicalPose().rotationPoint(),
                subLevel.logicalPose().orientation()
        ));
        locks.add(new Lock(handle, time));
    }
    private static class Lock{
        public Lock(FixedConstraintHandle handle, double time) {
            this.handle = handle;
            this.time = time;
        }
        FixedConstraintHandle handle;
        double time;
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide()) return;

        for (int i = locks.size() - 1; i >= 0; i--) {
            var lock = locks.elementAt(i);
            lock.time-=1.0/20.0;
            if (lock.time <= 0) {
                lock.handle.remove();
                locks.remove(lock);
            }
        }
    }
}
