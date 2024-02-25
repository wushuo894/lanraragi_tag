package lanraragi.tag.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public class Info implements Serializable {
    private String arcid;
    private String extension;
    private String isnew;
    private Integer pagecount;
    private Integer progress;
    private String tags;
    private String title;
}
