package Glaxium.POV.hand.editor;

import mchorse.bbs_mod.cubic.IBoneHierarchy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Filtered bone hierarchy view that exposes only first-person hand roots and their child bones.
 */
public class HandBoneHierarchy implements IBoneHierarchy
{
    private final IBoneHierarchy delegate;
    private final Set<String> roots;
    private final Set<String> allHandBones;

    public HandBoneHierarchy(IBoneHierarchy delegate, HandBoneUtils.HandBones handBones)
    {
        this.delegate = delegate;
        this.allHandBones = handBones != null ? handBones.depths().keySet() : Collections.emptySet();
        this.roots = new LinkedHashSet<>();

        if (handBones != null)
        {
            if (handBones.mainRoot() != null && this.allHandBones.contains(handBones.mainRoot()))
            {
                this.roots.add(handBones.mainRoot());
            }
            if (handBones.offRoot() != null && this.allHandBones.contains(handBones.offRoot()))
            {
                this.roots.add(handBones.offRoot());
            }
        }

        if (this.roots.isEmpty())
        {
            this.roots.addAll(this.allHandBones);
        }
    }

    @Override
    public Collection<String> getRootGroupKeys()
    {
        return this.roots;
    }

    @Override
    public Collection<String> getDirectChildrenKeys(String key)
    {
        if (this.delegate == null || !this.allHandBones.contains(key))
        {
            return Collections.emptyList();
        }

        List<String> children = new ArrayList<>();

        for (String child : this.delegate.getDirectChildrenKeys(key))
        {
            if (this.allHandBones.contains(child))
            {
                children.add(child);
            }
        }

        return children;
    }

    @Override
    public String getParentGroupKey(String key)
    {
        if (this.roots.contains(key) || !this.allHandBones.contains(key) || this.delegate == null)
        {
            return null;
        }

        String parent = this.delegate.getParentGroupKey(key);

        return this.allHandBones.contains(parent) ? parent : null;
    }
}
