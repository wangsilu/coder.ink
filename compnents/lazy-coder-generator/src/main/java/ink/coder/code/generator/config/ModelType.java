package ink.coder.code.generator.config;

/**
 * @author wangsilu
 * <p>
 * 模型类型
 */

public enum ModelType {

  Entity("Entity", "实体");
  private final String key;
  private final String name;

  ModelType(String key, String name) {
    this.key = key;
    this.name = name;
  }

  public String getKey() {
    return key;
  }

  public String getName() {
    return name;
  }

  public static ModelType getFromKey(String key) {
    for (ModelType t : ModelType.values()) {
      if (t.getKey().equals(key)) {
        return t;
      }
    }
    return null;
  }
}
