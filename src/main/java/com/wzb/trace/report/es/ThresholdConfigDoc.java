package com.wzb.trace.report.es;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.util.List;

@Data
public class ThresholdConfigDoc implements Serializable {

    @Id
    private String id;

    @Field(type = FieldType.Keyword, name = "project")
    private String project;

    @Field(type = FieldType.Integer, name = "threshold")
    private Integer threshold;

    @Field(type = FieldType.Object, name = "path-thresholds")
    private List<PathThreshold> pathThresholds;

    @Data
    public static class PathThreshold implements Serializable {

        @Field(type = FieldType.Keyword, name = "path")
        private String path;

        @Field(type = FieldType.Integer, name = "path-threshold")
        private Integer pathThreshold;
    }

}
