package Glaxium.POV.integration.access.bbs;

import org.joml.Vector3f;

public interface IGizmoDragFirstPerson
{
    void bbsPov$setRotationPivot(Vector3f pivot);

    Vector3f bbsPov$getRotationPivot();
}
