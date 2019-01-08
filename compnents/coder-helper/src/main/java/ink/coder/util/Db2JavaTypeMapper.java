package ink.coder.util;

/**
 * 数据库类型和java类型对照工具
 *
 * @author wangsilu
 */
public class Db2JavaTypeMapper {

  /**
   * 数据库类型转换为Java对照类型
   * @param dbType
   * @return
   */
  public static String getMatchType(String dbType) {
    if ("CHAR".equalsIgnoreCase(dbType) || "VARCHAR".equalsIgnoreCase(dbType)) {
      return "String";
    } else if ("int".equalsIgnoreCase(dbType) || "integer".equalsIgnoreCase(dbType)) {
      return "Integer";
    } else if ("long".equalsIgnoreCase(dbType) || "NUMERIC".equalsIgnoreCase(dbType)) {
      return "Long";
    }
    return dbType;
  }

}
