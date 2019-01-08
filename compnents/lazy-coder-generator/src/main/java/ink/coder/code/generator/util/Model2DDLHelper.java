package ink.coder.code.generator.util;

import ink.coder.code.generator.config.DialectType;
import ink.coder.code.generator.model.ModelMetaAttribute;
import ink.coder.code.generator.model.ModelMetaInfo;

public class Model2DDLHelper {


  public static String model2DDL(ModelMetaInfo model, DialectType type) {
    switch (type){
      case MySQL:
        return genMysqlDDL(model);
      default:
        throw new RuntimeException("暂不支持的数据库类型");
    }
  }

  private static String genMysqlDDL(ModelMetaInfo model) {
    StringBuilder ddl = new StringBuilder();
    ddl.append("CREATE TABLE `").append(model.getClassName()).append("` (\n");

    int idx = 0;

    String keys = "";

    for (ModelMetaAttribute attr : model.getAttributes()) {
      ddl.append("\t`").append(attr.getName()).append("` ").append(attr.getDataType()).append(attr.getLength() > 0 ? ("(" + attr.getLength() + (attr.getPrecis()>0?","+attr.getPrecis():"")+")") : "");
      if (attr.isPk() || attr.isNotNull()) {
        ddl.append(" NOT NULL");
      }
      if (attr.getDefaultValue() != null && !"".equals(attr.getDefaultValue())) {
        ddl.append(" DEFAULT '").append(attr.getDefaultValue()).append("'");
      }
      if (attr.getNameCn() != null && !"".equals(attr.getNameCn())) {
        ddl.append(" COMMENT '").append(attr.getNameCn()).append("'");
      }
      if (idx != model.getAttributes().size() - 1) {
        ddl.append(",\n");
      }
      if (attr.isPk()) {
        keys += "`" + attr.getName() + "`,";
      }
      idx++;
    }

    ddl.append("\n) ENGINE=InnoDB DEFAULT CHARSET=utf8 ").append(model.getNameCn() != null ? ("COMMENT='" + model.getNameCn() + "'") : "").append(";\n\n");

    if (keys.length() > 0) {
      ddl.append("ALTER TABLE `").append(model.getClassName()).append("` ADD PRIMARY KEY(");
      ddl.append(keys.substring(0, keys.length() - 1));
      ddl.append(");\n\n");
    }
    return ddl.toString();
  }

}
