package com.mygdx.tempto.entity.pose;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import com.mygdx.tempto.entity.player.Player;
import com.mygdx.tempto.util.MiscFunctions;

import org.ejml.simple.SimpleMatrix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.Line;

/**An enum of file information, each corresponding to data about a pose. Includes methods to read and write to those files. Input points and output points must not share IDs.*/
public enum PoseCatalog {

    PLAYER_STAND("player/stand.json", new String[]{"front_foot"}, new String[]{"hip"}),
    PLAYER_STAND2("player/stand2.json", new String[]{"front_foot"}, new String[]{"hip"}),

    PLAYER_WALK1("player/walk1.json", new String[]{"moving_foot"}/*Relative to planted foot*/, new String[]{"hip", "chest", "mf_hand", "pf_hand"}),

    ;
    /**Constants for reading the file*/
    private static final String INPUT = "input", OUTPUT = "output";

    /**Base filepath where all poses are stored in general*/
    private static final String BASE_FILEPATH = "data/pose/";
    /**Where the data file for this pose is stored*/
    private String filepath;
    /**The original input and output IDs, preserved for order. Inputs and outputs should not overlap, so that an id can be used without specifying input or output.*/
    public String[] inputIDs, outputIDs;
    /**The input points and output points data of the pose*/
    public LinearPoseData inputOutputData;
    /**The matrix representing the input space, where each column vector comprises the different input vectors concatenated together*/
    private SimpleMatrix inputSpace = new SimpleMatrix(1,1);
    /**The matrix representing the output space, where each column vector comprises the different output vectors concatenated together*/
    private SimpleMatrix outputSpace = new SimpleMatrix(1,1);
    /**The number of cases required to fully describe this pose, equivalent to n+1, where n is the number of independent variables in the input. For a pose with 1 vector2 input (2 variables, x and y), 3 cases are required, for a ose with 2 vector2s, 5 cases, etc.*/
    private int numCases;
    /**A/The {@link Posable} this pose is associated with, if any. E.g. a secondary instance of {@link Player} the editor created to edit player poses. Used in the editor to do things like place constraints or generate default points*/
    private Posable targetPosable;
    /**Any constraints on the points (purely used for editing), e.g. that when editing an example case, you can't drag the foot farther than leg_length from the hip*/
    private ArrayList<PoseConstraint> constraints;
    PoseCatalog(String subPath, String[] inputIDs, String[] outputIDs, Posable target) {
        this.filepath = BASE_FILEPATH + subPath;
        HashMap<String, Vector2[]> inputPoints = new HashMap<>();
        for (String inputID : inputIDs) inputPoints.put(inputID, new Vector2[]{});
        HashMap<String, Vector2[]> outputPoints = new HashMap<>();
        for (String outputID : outputIDs) outputPoints.put(outputID, new Vector2[]{});
        this.inputIDs = inputIDs;
        this.outputIDs = outputIDs;
        this.inputOutputData = new LinearPoseData(inputPoints, outputPoints);
        this.numCases = inputIDs.length*2 + 1;//TODO: This would need to change if more than Vector2 cases are considered
        this.targetPosable = target;

        this.loadFileData();
        this.generateSpaceMatricesFromLinearPoseData();
        this.updateLinearPoseDataFromSpaceMatrices();
        System.out.println(this.inputSpace.toString());
        System.out.println(this.outputSpace.toString());


    }



    PoseCatalog(String subPath, String[] inputIDs, String[] outputIDs) {
        this(subPath, inputIDs, outputIDs, null);
    }

