package com.github.drsmugleaf;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.BooleanIndexing;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.indexing.conditions.Conditions;

/**
 * Created by DrSmugleaf on 08/03/2020
 */
public class LastStepPreProcessor implements DataSetPreProcessor {

    public LastStepPreProcessor() {}

    public void preProcess(DataSet toPreProcess) {
        INDArray labels = toPreProcess.getLabels();
        INDArray mask = toPreProcess.getLabelsMaskArray();
        INDArray labels2d = pullLastTimeSteps(labels, mask);

        toPreProcess.setLabels(labels2d);
        toPreProcess.setLabelsMaskArray(null);
    }

    public INDArray pullLastTimeSteps(INDArray from, INDArray mask) {
        if (mask == null) {
            long point = from.size(2) - 1;
            return from.get(NDArrayIndex.all(), NDArrayIndex.all(), NDArrayIndex.point(point));
        } else {
            long[] shape = new long[2];
            shape[0] = from.size(0);
            shape[1] = from.size(1);

            INDArray lastTimeSteps = Nd4j.create(shape);
            INDArray lastStepArray = BooleanIndexing.lastIndex(mask, Conditions.epsNotEquals(0.0), 1);
            int[] forwardPassTimeSteps = lastStepArray.data().asInt();

            for (int i = 0; i < forwardPassTimeSteps.length; i++) {
                lastTimeSteps.putRow(i, from.get(NDArrayIndex.point(i), NDArrayIndex.all(), NDArrayIndex.point(forwardPassTimeSteps[i])));
            }

            return lastTimeSteps;
        }
    }

}
