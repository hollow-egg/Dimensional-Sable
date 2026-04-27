package dev.egg;

import dev.ryanhcode.sable.api.physics.object.rope.RopePhysicsObject;

import java.util.*;

public class SubLevelConnectionManager {
    private static final HashMap<UUID, Vector<RopePhysicsObject>> subLevelRopeMap = new HashMap<>(); //sublevel id to list of ropes
    private static final Set<RopePhysicsObject> ropeSet = new HashSet<>();

    public static Set<RopePhysicsObject> getRopeSet() {
        return ropeSet;
    }

    public static HashMap<UUID, Vector<RopePhysicsObject>> GetConnectedSubLevels(UUID startLevel)
    {
        HashMap<UUID, Vector<RopePhysicsObject>> result = new HashMap<>();
        if (startLevel == null)
            return result;

        Set<UUID> visited = new HashSet<>();
        Deque<UUID> queue = new ArrayDeque<>();

        queue.add(startLevel);
        visited.add(startLevel);

        while (!queue.isEmpty())
        {
            UUID level = queue.poll();

            Vector<RopePhysicsObject> ropes = subLevelRopeMap.getOrDefault(level, new Vector<>());
            result.put(level, ropes);

            if (ropes.isEmpty())
                continue;

            for (RopePhysicsObject rope : ropes)
            {
                RopePhysicsObjectAccessor accessor = (RopePhysicsObjectAccessor) rope;

                UUID start = accessor.dimensionalSable$startLevel();
                UUID end = accessor.dimensionalSable$endLevel();

                UUID next = start.equals(level) ? end : start;

                if (next == null || visited.contains(next))
                    continue;

                visited.add(next);
                queue.add(next);
            }
        }

        return result;
    }

    public static void AddRopeConnection(RopePhysicsObject rope)
    {
        RopePhysicsObjectAccessor ropeAccessor = (RopePhysicsObjectAccessor)rope;

        UUID startLevel = ropeAccessor.dimensionalSable$startLevel();
        UUID endLevel = ropeAccessor.dimensionalSable$endLevel();

        subLevelRopeMap.putIfAbsent(startLevel, new Vector<>());
        subLevelRopeMap.putIfAbsent(endLevel, new Vector<>());

        subLevelRopeMap.get(startLevel).add(rope);
        subLevelRopeMap.get(endLevel).add(rope);

        ropeSet.add(rope);
    }
    public static void RemoveRopeConnection(RopePhysicsObject rope)
    {
        RopePhysicsObjectAccessor ropeAccessor = (RopePhysicsObjectAccessor)rope;

        UUID startLevel = ropeAccessor.dimensionalSable$startLevel();
        UUID endLevel = ropeAccessor.dimensionalSable$endLevel();

        subLevelRopeMap.get(startLevel).remove(rope);
        subLevelRopeMap.get(endLevel).remove(rope);

        if (subLevelRopeMap.get(startLevel).isEmpty())
            subLevelRopeMap.remove(startLevel);
        if (subLevelRopeMap.containsKey(endLevel) && //if attached to self
            subLevelRopeMap.get(endLevel).isEmpty())
            subLevelRopeMap.remove(endLevel);

        ropeSet.remove(rope);
    }
}