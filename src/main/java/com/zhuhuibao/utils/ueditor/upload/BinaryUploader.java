package com.zhuhuibao.utils.ueditor.upload;


import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.zhuhuibao.utils.PropertiesUtils;
import com.zhuhuibao.utils.oss.AliOSSClient;
import com.zhuhuibao.utils.ueditor.PathFormat;
import com.zhuhuibao.utils.ueditor.define.AppInfo;
import com.zhuhuibao.utils.ueditor.define.BaseState;
import com.zhuhuibao.utils.ueditor.define.FileType;
import com.zhuhuibao.utils.ueditor.define.State;
import org.springframework.web.multipart.MultipartFile;

public class BinaryUploader {


    public static final State saveToObject(Map<String, Object> conf, MultipartFile upfile) {
        String uploadMode = PropertiesUtils.getValue("upload.mode");
        if ("zhb".equals(uploadMode)) {
            return save(conf, upfile);
        } else if ("alioss".equals(uploadMode)) {
            return saveToAliOSS(conf, upfile);
        } else {
            return new BaseState(false);
        }
    }

    public static final State saveToAliOSS(Map<String, Object> conf, MultipartFile upfile) {
//        FileItemStream fileStream = null;
//        boolean isAjaxUpload = request.getHeader("X_Requested_With") != null;
//
//        if (!ServletFileUpload.isMultipartContent(request)) {
//            return new BaseState(false, AppInfo.NOT_MULTIPART_CONTENT);
//        }
//
//        ServletFileUpload upload = new ServletFileUpload(
//                new DiskFileItemFactory());
//
//        if (isAjaxUpload) {
//            upload.setHeaderEncoding("UTF-8");
//        }

        //            FileItemIterator iterator = upload.getItemIterator(request);
//
//            while (iterator.hasNext()) {
//                fileStream = iterator.next();
//
//                if (!fileStream.isFormField())
//                    break;
//                fileStream = null;
//            }
//
//            if (fileStream == null) {
//                return new BaseState(false, AppInfo.NOTFOUND_UPLOAD_DATA);
//            }

//            String savePath = (String) conf.get("savePath");
        String originFileName = upfile.getOriginalFilename();//fileStream.getName();
        String suffix = FileType.getSuffixByFilename(originFileName);

        originFileName = originFileName.substring(0,
                originFileName.length() - suffix.length());
//            savePath = savePath + suffix;

        long maxSize = (Long) conf.get("maxSize");

        if (!validType(suffix, (String[]) conf.get("allowFiles"))) {
            return new BaseState(false, AppInfo.NOT_ALLOW_FILE_TYPE);
        }

        AliOSSClient aliOSSClient = new AliOSSClient();
        String type = (String) conf.get("actionType");

        Map<String, String> result = aliOSSClient.uploadStream(upfile, maxSize,
                type.equals("img") ? "img" : "doc", "ueditor");

        State storageState;
        if (result.get("status").equals("success")) {
            storageState = new BaseState(true);
        } else {
            storageState = new BaseState(false);
        }

        if (storageState.isSuccess()) {
            storageState.putInfo("url", result.get("data"));
            storageState.putInfo("type", suffix);
            storageState.putInfo("original", originFileName + suffix);
        }

        return storageState;
    }


    public static final State save(Map<String, Object> conf, MultipartFile upfile) {

        try {
            InputStream is = upfile.getInputStream();

            String savePath = (String) conf.get("savePath");
            String originFileName = upfile.getOriginalFilename();//fileStream.getName();
            String suffix = FileType.getSuffixByFilename(originFileName);

            originFileName = originFileName.substring(0,
                    originFileName.length() - suffix.length());
            savePath = savePath + suffix;

            long maxSize = ((Long) conf.get("maxSize")).longValue();

            if (!validType(suffix, (String[]) conf.get("allowFiles"))) {
                return new BaseState(false, AppInfo.NOT_ALLOW_FILE_TYPE);
            }

            savePath = PathFormat.parse(savePath, originFileName);

            //String physicalPath = conf.get("rootPath") + savePath;
            String physicalPath = conf.get("uploadRootPath") + savePath;

            State storageState = StorageManager.saveFileByInputStream(is,
                    physicalPath, maxSize);
            is.close();

            if (storageState.isSuccess()) {
                storageState.putInfo("url",PathFormat.format(savePath));
                storageState.putInfo("type", suffix);
                storageState.putInfo("original", originFileName + suffix);
            }

            return storageState;
        }
        catch (IOException e) {
            return new BaseState(false, AppInfo.PARSE_REQUEST_ERROR);
        }
    }


    private static boolean validType(String type, String[] allowTypes) {
        List<String> list = Arrays.asList(allowTypes);

        return list.contains(type);
    }
}
