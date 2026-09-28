package Glaxium.POV.hand.editor;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.RigBone;
import mchorse.bbs_mod.cubic.model.ArmorSlot;

public final class HandBoneUtils {
   private HandBoneUtils() {
   }

   public static HandBoneUtils.HandBones collect(ModelInstance model) {
      if (model != null && model.getModel() != null) {
         String mainRoot = getRoot(model.getFpMain(), "right_arm");
         String offRoot = getRoot(model.getFpOffhand(), "left_arm");
         Set<String> roots = new LinkedHashSet<>();
         roots.add(mainRoot);
         roots.add(offRoot);
         Map<String, Integer> depths = new LinkedHashMap<>();

         for (String name : model.getModel().getGroupKeysInHierarchyOrder()) {
            RigBone bone = model.getModel().getBone(name);
            int depth = handDepth(bone, roots);
            if (depth >= 0) {
               depths.put(name, depth);
            }
         }

         if (depths.isEmpty()) {
            Set<String> disabled = model.getDisabledBones();

            for (String namex : model.getModel().getGroupKeysInHierarchyOrder()) {
               if (disabled == null || !disabled.contains(namex)) {
                  depths.put(namex, 0);
               }
            }
         }

         return new HandBoneUtils.HandBones(mainRoot, offRoot, depths);
      } else {
         return HandBoneUtils.HandBones.EMPTY;
      }
   }

   private static int handDepth(RigBone bone, Set<String> roots) {
      for (int depth = 0; bone != null; depth++) {
         if (roots.contains(bone.getBoneName())) {
            return depth;
         }

         bone = bone.getParentBone();
      }

      return -1;
   }

   private static String getRoot(ArmorSlot slot, String fallback) {
      return slot != null && slot.group != null && !slot.group.isBlank() ? slot.group : fallback;
   }

   public static record HandBones(String mainRoot, String offRoot, Map<String, Integer> depths) {
      private static final HandBoneUtils.HandBones EMPTY = new HandBoneUtils.HandBones("right_arm", "left_arm", Map.of());

      public boolean contains(String bone) {
         return this.depths.containsKey(bone);
      }
   }
}
