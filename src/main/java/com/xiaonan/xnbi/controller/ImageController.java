package com.xiaonan.xnbi.controller;

import cn.hutool.core.io.FileUtil;
import com.xiaonan.xnbi.common.BaseResponse;
import com.xiaonan.xnbi.common.ErrorCode;
import com.xiaonan.xnbi.common.ResultUtils;
import com.xiaonan.xnbi.exception.BusinessException;
import com.xiaonan.xnbi.model.dto.image.UploadImageRequest;
import com.xiaonan.xnbi.model.entity.Image;
import com.xiaonan.xnbi.model.entity.User;
import com.xiaonan.xnbi.model.enums.FileUploadBizEnum;
import com.xiaonan.xnbi.service.ImageService;
import com.xiaonan.xnbi.service.UserService;
import com.xiaonan.xnbi.utils.image.AiImageUtils;
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
 * 图片分析接口
 *
 * @author <a href="https://github.com/lxiaonan">小楠</a>
 *
 */
@RestController
@RequestMapping("/image")
@Slf4j
public class ImageController {

    @Resource
    private UserService userService;

    @Resource
    private RedissonClient redissonClient;
    @Resource
    private ImageService imageService;
    /**
     * 文件上传,并返回ai解析结果
     *
     * @param file
     * @param uploadImageRequest
     * @param request
     * @return
     */
    @PostMapping("/upload")
    public BaseResponse<String> uploadImageAnalysis(@RequestPart("file") MultipartFile file,
                                           UploadImageRequest uploadImageRequest, HttpServletRequest request) {
        String biz = "user_avatar";
        String goal = uploadImageRequest.getGoal();
        FileUploadBizEnum fileUploadBizEnum = FileUploadBizEnum.getEnumByValue(biz);
        if (fileUploadBizEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String fileSuffix = validFile(file, fileUploadBizEnum);
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
            Image image = new Image();
            image.setGoal(goal);
            image.setImageType(fileSuffix);
            image.setBaseString("");
            boolean save = imageService.save(image);
            if (!save){
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"新增图片信息失败");
            }
            AiImageUtils aiImageUtils = new AiImageUtils(redissonClient);
            String ans = aiImageUtils.getAns(newFile, goal,image.getId());
            image.setGenResult(ans);
            image.setState("succeed");
            boolean update = imageService.updateById(image);
            if (!update){
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"更新图片信息失败");
            }
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
    private String validFile(MultipartFile multipartFile, FileUploadBizEnum fileUploadBizEnum) {
        // 文件大小
        long fileSize = multipartFile.getSize();
        // 文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        final long ONE_M = 1024 * 1024L;
        if (FileUploadBizEnum.USER_AVATAR.equals(fileUploadBizEnum)) {
            if (fileSize > ONE_M) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过 1M");
            }
            if (!Arrays.asList("jpeg", "jpg", "svg", "png", "webp").contains(fileSuffix)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件类型错误");
            }
        }
        return fileSuffix;
    }
}
