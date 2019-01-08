package ink.coder.code.generator.model;

import lombok.Data;

import java.util.List;

/**
 * @author wangsilu
 */
@Data
public class ModelMetaInfo {
  private String nameCn;
  private String className;
  private String packageName;
  private String directoryName;
  private String content;
  private List<ModelMetaAttribute> keys;
  private List<ModelMetaAttribute> attributes;
}
