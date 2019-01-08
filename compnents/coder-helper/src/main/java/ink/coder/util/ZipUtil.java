package ink.coder.util;

import java.io.*;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author wangsilu
 */
public class ZipUtil {

  public static void makeZip(String srcFilePath, String destFilePath) throws IOException {

    File src = new File(srcFilePath);
    if (!src.exists()) {
      throw new RuntimeException(srcFilePath + "不存在");
    }
    File zipFile = new File(destFilePath);
    FileOutputStream fos = new FileOutputStream(zipFile);
    CheckedOutputStream cos = new CheckedOutputStream(fos, new CRC32());
    ZipOutputStream zos = new ZipOutputStream(cos);
    String baseDir = "";
    compressbyType(src, zos, baseDir);
    zos.close();
  }


  private static void compressbyType(File src, ZipOutputStream zos, String baseDir) throws IOException {
    if (!src.exists()) {
      return;
    }
    if (src.isFile()) {
      compressFile(src, zos, baseDir);
    } else if (src.isDirectory()) {
      compressDir(src, zos, baseDir);
    }
  }


  /**
   * 压缩文件
   */
  private static void compressFile(File file, ZipOutputStream zos, String baseDir) throws IOException {

    if (!file.exists()) {
      return;
    }

    BufferedInputStream bis = new BufferedInputStream(
        new FileInputStream(file));
    ZipEntry entry = new ZipEntry(baseDir + file.getName());
    zos.putNextEntry(entry);
    int count;
    byte[] buf = new byte[4096];
    while ((count = bis.read(buf)) != -1) {
      zos.write(buf, 0, count);
    }
    bis.close();

  }


  /**
   * 压缩文件夹
   */
  private static void compressDir(File dir, ZipOutputStream zos, String baseDir) throws IOException {

    if (!dir.exists()) {
      return;
    }
    File[] files = dir.listFiles();
    if (files.length == 0) {
      zos.putNextEntry(new ZipEntry(baseDir + dir.getName() + File.separator));
    }

    for (File file : files) {
      compressbyType(file, zos, baseDir + dir.getName() + File.separator);
    }
  }


  public static void main(String[] args) {


    try {
      makeZip("C:\\Users\\wangsilu\\Desktop\\gen\\ink\\", "C:\\Users\\wangsilu\\Desktop\\gen\\version.zip");
    } catch (IOException e) {
      e.printStackTrace();
    }
    //
  }

}
