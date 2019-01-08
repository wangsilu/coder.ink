package ink.coder.code.generator.model;

import lombok.Data;

/**
 * @author wangsilu
 */
@Data
public class ModelMetaAttribute {

  private String nameCn;
  private String name;

  private String dataType;
  private int length;
  private int precis;
  private boolean isPk;
  private boolean notNull;
  private boolean isFk;
  private String defaultValue;

}
