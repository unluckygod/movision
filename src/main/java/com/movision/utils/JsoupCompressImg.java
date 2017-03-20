package com.movision.utils;

import java.util.LinkedHashMap;
import java.util.Map;

import com.movision.utils.file.FileUtil;
import com.movision.utils.oss.AliOSSClient;
import com.movision.utils.propertiesLoader.PropertiesLoader;
import com.movision.utils.ueditor.ImageUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

/**
 * @Author shuxf
 * @Date 2017/3/15 16:09
 * 通过jsoup解析帖子表中的content字段，解析html，处理帖子中的img，进行压缩处理
 */
@Service
public class JsoupCompressImg {

    @Autowired
    AliOSSClient aliossClient;

    private static final Logger log = LoggerFactory.getLogger(JsoupCompressImg.class);

    /**
     * jsoup解析html字符串，处理img
     *
     * @param content
     * @return
     */
    public Map<String, Object> compressImg(HttpServletRequest request, String content) {//content为带有img和html标签的富文本内容

        int w = 750;//图片压缩后的宽度
        int h = 425;//图片压缩后的高度

        //通过jsoup解析html
        Map<String, Object> map = new LinkedHashMap<>();
        try {
            Document doc = Jsoup.parse(content);
            Elements titleElms = doc.getElementsByTag("img");

            String compress_dir_path = PropertiesLoader.getValue("post.img.domain");//压缩图片路径url

            String compress_dir_local_path = PropertiesLoader.getValue("post.img.local.domain");//获取项目根目录/WWW/tomcat-8100/apache-tomcat-7.0.73/webapps/movision

//            String savedDir = request.getSession().getServletContext().getRealPath(compress_dir_local_path);
            String savedDir = request.getSession().getServletContext().getRealPath("");
            String tempDir = savedDir.substring(0, savedDir.lastIndexOf("/")) + compress_dir_local_path;
            System.out.println("测试获取的压缩图片服务器路径>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>" + savedDir.substring(0, savedDir.lastIndexOf("/")));

            List<String> existFileList = getExistFiles(compress_dir_path);

            for (int i = 0; i < titleElms.size(); i++) {

                boolean compressFlag = false;

                //遍历帖子中的所有图片列表

                //从img标签中获取src属性
                String imgurl = titleElms.get(i).attr("src");

                File file = new File(imgurl);//首先下载该文件到服务器路径下

                String filename = FileUtil.getPicName(imgurl);
                log.info("filename=" + filename);

                //原图的绝对路径
                String proto_img_dir = savedDir.substring(0, savedDir.lastIndexOf("/")) + PropertiesLoader.getValue("post.proto.img.domain") + filename;

                //根据图片url下载图片存在服务器/WWW/tomcat-8200/apache-tomcat-7.0.73/webapps/images/post/compressimg/目录下
//                FileUtil.downloadObject(imgurl, tempDir, filename, "img");

                if (StringUtils.isNotEmpty(imgurl)) {

                    // 1 生成压缩后的图片的url
                    String compress_file_path = compress_dir_path + filename;
                    log.info("压缩后的图片路径，compress_file_path=" + compress_file_path);

                    // 2 判断该文件夹下是否有同名的图片，若有则不处理，若没有则进行处理
                    if (CollectionUtils.isEmpty(existFileList) || !existFileList.contains(filename)) {
                        // 压缩核心算法
                        compressFlag = compressJpgOrPng(w, h, compressFlag, filename, proto_img_dir, compress_file_path);
                        // 处理过的图片加入到已处理集合，防止重复压缩图片
                        existFileList.add(filename);
                    } else {
                        compressFlag = true;
                        log.info("该图片已存在，不需要压缩，filename=" + filename);
                    }
                    if (compressFlag) {
                        // 上传
                        Map<String, String> resultmap = aliossClient.uploadLocalFile(file, "img", "compress");
                        log.info("upload>>>>>>>>>status:" + resultmap.get("status"));
                        log.info("upload>>>>>>>>>data:" + resultmap.get("data"));
                        if (resultmap.get("status").equals("success")) {
                            //如果上传成功，这里替换文章中的第i个img标签中的src属性

                        }
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    public boolean compressJpgOrPng(int w, int h, boolean compressFlag, String filename, String filePath, String compress_file_path) {
        if (filename.endsWith(".jpg")) {

            log.info("压缩jpg图片，filepath=" + filePath);
            compressFlag = ImgCompressUtil.ImgCompress(filePath, compress_file_path, w, h);
        }
        if (filename.endsWith(".png")) {
            File srcFile = new File(filePath);
            log.info("压缩png图片,filepath=" + filePath);
            compressFlag = ImageUtil.fromFile(srcFile).size(w, h).quality(0.7f).fixedGivenSize(false).keepRatio(true) // 图片宽高比例
                    .bgcolor(null) // 透明背景
                    .toFile(new File(compress_file_path));
        }
        return compressFlag;
    }

    /**
     * 获取指定文件夹下的文件 不分类型，即png,jpg都有
     *
     * @param compress_dir_path
     * @return
     */
    public List<String> getExistFiles(String compress_dir_path) {
        List<String> existFileList = new ArrayList<>();
        try {
            existFileList = FileUtil.readfileName(compress_dir_path, null);
            log.info("指定路径下的图片有：" + existFileList.toArray().toString());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return existFileList;
    }
}
