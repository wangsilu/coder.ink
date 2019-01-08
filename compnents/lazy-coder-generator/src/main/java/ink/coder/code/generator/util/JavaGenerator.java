package ink.coder.code.generator.util;

import ink.coder.code.generator.config.DialectType;
import ink.coder.code.generator.config.GenProperties;
import ink.coder.code.generator.config.ResultType;
import ink.coder.code.generator.config.TemplateType;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * 数据字典生成Java类
 *
 * @author wangsilu
 */
public abstract class JavaGenerator {

  /**
   * 根据数据字典生成java实体类
   * <p>
   * 基于Astah生成的数据字典进行生成
   *
   * @param sourcePath 源文件路径
   * @param properties 生成配置 {@link GenProperties}
   * @return
   * @throws FileNotFoundException
   */
  public static String genJavaEntiesFromExcel(String sourcePath, GenProperties properties) throws IOException {
    return DictionaryToJava.genJavaEnties(sourcePath, properties);
  }


  /**
   * 根据数据字典生成java类
   * <p>
   * 基于Astah生成的数据字典进行生成
   *
   * @param inputStream 文件输入流
   * @param properties  生成配置 {@link GenProperties}
   * @return
   * @throws IOException
   */
  public static String genJavaEntiesFromExcelStream(InputStream inputStream, GenProperties properties) throws IOException {
    return DictionaryToJava.genJavaEnties(inputStream, properties);
  }

  public static void main(String[] args) throws IOException {
    String workfile = "C:\\Users\\wangsilu\\Desktop\\entityListModel.xls";
    String basePath = "C:\\Users\\wangsilu\\Desktop\\gen\\20190108\\";
    String zipTargetPath = "C:\\Users\\wangsilu\\Desktop\\gen\\";
    GenProperties properties = new GenProperties();

    properties.setBasePath(basePath);
    properties.setCloseSourceInputStream(true);
    properties.setDialectType(DialectType.MySQL);
    properties.setResultType(ResultType.ZIP);
    properties.setTargetPackage("com.invoo");
    properties.setSerialize(true);
    properties.setGenDDL(true);
    properties.setTargetEncode("UTF-8");
    properties.setSerializeVersion(true);
    properties.setTemplateType(TemplateType.LOMBOK);
    properties.setTargetPath(zipTargetPath);
    properties.setExcelType("xls");

    String gen = genJavaEntiesFromExcel(workfile,properties);

    System.out.println(gen);

  }

}
