package lanraragi.tag.entity;

import java.io.Serializable;

@lombok.Data
public class Tag implements Serializable {
    private TagData data;
    private String error;
    private String operation;
    private Integer success;
    private String type;
}

