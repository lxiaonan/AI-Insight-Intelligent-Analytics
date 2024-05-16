package com.xiaonan.xnbi.controller;

import cn.hutool.core.io.FileUtil;
import com.xiaonan.xnbi.common.BaseResponse;
import com.xiaonan.xnbi.common.ErrorCode;
import com.xiaonan.xnbi.common.ResultUtils;
import com.xiaonan.xnbi.exception.BusinessException;
import com.xiaonan.xnbi.model.dto.image.UploadImageRequest;
import com.xiaonan.xnbi.model.dto.text.UploadTextRequest;
import com.xiaonan.xnbi.model.entity.Image;
import com.xiaonan.xnbi.model.entity.User;
import com.xiaonan.xnbi.model.enums.FileUploadBizEnum;
import com.xiaonan.xnbi.service.ImageService;
import com.xiaonan.xnbi.service.UserService;
import com.xiaonan.xnbi.utils.image.AiImageUtils;
import com.xiaonan.xnbi.utils.text.CharacterRecognition;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.redisson.api.RedissonClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Arrays;

/**
 * 文字识别接口
 *
 * @author <a href="https://github.com/lxiaonan">小楠</a>
 *
 */
@RestController
@RequestMapping("/text")
@Slf4j
public class TextController {

    @Resource
    private UserService userService;

    /**
     * 文件上传,并返回ai解析结果
     *
     * @param file
     * @param request
     * @return
     */
    @PostMapping("/upload")
    public BaseResponse<String> uploadTextAnalysis(@RequestPart("file") MultipartFile file,
                                                     HttpServletRequest request) {
        String biz = "text_ai";
        FileUploadBizEnum fileUploadBizEnum = FileUploadBizEnum.getEnumByValue(biz);
        if (fileUploadBizEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        validFile(file, fileUploadBizEnum);
        User loginUser = userService.getLoginUser(request);
        // 文件目录：根据业务、用户来划分
        String uuid = RandomStringUtils.randomAlphanumeric(8);
        String filename = uuid + "-" + file.getOriginalFilename();
        String filepath = String.format("/%s/%s/%s", fileUploadBizEnum.getValue(), loginUser.getId(), filename);
        File newFile = null;
        try {
            // 上传文件
            newFile = File.createTempFile(filepath, null);

            file.transferTo(newFile);

            CharacterRecognition characterRecognition = new CharacterRecognition(newFile);
            String ans = characterRecognition.getResult();

            // 返回可访问地址
            return ResultUtils.success(ans);
        } catch (Exception e) {
            log.error("file upload error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            if (file != null) {
                // 删除临时文件
                boolean delete = newFile.delete();
                if (!delete) {
                    log.error("file delete error, filepath = {}", filepath);
                }
            }
        }
    }

    /**
     * 校验文件
     *
     * @param multipartFile
     * @param fileUploadBizEnum 业务类型
     */
    private void validFile(MultipartFile multipartFile, FileUploadBizEnum fileUploadBizEnum) {
        // 文件大小
        long fileSize = multipartFile.getSize();
        // 文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        final long ONE_M = 1024 * 1024L;
        if (FileUploadBizEnum.USER_AVATAR.equals(fileUploadBizEnum)) {
            if (fileSize > 3 * ONE_M) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过 3M");
            }
            if (!Arrays.asList("jpeg", "jpg", "png", "bmp").contains(fileSuffix)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件类型错误");
            }
        }
    }
}
