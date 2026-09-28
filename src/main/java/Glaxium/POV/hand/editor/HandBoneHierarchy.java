package Glaxium.POV.hand.editor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import mchorse.bbs_mod.cubic.IBoneHierarchy;

public class HandBoneHierarchy implements IBoneHierarchy {
   private final IBoneHierarchy delegate;
   private final Set<String> roots;
   private final Set<String> allHandBones;

   public HandBoneHierarchy(IBoneHierarchy delegate, HandBoneUtils.HandBones handBones) {
      this.delegate = delegate;
      this.allHandBones = handBones != null ? handBones.depths().keySet() : Collections.emptySet();
      this.roots = new LinkedHashSet<>();
      if (handBones != null) {
         if (handBones.mainRoot() != null && this.allHandBones.contains(handBones.mainRoot())) {
            this.roots.add(handBones.mainRoot());
         }

         if (handBones.offRoot() != null && this.allHandBones.contains(handBones.offRoot())) {
            this.roots.add(handBones.offRoot());
         }
      }

      if (this.roots.isEmpty()) {
         this.roots.addAll(this.allHandBones);
      }
   }

   public Collection<String> getRootGroupKeys() {
      return this.roots;
   }

   public Collection<String> getDirectChildrenKeys(String key) {
      if (this.delegate != null && this.allHandBones.contains(key)) {
         List<String> children = new ArrayList<>();

         for (String child : this.delegate.getDirectChildrenKeys(key)) {
            if (this.allHandBones.contains(child)) {
               children.add(child);
            }
         }

         return children;
      } else {
         return Collections.emptyList();
      }
   }

   public String getParentGroupKey(String key) {
      if (!this.roots.contains(key) && this.allHandBones.contains(key) && this.delegate != null) {
         String parent = this.delegate.getParentGroupKey(key);
         return this.allHandBones.contains(parent) ? parent : null;
      } else {
         return null;
      }
   }
}
