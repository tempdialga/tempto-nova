package com.mygdx.tempto.entity.pose;

/**An interface for Entities or similar classes that can be used by {@link OldPose}.
 * Classes that implement this would have some kind of points that can be referenced by string tags*/
public interface Posable {
    float getAttribute(String attrName);
}
