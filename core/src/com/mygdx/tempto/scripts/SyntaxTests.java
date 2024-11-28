package com.mygdx.tempto.scripts;

import com.badlogic.gdx.math.Vector2;
import com.mygdx.tempto.entity.pose.Pose;
import com.mygdx.tempto.entity.testpoint.GlideToPoint;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class SyntaxTests {

    public static final String HI = "hi";
    public static void main(String[] args) {

        // 11-28-2024: Testing how maps work
//        HashMap<String, Vector2> testMap = new HashMap<>(Map.ofEntries(
//                Map.entry("hi", new Vector2(0,0))
//        ));
//
//        Map<String, Vector2> justMap = Map.of(
//                "hi", new Vector2(1,23),
//                "hello", new Vector2(2, 3)
//        );
//
//        System.out.println(justMap.get("hi"));
//        System.out.println(justMap.get(HI));

        // 11-28-2024: Testing the instanceof keyword for null instances
        Pose nullPose = null;
        if (nullPose instanceof GlideToPoint) {
            System.out.println("GlideToPoint found!");
        } else {
            System.out.println("No");
        }


    }
}
