package com.mygdx.tempto.entity.pose;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;

import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;
import org.ejml.simple.SimpleMatrix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**An enum of file information, each corresponding to data about a pose. Includes methods to read and write to those files.*/
public enum PoseCatalog {

    PLAYER_STAND("player/stand.json", new String[]{"front_foot"}, new String[]{"hip"}),

    ;
    /**Constants for reading the file*/
    private static final String INPUT = "input", OUTPUT = "output";

    /**Base filepath where all poses are stored in general*/
    private static final String BASE_FILEPATH = "data/pose/";
    /**Where the data file for this pose is stored*/
    private String filepath;
    /**The names of input points; currently assumes each point is a {@link Vector2}, but once poses become more established this may change.*/
    private HashMap<String, Vector2[]> inputPoints;
    /**The names of each output point; currently assumes each point is a {@link Vector2}, but once poses become more established this may change.*/
    private HashMap<String, Vector2[]> outputPoints;

    PoseCatalog(String subPath, String[] inputIDs, String[] outputIDs) {
        this.filepath = BASE_FILEPATH + subPath;
        this.inputPoints = new HashMap<>();
        for (String inputID : inputIDs) inputPoints.put(inputID, new Vector2[]{});
        this.outputPoints = new HashMap<>();
        for (String outputID : outputIDs) outputPoints.put(outputID, new Vector2[]{});

        this.loadFileData();

    }

    /**Loads pose data from the corresponding file.
     * Assumes a JSON format:
     *
     * {
     *     "input":{
     *         "somePointID":[
     *              [x1,y1],
     *              [x2,y2],
     *              [x3,y3]
     *              ...
     *         ],
     *         "anotherPointID":[
     *              [x1,y1],
     *              [x2,y2],
     *              [x3,y3]
     *              ...
     *         ],
     *     },
     *     "output:{
     *
     *     }
     * }*/
    void loadFileData() {

        FileHandle poseFile = Gdx.files.local(this.filepath);
        if (!poseFile.exists()) return;

        JsonValue poseData = new JsonReader().parse(poseFile);
        JsonValue poseInput = poseData.getChild(INPUT);
        JsonValue poseOutput = poseData.getChild(OUTPUT);
        for (String inputID : this.inputPoints.keySet()) {
            this.inputPoints.put(inputID, jsonArrayToVec2Array(poseInput.get(inputID)));
        }
        for (String outputID : this.outputPoints.keySet()) {
            this.outputPoints.put(outputID, jsonArrayToVec2Array(poseOutput.get(outputID)));
        }
    }

    /**Converts a {@link JsonValue} 2d JSON array [[x1,y1], [x2,y2], etc] to a Java array {Vector2(x1,y1), Vector2(x2,y2), etc}*/
    private static Vector2[] jsonArrayToVec2Array(JsonValue jsonArray) {
        ArrayList<Vector2> vectors = new ArrayList<>();
        for (JsonValue vectorJSON : jsonArray) {
            float[] vals = vectorJSON.asFloatArray();
            vectors.add(new Vector2(vals[0], vals[1]));
        }
        Vector2[] arr = new Vector2[vectors.size()];
        return vectors.toArray(arr);
    }

    /**Writes this pose information back to a JSON file, formatted as given in {@link #loadFileData()}*/
    public void writeToFile() {
        JsonValue rootJson = new JsonValue(JsonValue.ValueType.object);

        JsonValue inputPoints = new JsonValue(JsonValue.ValueType.object);
        for (String inputID : this.inputPoints.keySet()) {
            Vector2[] points = this.inputPoints.get(inputID);
            JsonValue pointArray = Vec2ArrayToJsonArray(points);
            inputPoints.addChild(inputID, pointArray);
        }

        JsonValue outputPoints = new JsonValue(JsonValue.ValueType.array);
        for (String outputID : this.outputPoints.keySet()) {
            Vector2[] points = this.outputPoints.get(outputID);
            JsonValue pointArray = Vec2ArrayToJsonArray(points);
            outputPoints.addChild(outputID, pointArray);
        }

        rootJson.addChild("input", inputPoints);
        rootJson.addChild("output", outputPoints);

        FileHandle poseFile = Gdx.files.local(this.filepath);
        poseFile.writeString(rootJson.prettyPrint(JsonWriter.OutputType.json, 50), false);
    }

    private static JsonValue Vec2ArrayToJsonArray(Vector2[] vec2Array) {
        JsonValue pointArray = new JsonValue(JsonValue.ValueType.array);
        for (Vector2 point : vec2Array) {
            JsonValue vec = new JsonValue(JsonValue.ValueType.array);
            vec.addChild(new JsonValue(point.x));
            vec.addChild(new JsonValue(point.y));
            pointArray.addChild(vec);
        }

        return pointArray;
    }

    Pose generatePose(Map<String, Vector2> controlPoints) {

        return null;
    }

    public record LinearPoseData(HashMap<String, Vector2[]> inputSpace, HashMap<String, Vector2[]> outputSpace) {}
}
