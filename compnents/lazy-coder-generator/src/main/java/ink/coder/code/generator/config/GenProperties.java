package ink.coder.code.generator.config;

import lombok.Data;

/**
 * Java生成配置
 *
 * @author wangsilu
 */
@Data
public class GenProperties {

  /**
   * 关闭源文件输入流
   */
  private boolean closeSourceInputStream;

  /**
   * 目标包名
   */
  private String targetPackage;

  /**
   * 目标文件码制
   */
  private String targetEncode;


  /**
   * 模板格式
   */
  private TemplateType templateType;

  /**
   * 返回格式
   */
  private ResultType resultType;

  /**
   * 生成目标目录
   */
  private String targetPath;

  /**
   * 实现序列化
   */
  private boolean serialize;

  /**
   * 生成序列号版本号
   */
  private boolean serializeVersion;

  /**
   * 生成建表语句
   */
  private boolean genDDL;

  /**
   * 数据库类型
   */
  private DialectType dialectType;

  /**
   * 额外注解
   */
  private String extrAnnotation;

  /**
   * 自定义版本
   */
  private Long customSerializeVersion;

  /**
   * 根目录
   */
  private String basePath;

  /**
   * Excel文件类型
   */
  private String excelType;


}
