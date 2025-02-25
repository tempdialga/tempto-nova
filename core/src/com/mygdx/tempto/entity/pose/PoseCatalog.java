package com.mygdx.tempto.entity.pose;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;

import org.ejml.simple.SimpleMatrix;

import java.util.Arrays;
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
    /**The original input and output IDs, preserved for order*/
    private String[] inputIDs, outputIDs;
    /**The input points and output points data of the pose*/
    private LinearPoseData inputOutputData;
    /**The matrix representing the input space, where each column vector comprises the different input vectors concatenated together*/
    private SimpleMatrix inputSpace = new SimpleMatrix(1,1);
    /**The matrix representing the output space, where each column vector comprises the different output vectors concatenated together*/
    private SimpleMatrix outputSpace = new SimpleMatrix(1,1);
    /**The number of cases required to fully describe this pose, equivalent to n+1, where n is the number of independent variables in the input. For a pose with 1 vector2 input (2 variables, x and y), 3 cases are required, for a ose with 2 vector2s, 5 cases, etc.*/
    private int numCases;


    PoseCatalog(String subPath, String[] inputIDs, String[] outputIDs) {
        this.filepath = BASE_FILEPATH + subPath;
        HashMap<String, Vector2[]> inputPoints = new HashMap<>();
        for (String inputID : inputIDs) inputPoints.put(inputID, new Vector2[]{});
        HashMap<String, Vector2[]> outputPoints = new HashMap<>();
        for (String outputID : outputIDs) outputPoints.put(outputID, new Vector2[]{});
        this.inputIDs = inputIDs;
        this.outputIDs = outputIDs;
        this.inputOutputData = new LinearPoseData(inputPoints, outputPoints);
        this.numCases = inputIDs.length*2 + 1;//TODO: This would need to change if more than Vector2 cases are considered

        this.loadFileData();
        this.generateSpaceMatricesFromLinearPoseData();
        this.updateLinearPoseDataFromSpaceMatrices();
        System.out.println(this.inputSpace.toString());
        System.out.println(this.outputSpace.toString());
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

    Pose generatePose(Map<String, Vector2> controlPoints) {

        return null;
    }

    /**Converts input and output vectors into the matrices representing the input and output spaces. TODO: refactor to be a little simpler to understand, likely by just generating the matrices here and filling them item by item*/
    public void generateSpaceMatricesFromLinearPoseData() {
//        SimpleMatrix[] inputColumns = new SimpleMatrix[this.numCases];
//        SimpleMatrix[] outputColumns = new SimpleMatrix[this.numCases];
//
//        for (int case_idx = 0; case_idx < this.numCases; case_idx++) {
//            Vector2[] caseInputVectors = new Vector2[this.inputIDs.length];
//            Vector2[] caseOutputVectors = new Vector2[this.outputIDs.length];
//            for (int input_idx = 0; input_idx < this.inputIDs.length; input_idx++) {
//                String inputID = this.inputIDs[input_idx];
//                Vector2[] inputPointCases = this.inputOutputData.inputSpace.get(inputID);
//
////                if (inputPointCases.length < this.numCases) throw new IllegalArgumentException("Not enough cases specified for input point '"+inputID+"' of pose '"+this.name()+"' (Required: "+this.numCases+", Provided: "+inputPointCases.length);
//                if (inputPointCases.length < this.numCases) {
//                    caseInputVectors[input_idx] = new Vector2();
//                    continue;
//                }
//                caseInputVectors[input_idx] = inputPointCases[case_idx];
//            }
//            for (int output_idx = 0; output_idx < this.outputIDs.length; output_idx++) {
//                String outputID = this.outputIDs[output_idx];
//                Vector2[] outputPointCases = this.inputOutputData.outputSpace.get(outputID);
//
////                if (outputPointCases.length < this.numCases) throw new IllegalArgumentException("Not enough cases specified for output point '"+outputID+"' of pose '"+this.name()+"' (Required: "+this.numCases+", Provided: "+outputPointCases.length);
//                if (outputPointCases.length < this.numCases) {
//                    caseOutputVectors[output_idx] = new Vector2();
//                    continue;
//                }
//                caseOutputVectors[output_idx] = outputPointCases[case_idx];
//            }
//
//            inputColumns[case_idx] = MiscFunctions.concatVector2sColumn(caseInputVectors, true);
//            outputColumns[case_idx] = MiscFunctions.concatVector2sColumn(caseOutputVectors, false);
//        }
//
//        this.inputSpace = this.inputSpace.concatColumns(inputColumns);
//        this.outputSpace = this.outputSpace.concatColumns(outputColumns);

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


}
