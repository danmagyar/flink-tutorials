package com.cloudera.streaming.examples.flink;

import com.cloudera.streaming.examples.flink.types.HeapAlert;
import com.cloudera.streaming.examples.flink.types.HeapMetrics;
import org.apache.commons.compress.utils.Sets;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class HeapMonitorPipelineTest {

    static Set<HeapAlert> testOutput = new HashSet<>();

    @Test
    public void testPipeline() throws Exception {

        final String alertMask = "42";

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        HeapMetrics alert1 = testStats(0.42);
        HeapMetrics regular1 = testStats(0.452);
        HeapMetrics regular2 = testStats(0.245);
        HeapMetrics alert2 = testStats(0.9423);

        DataStreamSource<HeapMetrics> testInput = env.fromElements(alert1, alert2, regular1, regular2);
        HeapMonitorPipeline.computeHeapAlerts(testInput, ParameterTool.fromArgs(new String[]{"--alertMask", alertMask}))
                .addSink(new SinkFunction<HeapAlert>() {
                    @Override
                    public void invoke(HeapAlert value) {
                        testOutput.add(value);
                    }
                })
                .setParallelism(1);

        env.execute();

        assertEquals(Sets.newHashSet(HeapAlert.maskRatioMatch(alertMask, alert1),
                HeapAlert.maskRatioMatch(alertMask, alert2)), testOutput);
    }

    private HeapMetrics testStats(double ratio) {
        return new HeapMetrics(HeapMetrics.OLD_GEN, 0, 0, ratio, 0, "testhost");
    }
}