    /**Returns the input or output point Vector2 corresponding to the given ID in the given case. If no points with this id are found, returns null.*/
    public Vector2 getPoint(String id, int caseIdx) {
        LinearPoseData data = this.inputOutputData;
        if (data.inputSpace.containsKey(id)) {
            return data.inputSpace.get(id)[caseIdx];
        } else if (data.outputSpace.containsKey(id)) {
            return data.outputSpace.get(id)[caseIdx];
        }
        return null;
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
    public void loadFileData() {

        FileHandle poseFile = Gdx.files.local(this.filepath);
        if (!poseFile.exists()) return;

        this.inputOutputData = LinearPoseData.JSON.fromJson(LinearPoseData.class, poseFile);
    }

    /**Writes this pose information back to a JSON file, formatted as given in {@link #loadFileData()}*/
    public void writeToFile() {
        FileHandle poseFile = Gdx.files.local(this.filepath);

        System.out.println(Arrays.toString(this.inputOutputData.inputSpace.get(this.inputIDs[0])));
        poseFile.writeString(LinearPoseData.JSON.prettyPrint(this.inputOutputData), false);
    }


    /**Converts input and output vectors into the matrices representing the input and output spaces. TODO: refactor to be a little simpler to understand, likely by just generating the matrices here and filling them item by item*/
    public void generateSpaceMatricesFromLinearPoseData() {
        int numInputRows = this.inputIDs.length*2+1;
        int numOutputRows = this.outputIDs.length*2;
        int numCols = this.numCases;

        this.inputSpace = new SimpleMatrix(numInputRows, numCols);
        this.outputSpace = new SimpleMatrix(numOutputRows, numCols);
        for (int col = 0; col < this.numCases; col++) {
            //Input Space
            for (int i = 0; i < this.inputIDs.length; i++) {
                Vector2[] inputVectorCases = this.inputOutputData.inputSpace.get(this.inputIDs[i]);
                Vector2 inputVector = new Vector2();
                if (!(inputVectorCases.length < this.numCases)) {
                    inputVector.set(inputVectorCases[col]);
                }

                int xrow = i*2;
                int yrow = i*2+1;
                this.inputSpace.set(xrow, col, inputVector.x);
                this.inputSpace.set(yrow, col, inputVector.y);
            }
            this.inputSpace.set(numInputRows-1, col, 1);

            //Output Space
            for (int i = 0; i < this.outputIDs.length; i++) {
                Vector2[] outputVectorCases = this.inputOutputData.outputSpace.get(this.outputIDs[i]);
                Vector2 outputVector = new Vector2();
                if (!(outputVectorCases.length < this.numCases)) {
                    outputVector.set(outputVectorCases[col]);
                }

                int xrow = i*2;
                int yrow = i*2+1;
                this.outputSpace.set(xrow, col, outputVector.x);
                this.outputSpace.set(yrow, col, outputVector.y);
            }
        }
    }

    /**Inverse of {@link #generateSpaceMatricesFromLinearPoseData()}; writes space matrices' data back to {@link #inputOutputData}*/
    public void updateLinearPoseDataFromSpaceMatrices() {
        //Input Space
        for (int i = 0; i < this.inputIDs.length; i++) {
            String vectorID = this.inputIDs[i];
            Vector2[] vectorCases = this.inputOutputData.inputSpace.get(vectorID);
            if (vectorCases.length < this.numCases) {
                Vector2[] newCases = new Vector2[this.numCases];
                System.arraycopy(vectorCases, 0, newCases, 0, vectorCases.length);
                for (int new_idx = vectorCases.length; new_idx < this.numCases; new_idx++) {
                    newCases[new_idx] = new Vector2(01,01);
                }
                vectorCases = newCases;
                this.inputOutputData.inputSpace.put(vectorID, newCases);
                System.out.println(Arrays.toString(newCases));
            }

            int xrow = i*2;
            int yrow = i*2+1;


            for (int col = 0; col < this.numCases; col++) {
                vectorCases[col].set(
                        (float) this.inputSpace.get(xrow, col),
                        (float) this.inputSpace.get(yrow, col)
                );
            }
        }

        //Output Space
        for (int i = 0; i < this.outputIDs.length; i++) {
            String vectorID = this.outputIDs[i];
            Vector2[] vectorCases = this.inputOutputData.outputSpace.get(vectorID);
            if (vectorCases.length < this.numCases) {
                Vector2[] newCases = new Vector2[this.numCases];
                System.arraycopy(vectorCases, 0, newCases, 0, vectorCases.length);
                for (int new_idx = vectorCases.length; new_idx < this.numCases; new_idx++) {
                    newCases[new_idx] = new Vector2(10,10);
                }
                vectorCases = newCases;
                this.inputOutputData.outputSpace.put(vectorID, newCases);
            }



            int xrow = i*2;
            int yrow = i*2+1;
            for (int col = 0; col < this.numCases; col++) {

                vectorCases[col].set(
                        (float) this.outputSpace.get(xrow, col),
                        (float) this.outputSpace.get(yrow, col)
                );
            }
        }
    }

    public Pose getPoseForInput(Vector2... inputs) {
        SimpleMatrix input = MiscFunctions.concatVector2sColumn(inputs, true);
        SimpleMatrix caseMixVector = this.inputSpace.solve(input);
        SimpleMatrix correspondingOutput = this.outputSpace.mult(caseMixVector);

        HashMap<String, Vector2> newPosePoints = new HashMap<>();
        for (int i = 0; i < inputs.length; i++) {
            newPosePoints.put(this.inputIDs[i], inputs[i]);
        }
        for (int i = 0; i < this.outputIDs.length; i++) {
            newPosePoints.put(this.outputIDs[i], new Vector2((float) correspondingOutput.get(i*2, 0), (float) correspondingOutput.get(i*2+1, 0)));
        }

        return new Pose(newPosePoints);
    }

    public Vector2 getPointForInput(String toGet, Vector2... inputs) {
        int idx = this.getOutputIndex(toGet);
        System.out.println("Idx found: "+idx);
        if (idx == -1) throw new IllegalArgumentException("Output point '"+toGet+"' not found. Available output points: "+ Arrays.toString(this.outputIDs));

        SimpleMatrix input = MiscFunctions.concatVector2sColumn(inputs, true);
//        System.out.println("Input space: "+this.inputSpace);
//        System.out.println("Input: "+input);
//        System.out.println("Inverted input: "+this.inputSpace.invert());
        SimpleMatrix caseMixVector = this.inputSpace.solve(input);
//        System.out.println("Case mix: "+caseMixVector);
//        System.out.println("Output space: "+this.outputSpace);
        SimpleMatrix correspondingOutput = this.outputSpace.mult(caseMixVector);
        System.out.println("Case mix output: "+correspondingOutput);
        Vector2 outputPoint = new Vector2((float) correspondingOutput.get(idx*2, 0), (float) correspondingOutput.get(idx*2+1, 0));
        System.out.println("Output for "+toGet+": "+outputPoint);
        return outputPoint;
    }

    /**Returns the index of the given output by name, or -1 if one cannot be found.*/
    public int getOutputIndex(String output) {
        for (int i = 0; i < this.outputIDs.length; i++) {
            if (output.equals(this.outputIDs[i])) {
                return i;
            }
        }
        return -1;
    }

    public int getNumCases() {
        return numCases;
    }

    public ArrayList<PoseConstraint> getConstraints() {
        return constraints;
    }

    public static class LinearPoseData {
        /**The {@link Json} instance used to read and write json data to serialize each pose's {@link LinearPoseData} instance*/
        private static final Json JSON = new Json();
        static {
            JSON.setOutputType(JsonWriter.OutputType.json);
//        JSON.setSerializer(LinearPoseData.class, new Json.Serializer<LinearPoseData>() {
//
//            @Override
//            public void write(Json json, LinearPoseData object, Class knownType) {
//
//            }
//
//            @Override
//            public LinearPoseData read(Json json, JsonValue jsonData, Class type) {
//                return null;
//            }
//        });
            JSON.setSerializer(Vector2.class, new Json.Serializer<Vector2>() {
                @Override
                public void write(Json json, Vector2 object, Class knownType) {
                    json.writeObjectStart();
                    json.writeValue("x", object.x);
                    json.writeValue("y", object.y);
                    json.writeObjectEnd();
                }

                @Override
                public Vector2 read(Json json, JsonValue jsonData, Class type) {
                    return new Vector2(
                            jsonData.getFloat("x"),
                            jsonData.getFloat("y")
                    );
                }
            });
        }


        public HashMap<String, Vector2[]> inputSpace;
        public HashMap<String, Vector2[]> outputSpace;
        public LinearPoseData() {
        }

        public LinearPoseData(HashMap<String, Vector2[]> inputSpace, HashMap<String, Vector2[]> outputSpace) {
            this.inputSpace = inputSpace;
            this.outputSpace = outputSpace;
        }

    }



    public class PoseConstraint {

    }
}
