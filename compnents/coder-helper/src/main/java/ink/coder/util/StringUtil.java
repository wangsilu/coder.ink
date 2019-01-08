package ink.coder.util;

import java.util.List;
import java.util.Map;

/**
 * 文本类工具
 *
 * @author
 * @since
 */
public class StringUtil {

  /**
   * 将文本转换为驼峰命名规则
   * @param srcStr 源文本
   * @param firstUppercase 首字母是否大写
   * @return
   */
  public static String formatCamel(String srcStr, boolean firstUppercase) {
    if (srcStr == null || "".equalsIgnoreCase(srcStr)) {
      return null;
    }
    String[] items = srcStr.split("_");
    StringBuilder res = new StringBuilder(srcStr.length());
    int idx = 0;
    for (String item : items) {
      if (idx == 0) {
        if(firstUppercase) {
          res.append(item.substring(0, 1).toUpperCase());
        }else{
          res.append(item.substring(0, 1).toLowerCase());
        }
      } else {
        res.append(item.substring(0, 1).toUpperCase());
      }
      res.append(item.substring(1).toLowerCase());
      idx++;
    }
    return res.toString();
  }

  /**
   * 拼接字符串
   * @param strs
   * @return
   */
  public static String concatString(List<String> strs){
    StringBuilder sb = new StringBuilder();
    for (String s:strs) {
      sb.append(s);
    }
    return sb.toString();
  }

  /**
   * 拼接字符串
   * @param strs
   * @return
   */
  public static String concatString(Map<String,String> strs){
    StringBuilder sb = new StringBuilder();
    for (String s:strs.values()) {
      sb.append(s);
    }
    return sb.toString();
  }


  public static void main(String[] args) {
//    System.out.println(formatCamel("fasfa_dfsf_sfs",false));
//    System.out.println(formatCamel("fasfa_dfsf_sfs",true));
  }

}
