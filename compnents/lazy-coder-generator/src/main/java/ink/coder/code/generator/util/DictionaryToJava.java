package ink.coder.code.generator.util;

import com.google.common.base.Preconditions;
import ink.coder.code.generator.config.GenProperties;
import ink.coder.code.generator.config.ModelType;
import ink.coder.code.generator.config.TemplateType;
import ink.coder.code.generator.model.ClassFileInfo;
import ink.coder.code.generator.model.ModelMetaAttribute;
import ink.coder.code.generator.model.ModelMetaInfo;
import ink.coder.util.Db2JavaTypeMapper;
import ink.coder.util.StringUtil;
import ink.coder.util.ZipUtil;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 数据字典生成Java类
 *
 * @author wangsilu
 */
public class DictionaryToJava {

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
  static String genJavaEnties(String sourcePath, GenProperties properties) throws IOException {
    Preconditions.checkNotNull(sourcePath, "源文件参数错误");

    properties.setCloseSourceInputStream(true);

    InputStream inputStream = new FileInputStream(new File(sourcePath));
    return genJavaEnties(inputStream, properties);
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
  static String genJavaEnties(InputStream inputStream, GenProperties properties) throws IOException {
    Workbook workbook = null;
    try {
      if (properties.getExcelType().equalsIgnoreCase("xls")) {
        workbook = new HSSFWorkbook(inputStream);
      } else {
        workbook = new XSSFWorkbook(inputStream);
      }
      Iterator<Sheet> sheetIterator = workbook.sheetIterator();

      List<ModelMetaInfo> models = new ArrayList<ModelMetaInfo>();

      while (sheetIterator.hasNext()) {
        Sheet sheet = sheetIterator.next();

        String type = sheet.getRow(0).getCell(0).getStringCellValue();
        if (ModelType.Entity.getKey().equalsIgnoreCase(type)) {
          ModelMetaInfo metaInfo = pareseToModelMetaInfo(sheet);
          if (metaInfo != null) {
            models.add(metaInfo);
          }
        }
      }

      return genFromTemplate(models, properties);

    } finally {
      if (workbook != null) {
        workbook.close();
      }
      try {
        if (properties.isCloseSourceInputStream()) {
          inputStream.close();
        }
      } catch (IOException e) {
        throw e;
      }
    }
  }

  private static String genFromTemplate(List<ModelMetaInfo> models, GenProperties config) throws IOException {

    List<ClassFileInfo> modelList = new ArrayList<ClassFileInfo>();

    List<String> ddls = new ArrayList<String>();

    for (ModelMetaInfo model : models) {
      // 模型层
      if(model.getKeys().size()>1){
        modelList.add(genModelKeyClassDef(model, config));
      }
      modelList.add(genModelClassDef(model, config));
      // 服务层
      modelList.add(genServiceClassDef(model, config));
      modelList.add(genServiceImplClassDef(model, config));
      // DAO层
      modelList.add(genDAOClassDef(model, config));
      // Mapper对应XML配置文件
      modelList.add(genDAOXmlDef(model, config));


      if (config.isGenDDL()) {
        String ddl = Model2DDLHelper.model2DDL(model, config.getDialectType());
        if (!ddls.contains(ddl)) {
          ddls.add(Model2DDLHelper.model2DDL(model, config.getDialectType()));
        }
      }
    }

    switch (config.getResultType()) {
      case STRING:
        List<String> classes = new ArrayList<String>();
        for (ClassFileInfo classFileInfo : modelList) {
          classes.add(classFileInfo.getTextInfo());
        }
        return StringUtil.concatString(classes) + StringUtil.concatString(ddls);
      case FILE:
        return saveClassToDisk(modelList, ddls, config, false);
      case ZIP:
        return saveClassToDisk(modelList, ddls, config, true);
      default:
        return saveClassToDisk(modelList, ddls, config, true);
    }
  }


  private static String saveClassToDisk(List<ClassFileInfo> classes, List<String> ddls, GenProperties config, boolean zip) throws IOException {
    String basePath = (config.getBasePath() == null ? "." : config.getBasePath()) + File.separator;
    String packDirectory = config.getTargetPackage().replace(".", File.separator);

    File targetDir = new File(basePath + packDirectory);
    targetDir.mkdirs();

    String workPath = targetDir.getAbsolutePath() + File.separator;
    for (ClassFileInfo entry : classes) {

      /**
       * 生文件
       */
      File classFile = new File(basePath + entry.getPath());
      if (!classFile.getParentFile().exists()) {
        classFile.getParentFile().mkdirs();
      }
      FileOutputStream fos = new FileOutputStream(classFile);

      fos.write(entry.getTextInfo().getBytes(config.getTargetEncode() == null ? "UTF-8" : config.getTargetEncode()));
      fos.close();
    }

    File ddlFile = new File(basePath + "ddl.sql");
    FileOutputStream fos = new FileOutputStream(ddlFile);
    for (String ddl : ddls) {
      fos.write(ddl.getBytes(config.getTargetEncode() == null ? "UTF-8" : config.getTargetEncode()));
    }
    fos.close();
    if (zip) {
      ZipUtil.makeZip(basePath, config.getTargetPath() + File.separator + "version.zip");
      return basePath + File.separator + "version.zip";
    } else {
      return workPath;
    }
  }

  /**
   * 生成模型层定义（实体类）
   *
   * @param model
   * @param config
   * @return
   */
  private static ClassFileInfo genModelClassDef(ModelMetaInfo model, GenProperties config) {
    StringBuilder classDef = new StringBuilder();
    classDef.append("/**\n");
    classDef.append("* Powered By [lazy-coder-generator] \n");
    classDef.append("* Web Site: https://www.coder.ink\n");
    classDef.append("*  Since 2018\n");
    classDef.append("*/\n\n");

    if (config.getTargetPackage() != null) {
      classDef.append("package ").append(config.getTargetPackage()).append(".model;").append("\n\n");
    }

    classDef.append("/**\n");
    classDef.append("* ").append(model.getNameCn()).append("\n");
    classDef.append("*/\n\n");

    if (TemplateType.LOMBOK.equals(config.getTemplateType())) {
      classDef.append("@lombok.Data\n");
    } else if (TemplateType.JPA.equals(config.getTemplateType())) {
      classDef.append("@Entity\n");
      classDef.append("@Table(name=\"").append(model.getClassName()).append("\")\n");
    }

    boolean multiKey =  model.getKeys().size()>1;
    if(multiKey) {
      classDef.append("public class ").append(genClassName(model.getClassName())).append(" extends ").append(genClassName(model.getClassName())+"Key").append(" {\n");
    }else{
      classDef.append("public class ").append(genClassName(model.getClassName())).append(config.isSerialize() ? " implements java.io.Serializable{\n" : "{\n");
    }

    if (config.isSerializeVersion()) {
      if (config.getCustomSerializeVersion() != null) {
        classDef.append("\n\tprivate static final long serialVersionUID = ").append(config.getCustomSerializeVersion().longValue()).append("L;\n");
      } else {
        classDef.append("\n\tprivate static final long serialVersionUID = 1L;\n");
      }
    }

    for (ModelMetaAttribute attr : model.getAttributes()) {
      if(multiKey && attr.isPk()){
        continue;
      }
      classDef.append("\t/**\n");
      classDef.append("\t* ").append(attr.getNameCn()).append("\n");
      classDef.append("\t*/\n");
      classDef.append("\tprivate ").append(Db2JavaTypeMapper.getMatchType(attr.getDataType())).append(" ").append(StringUtil.formatCamel(attr.getName(), false)).append(";\n\n");
    }

    classDef.append("}\n");

    ClassFileInfo classFileInfo = new ClassFileInfo();
    classFileInfo.setTextInfo(classDef.toString());
    classFileInfo.setClassName(genClassName(model.getClassName()));

    String path = config.getTargetPackage().replace(".", File.separator) + File.separator + "model" + File.separator + classFileInfo.getClassName() + ".java";
    classFileInfo.setPath(path);

    return classFileInfo;
  }

  /**
   * 生成模型层定义（实体类）
   *
   * @param model
   * @param config
   * @return
   */
  private static ClassFileInfo genModelKeyClassDef(ModelMetaInfo model, GenProperties config) {
    StringBuilder classDef = new StringBuilder();
    classDef.append("/**\n");
    classDef.append("* Powered By [lazy-coder-generator] \n");
    classDef.append("* Web Site: https://www.coder.ink\n");
    classDef.append("*  Since 2018\n");
    classDef.append("*/\n\n");

    if (config.getTargetPackage() != null) {
      classDef.append("package ").append(config.getTargetPackage()).append(".model;").append("\n\n");
    }

    classDef.append("/**\n");
    classDef.append("* ").append(model.getNameCn()).append(" Key\n");
    classDef.append("*/\n\n");

    if (TemplateType.LOMBOK.equals(config.getTemplateType())) {
      classDef.append("@lombok.Data\n");
    } else if (TemplateType.JPA.equals(config.getTemplateType())) {
      classDef.append("@Entity\n");
      classDef.append("@Table(name=\"").append(model.getClassName()).append("\")\n");
    }

    classDef.append("public class ").append(genClassName(model.getClassName())).append(config.isSerialize() ? " implements java.io.Serializable{\n" : "{\n");

    if (config.isSerializeVersion()) {
      if (config.getCustomSerializeVersion() != null) {
        classDef.append("\n\tprivate static final long serialVersionUID = ").append(config.getCustomSerializeVersion().longValue()).append("L;\n");
      } else {
        classDef.append("\n\tprivate static final long serialVersionUID = 1L;\n");
      }
    }

    for (ModelMetaAttribute attr : model.getKeys()) {
      classDef.append("\t/**\n");
      classDef.append("\t* ").append(attr.getNameCn()).append("\n");
      classDef.append("\t*/\n");
      classDef.append("\tprivate ").append(Db2JavaTypeMapper.getMatchType(attr.getDataType())).append(" ").append(StringUtil.formatCamel(attr.getName(), false)).append(";\n\n");
    }

    classDef.append("}\n");

    ClassFileInfo classFileInfo = new ClassFileInfo();
    classFileInfo.setTextInfo(classDef.toString());
    classFileInfo.setClassName(genClassName(model.getClassName()));

    String path = config.getTargetPackage().replace(".", File.separator) + File.separator + "model" + File.separator + classFileInfo.getClassName() + "Key.java";
    classFileInfo.setPath(path);

    return classFileInfo;
  }

  /**
   * 生成服务层定义
   *
   * @param model
   * @param config
   * @return
   */
  private static ClassFileInfo genServiceClassDef(ModelMetaInfo model, GenProperties config) {
    StringBuilder classDef = new StringBuilder();
    classDef.append("/**\n");
    classDef.append("* Powered By [lazy-coder-generator] \n");
    classDef.append("* Web Site: https://www.coder.ink\n");
    classDef.append("*  Since 2018\n");
    classDef.append("*/\n\n");

    if (config.getTargetPackage() != null) {
      classDef.append("package ").append(config.getTargetPackage()).append(".service;").append("\n\n");
    }

    classDef.append("/**\n");
    classDef.append("* ").append(model.getNameCn()).append("服务接口定义\n");
    classDef.append("*/\n\n");

    String className = genClassName(model.getClassName());

    classDef.append("public interface ").append(genClassName(model.getClassName())).append("Service {");

    classDef.append("\n\n\t/**");
    classDef.append("\n\t* ").append(model.getNameCn()).append("新增接口（字段选择性插入，空属性不处理）");
    classDef.append("\n\t*/");
    classDef.append("\n\tpublic absract int insert(").append(className).append(" record);");

    classDef.append("\n\n\t/**");
    classDef.append("\n\t* ").append(model.getNameCn()).append("更新接口（字段选择性更新，空属性不处理）");
    classDef.append("\n\t*/");
    classDef.append("\n\tpublic int updateByPrimaryKey(").append(className).append(" record);");

    String keyClass = "";
    String keyName = "key";
    if (model.getKeys() != null && model.getKeys().size() > 0) {
      if (model.getKeys().size() == 1) {
        keyClass = Db2JavaTypeMapper.getMatchType(model.getKeys().get(0).getDataType());
        keyName = genClassAttrName(model.getKeys().get(0).getName());
      } else {
        keyClass = config.getTargetPackage() + ".model." + className + "Key";
      }

      classDef.append("\n\n\t/**");
      classDef.append("\n\t* ").append(model.getNameCn()).append("删除接口（根据主键删除）");
      classDef.append("\n\t*/");
      classDef.append("\n\tpublic int deleteByPrimaryKey(").append(keyClass).append(" ").append(keyName).append(");");

      classDef.append("\n\n\t/**");
      classDef.append("\n\t* ").append(model.getNameCn()).append("查询接口（根据主键查询）");
      classDef.append("\n\t*/");
      classDef.append("\n\tpublic ").append(className).append(" selectByPrimaryKey(").append(keyClass).append(" ").append(keyName).append(");");
    }

    classDef.append("\n\n}\n");

    ClassFileInfo classFileInfo = new ClassFileInfo();
    classFileInfo.setTextInfo(classDef.toString());
    classFileInfo.setClassName(className + "Service");

    String path = config.getTargetPackage().replace(".", File.separator) + File.separator + "service" + File.separator + className + "Service.java";
    classFileInfo.setPath(path);

    return classFileInfo;
  }

  /**
   * 生成服务层定义
   *
   * @param model
   * @param config
   * @return
   */
  private static ClassFileInfo genServiceImplClassDef(ModelMetaInfo model, GenProperties config) {
    StringBuilder classDef = new StringBuilder();
    classDef.append("/**\n");
    classDef.append("* Powered By [lazy-coder-generator] \n");
    classDef.append("* Web Site: https://www.coder.ink\n");
    classDef.append("*  Since 2018\n");
    classDef.append("*/\n\n");

    String className = genClassName(model.getClassName());

    if (config.getTargetPackage() != null) {
      classDef.append("package ").append(config.getTargetPackage()).append(".service.impl;").append("\n\n");
    }

    classDef.append("/**\n");
    classDef.append("* ").append(model.getNameCn()).append("服务接口\n");
    classDef.append("*/");

    classDef.append("\n\nimport ").append(config.getTargetPackage()).append(".model.").append(className).append(";");
    classDef.append("\n\n@org.springframework.stereotype.Service");
    classDef.append("\n@lombok.extern.slf4j.Slf4j");
    classDef.append("\npublic class ").append(className).append("ServiceImpl implements ").append(className).append("{\n");

    classDef.append("\n\t@Autowired");
    classDef.append("\n\tprivate ").append(className).append("Mapper ").append(StringUtil.formatCamel(model.getClassName(), false)).append("Mapper");

    classDef.append("\n\n\t@Override");
    classDef.append("\n\tpublic int insert(").append(className).append(" record){");
    classDef.append("\n\t\treturn ").append(StringUtil.formatCamel(model.getClassName(), false)).append("Mapper.insert(record);");
    classDef.append("\n\t}");

    classDef.append("\n\n\t@Override");
    classDef.append("\n\tpublic int updateByPrimaryKey(").append(className).append(" record){");
    classDef.append("\n\t\treturn ").append(StringUtil.formatCamel(model.getClassName(), false)).append("Mapper.updateByPrimaryKey(record);");
    classDef.append("\n\t}");

    String keyClass = "";
    String keyName = "key";
    if (model.getKeys() != null && model.getKeys().size() > 0) {
      if (model.getKeys().size() == 1) {
        keyClass = Db2JavaTypeMapper.getMatchType(model.getKeys().get(0).getDataType());
        keyName = genClassAttrName(model.getKeys().get(0).getName());
      } else {
        keyClass = config.getTargetPackage() + ".model." + className + "Key";
      }
      classDef.append("\n\n\t@Override");
      classDef.append("\n\tpublic int deleteByPrimaryKey(").append(keyClass).append(" ").append(keyName).append("){");
      classDef.append("\n\t\treturn ").append(StringUtil.formatCamel(model.getClassName(), false)).append("Mapper.deleteByPrimaryKey(").append(keyName).append(");");
      classDef.append("\n\t}");

      classDef.append("\n\n\t@Override");
      classDef.append("\n\tpublic ").append(className).append(" selectByPrimaryKey(").append(keyClass).append(" ").append(keyName).append("){");
      classDef.append("\n\t\treturn ").append(StringUtil.formatCamel(model.getClassName(), false)).append("Mapper.selectByPrimaryKey(").append(keyName).append(");");
      classDef.append("\n\t}");
    }

    classDef.append("\n\n}\n");

    ClassFileInfo classFileInfo = new ClassFileInfo();
    classFileInfo.setTextInfo(classDef.toString());
    classFileInfo.setClassName(className + "ServiceImpl");

    String path = config.getTargetPackage().replace(".", File.separator) + File.separator + "service" + File.separator + "impl" + File.separator + className + "ServiceImpl.java";
    classFileInfo.setPath(path);

    return classFileInfo;
  }


  private static ClassFileInfo genDAOXmlDef(ModelMetaInfo model, GenProperties config) {
    StringBuilder classDef = new StringBuilder();
    classDef.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
    classDef.append("<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\" >\n");

    String className = genClassName(model.getClassName());
    String qualifiedModelClass = config.getTargetPackage() + ".model." + className;

    StringBuilder keyCondi = new StringBuilder();
    int idx = 0;
    for (ModelMetaAttribute attr : model.getKeys()) {
      if (idx == 0) {
        keyCondi.append("\twhere `").append(attr.getName()).append("`=#{").append(genClassAttrName(attr.getName())).append("} \n");
      } else {
        keyCondi.append("\t\tand `").append(attr.getName()).append("`=#{").append(genClassAttrName(attr.getName())).append("} \n");
      }
      idx++;
    }

    classDef.append("<mapper namespace=\"").append(qualifiedModelClass).append("Mapper\" >\n");
    classDef.append("<resultMap id=\"BaseResultMap\" type=\"").append(qualifiedModelClass).append("\" >\n");

    StringBuilder cols = new StringBuilder("\t");
    StringBuilder insertCol = new StringBuilder();
    StringBuilder updateCol = new StringBuilder();
    StringBuilder insertVal = new StringBuilder();

    for (ModelMetaAttribute attr : model.getAttributes()) {
      cols.append("`" + attr.getName()).append("`,");
      classDef.append(attr.isPk() ? "\t<id column=\"" : "\t<result column=\"").append(attr.getName()).append("\" property=\"").append(genClassAttrName(attr.getName())).append("\" jdbcType=\"").append(attr.getDataType().toUpperCase()).append("\"\\>\n");

      insertCol.append("\t\t<if test=\"").append(genClassAttrName(attr.getName())).append("\" != null\" >\n");
      insertCol.append("\t\t\t").append(attr.getName()).append(",\n");
      insertCol.append("\t\t</if>\n");

      insertVal.append("\t\t<if test=\"").append(genClassAttrName(attr.getName())).append("\" != null\" >\n");
      insertVal.append("\t\t\t#{").append(genClassAttrName(attr.getName())).append("},\n");
      insertVal.append("\t\t</if>\n");

      updateCol.append("\t\t<if test=\"").append(genClassAttrName(attr.getName())).append("\" != null\" >\n");
      updateCol.append("\t\t\t`").append(attr.getName()).append("`=#{").append(genClassAttrName(attr.getName())).append("},\n");
      updateCol.append("\t\t</if>\n");

    }

    classDef.append("</resultMap>\n");

    classDef.append("<sql id=\"Base_Column_List\" >\n");
    classDef.append(cols.toString().substring(0, cols.toString().length() - 1)).append("\n");
    classDef.append("</sql>\n");

    if (model.getKeys().size() > 0) {
      classDef.append("<select id=\"selectByPrimaryKey\" resultMap=\"BaseResultMap\" >\n");
      classDef.append("\tselect\n");
      classDef.append("\t\t<include refid=\"Base_Column_List\" />\n");
      classDef.append("\tfrom ").append(model.getClassName()).append("\n");
      classDef.append(keyCondi.toString());
      classDef.append("</select>\n");

      classDef.append("<delete id=\"deleteByPrimaryKey\" >\n");
      classDef.append("\tdelete from ").append(model.getClassName()).append("\n");
      classDef.append(keyCondi.toString());
      classDef.append("</delete>\n");
    }

    classDef.append("<insert id=\"insert\" parameterType=\"").append(qualifiedModelClass).append("\" >\n");
    classDef.append("\tinsert into ").append(model.getClassName()).append("\n");
    classDef.append("\t<trim prefix=\"(\" suffix=\")\" suffixOverrides=\",\" >\n");
    classDef.append(insertCol.toString());
    classDef.append("\t</trim>\n");
    classDef.append("\t<trim prefix=\"values (\" suffix=\")\" suffixOverrides=\",\" >\n");
    classDef.append(insertVal.toString());
    classDef.append("\t</trim>\n");
    classDef.append("</insert>\n");

    classDef.append("<update id=\"updateByPrimaryKey\" parameterType=\"").append(qualifiedModelClass).append("\" >\n");
    classDef.append("\tupdate ").append(model.getClassName()).append("\n");
    classDef.append("\t<set>\n");
    classDef.append(updateCol.toString());
    classDef.append("\t</set>\n");
    classDef.append(keyCondi);
    classDef.append("</update>\n");

    ClassFileInfo classFileInfo = new ClassFileInfo();
    classFileInfo.setTextInfo(classDef.toString());
    classFileInfo.setClassName(className + "Mapper");

    String path = "mapper" + File.separator + File.separator + className + "Mapper.xml";
    classFileInfo.setPath(path);

    return classFileInfo;
  }

  /**
   * 生成DAO层接口
   *
   * @param model
   * @param config
   * @return
   */
  private static ClassFileInfo genDAOClassDef(ModelMetaInfo model, GenProperties config) {
    StringBuilder classDef = new StringBuilder();
    classDef.append("/**\n");
    classDef.append("* Powered By [lazy-coder-generator] \n");
    classDef.append("* Web Site: https://www.coder.ink\n");
    classDef.append("*  Since 2018\n");
    classDef.append("*/\n\n");

    String className = genClassName(model.getClassName());

    if (config.getTargetPackage() != null) {
      classDef.append("package ").append(config.getTargetPackage()).append(".dao;").append("\n\n");
    }

    classDef.append("/**\n");
    classDef.append("* ").append(model.getNameCn()).append(" DAO接口（for myBatis）\n");
    classDef.append("*/");

    classDef.append("\n\nimport ").append(config.getTargetPackage()).append(".model.").append(className).append(";");

    classDef.append("\n\n@org.springframework.stereotype.Repository");
    classDef.append("\npublic interface ").append(className).append("Mapper {\n");
    classDef.append("\n\n\tpublic int insert(").append(className).append(" record);");
    classDef.append("\n\n\tpublic updateByPrimaryKey(").append(className).append(" record);");

    String keyClass = "";
    String keyName = "key";
    if (model.getKeys() != null && model.getKeys().size() > 0) {
      if (model.getKeys().size() == 1) {
        keyClass = Db2JavaTypeMapper.getMatchType(model.getKeys().get(0).getDataType());
        keyName = genClassAttrName(model.getKeys().get(0).getName());
      } else {
        keyClass = config.getTargetPackage() + ".model." + className + "Key";
      }
      classDef.append("\n\n\tpublic int deleteByPrimaryKey(").append(keyClass).append(" ").append(keyName).append(");");
      classDef.append("\n\n\tpublic ").append(className).append(" selectByPrimaryKey(").append(keyClass).append(" ").append(keyName).append(");");
    }

    classDef.append("\n\n\tpublic com.github.pagehelper.Page pageQuery").append("(Map<String, Object> param);");

    classDef.append("\n\n}\n");

    ClassFileInfo classFileInfo = new ClassFileInfo();
    classFileInfo.setTextInfo(classDef.toString());
    classFileInfo.setClassName(className + "Mapper");

    String path = config.getTargetPackage().replace(".", File.separator) + File.separator + "dao" + File.separator + className + "Mapper.java";
    classFileInfo.setPath(path);

    return classFileInfo;
  }

  private static String genClassName(String tableName) {
    return StringUtil.formatCamel(tableName, true);
  }

  private static String genClassAttrName(String colName) {
    return StringUtil.formatCamel(colName, false);
  }

  /**
   * 将表单装配为模型类
   *
   * @param sheet
   * @return
   */
  private static ModelMetaInfo pareseToModelMetaInfo(Sheet sheet) {
    int totalRowNum = sheet.getLastRowNum();

    if (totalRowNum == 0) {
      throw new RuntimeException("表格内容为空");
    }

    String nameCn = sheet.getRow(0).getCell(7).getStringCellValue();
    String name = sheet.getRow(1).getCell(7).getStringCellValue();

    if (name == null || "".equalsIgnoreCase(name.trim())) {
      throw new RuntimeException(String.format("{} 未设置实体类名称", sheet.getSheetName()));
    }

    ModelMetaInfo modelMetaInfo = new ModelMetaInfo();
    modelMetaInfo.setNameCn(nameCn);
    modelMetaInfo.setClassName(name);

    if (totalRowNum > 8) {
      int index = 8;
      modelMetaInfo.setAttributes(new ArrayList<ModelMetaAttribute>());
      modelMetaInfo.setKeys(new ArrayList<ModelMetaAttribute>());
      while (index <= totalRowNum) {
        Row row = sheet.getRow(index);
        ModelMetaAttribute attr = parseToModelAttribute(row);
        if (attr != null) {
          modelMetaInfo.getAttributes().add(attr);
          if (attr.isPk()) {
            modelMetaInfo.getKeys().add(attr);
          }
        }
        index++;
      }
    }

    return modelMetaInfo;
  }

  /**
   * 将数据行装配为模型属性
   *
   * @param row
   * @return
   */
  private static ModelMetaAttribute parseToModelAttribute(Row row) {

    ModelMetaAttribute attr = new ModelMetaAttribute();
    String nameCn = row.getCell(1).getStringCellValue();
    attr.setNameCn(nameCn);
    String name = row.getCell(6).getStringCellValue();
    attr.setName(name);
    String isPk = row.getCell(11).getStringCellValue();
    attr.setPk("Y".equalsIgnoreCase(isPk));
    String isFk = row.getCell(12).getStringCellValue();
    attr.setFk("Y".equalsIgnoreCase(isFk));
    String notNull = row.getCell(15).getStringCellValue();
    attr.setNotNull("Y".equalsIgnoreCase(notNull));
    String dataType = row.getCell(16).getStringCellValue();
    attr.setDataType(dataType);
    String length = row.getCell(20).getStringCellValue();
    if (length != null && !"".equals(length)) {
      if (length.indexOf(",") > 0) {
        attr.setLength(Integer.valueOf(length.split(",")[0]));
        attr.setPrecis(Integer.valueOf(length.split(",")[1]));
      } else {
        attr.setLength(Integer.valueOf(length));
        attr.setPrecis(0);
      }
    }
    String initValue = row.getCell(24).getStringCellValue();
    attr.setDefaultValue(initValue);

    return attr;
  }

}
