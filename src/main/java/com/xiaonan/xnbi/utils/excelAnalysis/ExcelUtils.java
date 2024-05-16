package com.xiaonan.xnbi.utils.excelAnalysis;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.xiaonan.xnbi.common.ResultUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExcelUtils {
    public static String excelToCsv(MultipartFile file) {
        try {
//            File file1 = ResourceUtils.getFile("classpath:网站数据.xlsx");
            // 读取数据
            List<Map<Integer, String>> list = EasyExcel.read(file.getInputStream())
                    .excelType(ExcelTypeEnum.XLSX)
                    .sheet()
                    .headRowNumber(0)
                    .doReadSync();
            if (list.isEmpty()) return null;
            LinkedHashMap<Integer, String> head = (LinkedHashMap<Integer, String>) list.get(0);
            List<String> collect = head.values().stream().filter(ObjectUtils::isNotEmpty).collect(Collectors.toList());
            StringBuilder res = new StringBuilder();
            res.append("{");
            res.append(StringUtils.join(collect,",")).append("\n");
            for(int i = 1;i < list.size();i ++){
                LinkedHashMap<Integer, String> hashMap = (LinkedHashMap<Integer, String>) list.get(i);
                List<String> values = hashMap.values().stream().filter(ObjectUtils::isNotEmpty).collect(Collectors.toList());
                if(i ==  list.size() - 1)
                    res.append(StringUtils.join(values,",")).append("，用,作为分隔符}").append("\n");
                else res.append(StringUtils.join(values,",")).append("\n");
            }
            return res.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
